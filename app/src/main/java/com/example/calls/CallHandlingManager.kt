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
        val matchingRule = rules.find { number.contains(it.phoneNumber.replace(Regex("[^0-9+]"), "")) || it.phoneNumber.replace(Regex("[^0-9+]"), "").contains(number) }

        val tier = getRelationshipTier(context, number)
        val isTemporarilyVipResult = isTemporarilyVip(context, number)
        val isVip = (tier == "Inner Circle") || isTemporarilyVipResult
        val isContact = (tier == "Standard" || tier == "Inner Circle")

        val blockSpam = runBlocking { settingsRepo.getBooleanSync(SettingsRepository.BLOCK_SPAM_CALLS, false) }
        val ghostModeBase = runBlocking { settingsRepo.getBooleanSync(SettingsRepository.GHOST_MODE, false) }
        val ghostModePauseEndTime = runBlocking { settingsRepo.getLongSync(SettingsRepository.GHOST_MODE_PAUSE_END_TIME, 0L) }
        val ghostMode = ghostModeBase && System.currentTimeMillis() > ghostModePauseEndTime
        
        val autoForward = runBlocking { settingsRepo.getBooleanSync(SettingsRepository.AUTO_FORWARD_CALLS, false) }

        if (tier == "Blocked") {
            Log.d("CallHandlingManager", "Call from Blocked tier: $number. Rejecting.")
            rejectCall(context)
            runBlocking {
                try {
                    val appDb = (context.applicationContext as com.example.ShieldApplication).container.database
                    appDb.smsLogDao().insert(com.example.data.SmsLogEntity(
                        timestamp = System.currentTimeMillis(),
                        sender = number,
                        message = "Blocked Caller",
                        targetNumber = "",
                        status = "BLOCKED_CALL"
                    ))
                } catch (e: Exception) {
                    Log.e("CallHandlingManager", "Failed to log Blocked call", e)
                }
            }
            return
        }
        
        if (tier == "Muted") {
            Log.d("CallHandlingManager", "Call from Muted tier: $number. Silencing ringer.")
            silenceRinger(context)
            runBlocking {
                try {
                    val appDb = (context.applicationContext as com.example.ShieldApplication).container.database
                    appDb.smsLogDao().insert(com.example.data.SmsLogEntity(
                        timestamp = System.currentTimeMillis(),
                        sender = number,
                        message = "Muted Caller",
                        targetNumber = "",
                        status = "MUTED_CALL"
                    ))
                } catch (e: Exception) {
                    Log.e("CallHandlingManager", "Failed to log Muted call", e)
                }
            }
            // Allow it to ring silently
        }

        if (ghostMode && !isVip) {
            Log.d("CallHandlingManager", "Ghost Mode active. Rejecting call from $number.")
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
            
            // Auto reply for standards during Ghost Mode
            if (tier == "Standard") {
                com.example.shield.AutoResponder.handleMissedCall(context, number, "Standard Contact")
            }
            return
        }


    
        if (isVip) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val currentMode = audioManager.ringerMode
            if (currentMode != AudioManager.RINGER_MODE_NORMAL) {
                try {
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    val canBypass = android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M || notificationManager.isNotificationPolicyAccessGranted
                    
                    if (canBypass) {
                        originalRingerMode = currentMode
                        originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING)
                        
                        audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
                        audioManager.setStreamVolume(AudioManager.STREAM_RING, maxVol, 0)
                        
                        currentlyBypassedNumber = number
                        Log.d("CallHandlingManager", "VIP Volume Hijack activated for: $number")
                    } else {
                        Log.d("CallHandlingManager", "Missing Notification Policy Access to bypass Silent Mode")
                    }
                } catch (e: Exception) {
                    Log.e("CallHandlingManager", "Failed to bypass Silent Mode for VIP", e)
                }
            } else {
                Log.d("CallHandlingManager", "VIP call ringing normally: $number")
            }
        }
    }

    fun restoreAudioState(context: Context) {
        if (currentlyBypassedNumber != null) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            
            try {
                if (originalRingerMode != null && originalRingerMode != AudioManager.RINGER_MODE_NORMAL) {
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    val canRestore = android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M || notificationManager.isNotificationPolicyAccessGranted
                    
                    if (canRestore) {
                        audioManager.ringerMode = originalRingerMode!!
                        originalVolume?.let { audioManager.setStreamVolume(AudioManager.STREAM_RING, it, 0) }
                    }
                } else {
                    audioManager.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_UNMUTE, 0)
                    originalVolume?.let { audioManager.setStreamVolume(AudioManager.STREAM_RING, it, 0) }
                }
            } catch (e: Exception) {
                Log.e("CallHandlingManager", "Failed to restore audio state", e)
            }
            
            originalVolume = null
            originalRingerMode = null
            currentlyBypassedNumber = null
            Log.d("CallHandlingManager", "Restored audio state after call.")
        }
    }

    fun handleMissedCall(context: Context, number: String) {
        restoreAudioState(context)
        com.example.shield.AutoResponder.handleMissedCall(context, number, "Missed Caller")
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

    private fun isStarredContact(context: Context, number: String): Boolean {
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return false
        }
        return try {
            val uri = android.net.Uri.withAppendedPath(android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI, android.net.Uri.encode(number))
            val projection = arrayOf(android.provider.ContactsContract.PhoneLookup.STARRED)
            var isStarred = false
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val starredIndex = cursor.getColumnIndex(android.provider.ContactsContract.PhoneLookup.STARRED)
                    if (starredIndex >= 0) {
                        isStarred = cursor.getInt(starredIndex) == 1
                    }
                }
            }
            isStarred
        } catch (e: Exception) {
            Log.e("CallHandlingManager", "Failed to check starred status", e)
            false
        }
    }

    
    fun getRelationshipTier(context: Context, number: String): String {
        val phoneRuleRepo = (context.applicationContext as com.example.ShieldApplication).container.phoneRuleRepository
        val rules = kotlinx.coroutines.runBlocking { phoneRuleRepo.getAllRulesSync() }
        
        val cleanIncoming = number.replace(Regex("[^0-9+]"), "")
        val rule = rules.find { 
            val cleanRule = it.phoneNumber.replace(Regex("[^0-9+]"), "")
            cleanRule.isNotEmpty() && (cleanIncoming.contains(cleanRule) || cleanRule.contains(cleanIncoming))
        }
        
        if (rule != null) {
            if (rule.relationshipTier == "Blocked") return "Blocked"
            if (rule.relationshipTier == "Muted") return "Muted"
            if (rule.relationshipTier == "Inner Circle" || rule.isVip) return "Inner Circle"
            if (rule.relationshipTier == "Standard") return "Standard"
        }
        
        if (isStarredContact(context, number)) {
            return "Inner Circle"
        }
        
        if (isNumberInContacts(context, number)) {
            return "Standard"
        }
        
        return "Unknown"
    }

    fun isNumberVip(context: Context, number: String): Boolean {
        return getRelationshipTier(context, number) == "Inner Circle"
    }
    
    fun isBlocked(context: Context, number: String): Boolean {
        return getRelationshipTier(context, number) == "Blocked"
    }

    fun isNuisance(context: Context, number: String): Boolean {
        return getRelationshipTier(context, number) == "Nuisance"
    }
fun isKnownSpam(number: String): Boolean {
        return number.startsWith("+1800") || number.contains("spam", ignoreCase = true)
    }

    
    fun silenceRinger(context: Context) {
        try {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            if (androidx.core.app.ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ANSWER_PHONE_CALLS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                @Suppress("DEPRECATION") telecomManager.silenceRinger()
            }
        } catch (e: Exception) {
            Log.e("CallHandlingManager", "Failed to silence ringer", e)
        }
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
