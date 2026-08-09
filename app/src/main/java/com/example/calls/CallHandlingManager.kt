package com.example.calls

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.telecom.TelecomManager
import android.util.Log
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.runBlocking

object CallHandlingManager {

    private var originalRingerMode: Int? = null
    private var originalVolume: Int? = null
    private var originalAlarmVolume: Int? = null
    private var originalInterruptionFilter: Int? = null
    private var currentlyBypassedNumber: String? = null
    private var vipRingtone: android.media.Ringtone? = null

    private fun isTemporarilyVip(context: Context, number: String): Boolean {
        val settingsRepo = (context.applicationContext as com.example.ShieldApplication).container.settingsRepository
        val thresholdCalls = runBlocking { settingsRepo.getIntSync(SettingsRepository.DND_THRESHOLD_CALLS, 2) }
        val timeframeMinutes = runBlocking { settingsRepo.getIntSync(SettingsRepository.DND_TIMEFRAME_MINUTES, 5) }
        
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALL_LOG) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return false
        }
        
        try {
            val projection = arrayOf(android.provider.CallLog.Calls.DATE, android.provider.CallLog.Calls.NUMBER)
            val cursor = context.contentResolver.query(
                android.provider.CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                android.provider.CallLog.Calls.DATE + " DESC LIMIT 20"
            )
            
            var recentCount = 1 // Count the current ringing call
            val now = System.currentTimeMillis()
            val timeLimit = now - (timeframeMinutes * 60 * 1000L)
            
            cursor?.use { c ->
                val dateIdx = c.getColumnIndex(android.provider.CallLog.Calls.DATE)
                val numberIdx = c.getColumnIndex(android.provider.CallLog.Calls.NUMBER)
                
                while (c.moveToNext()) {
                    val callDate = c.getLong(dateIdx)
                    if (callDate < timeLimit) break
                    
                    val callNumber = c.getString(numberIdx) ?: ""
                    if (android.telephony.PhoneNumberUtils.compare(callNumber, number) || callNumber.contains(number) || number.contains(callNumber)) {
                        recentCount++
                    }
                }
            }
            return recentCount >= thresholdCalls
        } catch (e: Exception) {
            Log.e("CallHandlingManager", "Failed to check call log for VIP", e)
            return false
        }
    }

    fun handleIncomingCall(context: Context, number: String) {
        val settingsRepo = (context.applicationContext as com.example.ShieldApplication).container.settingsRepository
        
        val isKillSwitchOn = runBlocking { settingsRepo.getBooleanSync(androidx.datastore.preferences.core.booleanPreferencesKey("master_kill_switch"), false) }
        if (isKillSwitchOn) {
            Log.d("CallHandlingManager", "App is in Dumb State (Master Kill Switch ON). Ignoring call.")
            return
        }

        val phoneRuleRepo = (context.applicationContext as com.example.ShieldApplication).container.phoneRuleRepository
        
        val rules = runBlocking { phoneRuleRepo.getAllRulesSync() }
        val matchingRule = rules.find { number.contains(it.phoneNumber) || it.phoneNumber.contains(number) }
        
        val isPersistentVip = isTemporarilyVip(context, number)
        val isVip = matchingRule?.isVip == true || isPersistentVip || isNumberVip(context, number)
        val isContact = isNumberInContacts(context, number)
        
        val blockSpam = runBlocking { settingsRepo.getBooleanSync(SettingsRepository.BLOCK_SPAM_CALLS, false) }
        val ghostModeBase = runBlocking { settingsRepo.getBooleanSync(SettingsRepository.GHOST_MODE, false) }
        val ghostModePauseEndTime = runBlocking { settingsRepo.getLongSync(SettingsRepository.GHOST_MODE_PAUSE_END_TIME, 0L) }
        val ghostMode = ghostModeBase && System.currentTimeMillis() > ghostModePauseEndTime
        
        val autoForward = runBlocking { settingsRepo.getBooleanSync(SettingsRepository.AUTO_FORWARD_CALLS, false) }
        
        if (ghostMode && !isContact && !isVip) {
            rejectCall(context)
            runBlocking { 
                settingsRepo.incrementSpamBlockedCount()
                try {
                    val appDb = (context.applicationContext as com.example.ShieldApplication).container.database
                    appDb.smsLogDao().insert(com.example.data.SmsLogEntity(
                        timestamp = System.currentTimeMillis(),
                        sender = number,
                        message = "Blocked by Ghost Mode",
                        targetNumber = "",
                        status = "SPAM_BLOCKED"
                    ))
                } catch (e: Exception) {
                    Log.e("CallHandlingManager", "Failed to log ghost mode block", e)
                }
            }
            Log.d("CallHandlingManager", "Ghost Mode rejected unknown caller: $number")
            return
        }

        if (autoForward && !isVip) {
            rejectCall(context)
            Log.d("CallHandlingManager", "Auto Forward rejected unknown caller: $number")
            
            val forwardTarget = runBlocking { settingsRepo.getStringSync(SettingsRepository.FORWARD_PHONE, "") }
            
            runBlocking {
                try {
                    val appDb = (context.applicationContext as com.example.ShieldApplication).container.database
                    appDb.smsLogDao().insert(com.example.data.SmsLogEntity(
                        timestamp = System.currentTimeMillis(),
                        sender = number,
                        message = "Call Forwarded to $forwardTarget",
                        targetNumber = forwardTarget,
                        status = "CALL_FORWARDED"
                    ))
                } catch (e: Exception) {
                    Log.e("CallHandlingManager", "Failed to log call forwarding", e)
                }
            }
            if (forwardTarget.isNotBlank()) {
                CallForwardingHelper.activateForwarding(context, forwardTarget)
                
                val request = androidx.work.OneTimeWorkRequestBuilder<com.example.shield.DeactivateForwardingWorker>()
                    .setInitialDelay(5, java.util.concurrent.TimeUnit.MINUTES)
                    .build()
                androidx.work.WorkManager.getInstance(context).enqueue(request)
            }

            // Send Auto-Reply
            try {
                val data = androidx.work.Data.Builder()
                    .putString("targetNumber", number)
                    .putString("message", "I'm currently busy, please text or hold on, forwarding your call...")
                    .build()
                val smsRequest = androidx.work.OneTimeWorkRequestBuilder<com.example.shield.SmsWorker>()
                    .setInputData(data)
                    .build()
                androidx.work.WorkManager.getInstance(context).enqueue(smsRequest)
            } catch (e: Exception) {
                Log.e("CallHandlingManager", "Failed to send forwarding auto-reply", e)
            }
            return
        }
        
        if (blockSpam && isKnownSpam(number)) {
            rejectCall(context)
            runBlocking { 
                settingsRepo.incrementSpamBlockedCount()
                try {
                    val appDb = (context.applicationContext as com.example.ShieldApplication).container.database
                    appDb.smsLogDao().insert(com.example.data.SmsLogEntity(
                        timestamp = System.currentTimeMillis(),
                        sender = number,
                        message = "Spam Call Blocked",
                        targetNumber = "",
                        status = "SPAM_BLOCKED"
                    ))
                } catch (e: Exception) {
                    Log.e("CallHandlingManager", "Failed to log spam block", e)
                }
            }
            Log.d("CallHandlingManager", "Rejected spam call from $number")
            return
        }
        
        if (isVip) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            originalRingerMode = audioManager.ringerMode
            originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING)
            originalAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
            
            if (notificationManager.isNotificationPolicyAccessGranted) {
                originalInterruptionFilter = notificationManager.currentInterruptionFilter
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            }
            
            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
            
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
            audioManager.setStreamVolume(AudioManager.STREAM_RING, maxVol, 0)
            
            val maxAlarmVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxAlarmVol, 0)
            
            try {
                val ringtoneUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM) ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE)
                vipRingtone = android.media.RingtoneManager.getRingtone(context, ringtoneUri)
                val audioAttributes = android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                vipRingtone?.audioAttributes = audioAttributes
                vipRingtone?.play()
            } catch (e: Exception) {
                Log.e("CallHandlingManager", "Failed to play VIP ringtone", e)
            }
            
            currentlyBypassedNumber = number
            Log.d("CallHandlingManager", "VIP caller bypassed DND: $number")
        }
    }

    fun restoreAudioState(context: Context) {
        if (currentlyBypassedNumber != null) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            originalVolume?.let { audioManager.setStreamVolume(AudioManager.STREAM_RING, it, 0) }
            originalAlarmVolume?.let { audioManager.setStreamVolume(AudioManager.STREAM_ALARM, it, 0) }
            originalRingerMode?.let { audioManager.ringerMode = it }
            
            if (notificationManager.isNotificationPolicyAccessGranted) {
                originalInterruptionFilter?.let { notificationManager.setInterruptionFilter(it) }
            }
            
            vipRingtone?.stop()
            vipRingtone = null
            
            originalVolume = null
            originalAlarmVolume = null
            originalRingerMode = null
            originalInterruptionFilter = null
            currentlyBypassedNumber = null
            Log.d("CallHandlingManager", "Restored audio state after VIP call.")
        }
    }

    fun handleMissedCall(context: Context, number: String) {
        restoreAudioState(context)
        
        val settingsRepo = (context.applicationContext as com.example.ShieldApplication).container.settingsRepository
        val autoReplyEnabled = runBlocking { settingsRepo.getBooleanSync(SettingsRepository.AUTO_RESPOND_MISSED_CALL, false) }
        
        if (autoReplyEnabled) {
            val restrictedListStr = runBlocking { settingsRepo.getStringSync(SettingsRepository.AUTO_REPLY_RESTRICTED_NUMBERS, "") }
            val restricted = restrictedListStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            
            val shouldReply = if (restricted.isEmpty()) {
                isNumberInContacts(context, number)
            } else {
                isNumberInContacts(context, number)
            }
            
            if (shouldReply) {
                val autoReplyMessage = runBlocking { settingsRepo.getStringSync(SettingsRepository.BUSY_REPLY_MSG, "I am currently in another call. I will call you back later.") }
                try {
                    val data = androidx.work.Data.Builder()
                        .putString("targetNumber", number)
                        .putString("message", autoReplyMessage)
                        .build()
                    val smsRequest = androidx.work.OneTimeWorkRequestBuilder<com.example.shield.SmsWorker>()
                        .setInputData(data)
                        .build()
                    androidx.work.WorkManager.getInstance(context).enqueue(smsRequest)
                    Log.d("CallHandlingManager", "Sent auto-reply to missed call: $number")
                } catch (e: Exception) {
                    Log.e("CallHandlingManager", "Failed to send auto-reply", e)
                }
            }
        }
    }

    fun handleCallAnswered(context: Context, number: String) {
        restoreAudioState(context)
    }

    fun isNumberInContacts(context: Context, number: String): Boolean {
        if (androidx.core.app.ActivityCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return false
        }
        try {
            val uri = Uri.withAppendedPath(android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
            val cursor = context.contentResolver.query(uri, arrayOf(android.provider.ContactsContract.PhoneLookup._ID), null, null, null)
            val exists = cursor?.moveToFirst() == true
            cursor?.close()
            return exists
        } catch (e: Exception) {
            Log.e("CallHandlingManager", "Failed to check contacts", e)
            return false
        }
    }

    fun isNumberVip(context: Context, number: String): Boolean {
        val settingsRepo = (context.applicationContext as com.example.ShieldApplication).container.settingsRepository
        val dndOverrideEnabled = runBlocking { settingsRepo.getBooleanSync(SettingsRepository.OVERRIDE_DND, false) }
        if (!dndOverrideEnabled) return false
        
        val vipListStr = runBlocking { settingsRepo.getStringSync(SettingsRepository.VIP_CALLERS, "") }
        val vips = vipListStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        
        return vips.any { number.contains(it) || it.contains(number) }
    }
    
    fun isKnownSpam(number: String): Boolean {
        return number.startsWith("+1800") || number.contains("spam", ignoreCase = true)
    }

    fun rejectCall(context: Context) {
        try {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            if (androidx.core.app.ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ANSWER_PHONE_CALLS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                @Suppress("DEPRECATION") telecomManager.endCall()
            }
        } catch (e: Exception) {
            Log.e("CallHandlingManager", "Failed to reject call", e)
        }
    }
}
