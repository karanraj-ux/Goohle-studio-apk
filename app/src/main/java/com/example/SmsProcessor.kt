package com.example

import android.content.Context
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.SmsLogEntity
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

data class ProcessingResult(val status: String, val durationMs: Long)

object SmsProcessor {
    suspend fun processReceivedMessage(context: Context, sender: String, body: String, timestamp: Long = System.currentTimeMillis(), isSimulation: Boolean = false): ProcessingResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val settingsRepo = (context.applicationContext as com.example.ShieldApplication).container.settingsRepository
        
        val isEnabled = settingsRepo.getBooleanSync(SettingsRepository.SMS_FORWARDING_ENABLED, false)
        val isKillSwitchOn = settingsRepo.getBooleanSync(androidx.datastore.preferences.core.booleanPreferencesKey("master_kill_switch"), false)
        
        if (isKillSwitchOn) {
            return@withContext ProcessingResult("KILLED_BY_MASTER_SWITCH", System.currentTimeMillis() - startTime)
        }

        var targetNumbersStr = settingsRepo.getStringSync(SettingsRepository.TARGET_NUMBERS)
        if (targetNumbersStr.isBlank()) {
            val legacy = settingsRepo.getStringSync(androidx.datastore.preferences.core.stringPreferencesKey("target_number"))
            if (legacy.isNotBlank()) targetNumbersStr = legacy
        }

        var sendersStr = settingsRepo.getStringSync(SettingsRepository.SENDERS)
        if (sendersStr.isBlank()) {
            val legacy = settingsRepo.getStringSync(androidx.datastore.preferences.core.stringPreferencesKey("sender_filter"))
            if (legacy.isNotBlank()) sendersStr = legacy
        }

        val keywordFilter = settingsRepo.getStringSync(SettingsRepository.KEYWORD_FILTER)
        val webhookUrl = settingsRepo.getStringSync(SettingsRepository.WEBHOOK_URL)

        val targetNumbers = targetNumbersStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val senders = sendersStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        val repo = (context.applicationContext as com.example.ShieldApplication).container.smsRepository

        if (!isEnabled && !isSimulation) {
            return@withContext ProcessingResult("IGNORED", 0L)
        }

        val senderMatches = senders.isEmpty() || senders.any { sender.contains(it, ignoreCase = true) }
        val keywordMatches = keywordFilter.isBlank() || body.contains(keywordFilter, ignoreCase = true)

        var finalStatus = "IGNORED"

        // Phase 5: On-Device ML for Offline Scam Detection
        val offlineClassifier = com.example.ml.OfflineScamClassifier
        offlineClassifier.init(context)
        if (offlineClassifier.isScam(body) || com.example.shield.ScamDictionary.isScam(context, body)) {
            Log.d("SmsProcessor", "Scam detected locally! Blocking.")
            com.example.widget.WidgetUpdater.updateWidgetState(context, "SCAM", "Blocked scam from $sender")
            if (!isSimulation) {
                repo.insertLog(SmsLogEntity(timestamp = timestamp, sender = sender, message = body, targetNumber = "BLOCKED", status = "SCAM_BLOCKED"))
            }
            return@withContext ProcessingResult("SCAM_BLOCKED", System.currentTimeMillis() - startTime)
        }

        if (senderMatches && keywordMatches) {
            finalStatus = "SUCCESS"
            
            // Handle Custom Rules
            val customRules = (context.applicationContext as com.example.ShieldApplication).container.ruleRepository.getAllRulesSync()
            
            for (rule in customRules) {
                val trigger = rule.trigger
                val action = rule.action
                if (body.contains(trigger, ignoreCase = true) || sender.contains(trigger, ignoreCase = true)) {
                    Log.d("SmsProcessor", "Custom Rule triggered: $trigger -> $action")
                    if (action.contains("Webhook", ignoreCase = true)) {
                        com.example.shield.ForwardingManager.forwardMessage(context, sender, "[$trigger triggered] $body", "CUSTOM_RULE", webhookUrl, null)
                    }
                    if (action.contains("Tasker", ignoreCase = true) || action.contains("MacroDroid", ignoreCase = true)) {
                        com.example.shield.ForwardingManager.forwardMessage(context, sender, "[$trigger triggered] $body", "CUSTOM_RULE", null, null)
                    }
                    if (action.contains("Forward", ignoreCase = true)) {
                        targetNumbers.forEachIndexed { index, targetNumber ->
                            forwardSms(context, targetNumber, "[$trigger] Fwd from $sender: $body", index)
                        }
                    }
                    if (action.contains("Tasker", ignoreCase = true) || action.contains("MacroDroid", ignoreCase = true) || action.contains("Intent", ignoreCase = true)) {
                        com.example.shield.ForwardingManager.forwardMessage(context, sender, body, trigger, null, null)
                    }
                }
            }

            // Phase 4: Handle Auto Responder for SMS
            com.example.shield.AutoResponder.handleIncomingSms(context, sender, body)
            
            val fwdMsg = "Fwd from $sender: $body"
            
            targetNumbers.forEachIndexed { index, targetNumber ->
                if (isSimulation) {
                    Log.d("SmsProcessor", "Simulated sending to $targetNumber")
                } else {
                    val res = forwardSms(context, targetNumber, fwdMsg, index)
                    if (res != "SUCCESS") {
                        finalStatus = "FAILED"
                    }
                }
            }

            if (!isSimulation) {
                val type = if (com.example.shield.OtpDetector.containsOtp(body)) "OTP" 
                           else if (com.example.shield.MerchantDetector.isMerchantOrBankAlert(context, body)) "TRANSACTION"
                           else "GENERIC_SMS"
                com.example.shield.ForwardingManager.forwardMessage(context, sender, body, type, webhookUrl, null)
            }

            // Synchronize widget state corresponding to message classification
            if (com.example.shield.OtpDetector.containsOtp(body)) {
                com.example.widget.WidgetUpdater.updateWidgetState(context, "OTP", body)
            } else {
                com.example.widget.WidgetUpdater.updateWidgetState(context, "DEFAULT", "Forwarded from $sender")
            }
            
            // DND Bypass for Important Forwarded Messages
            val overrideDnd = settingsRepo.getBooleanSync(androidx.datastore.preferences.core.booleanPreferencesKey("override_dnd"), false)
            if (!isSimulation && overrideDnd) {
                try {
                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                    val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_ALARM)
                    audioManager.setStreamVolume(android.media.AudioManager.STREAM_ALARM, maxVolume, 0)
                    
                    val ringtoneUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                    val ringtone = android.media.RingtoneManager.getRingtone(context, ringtoneUri)
                    ringtone.streamType = android.media.AudioManager.STREAM_ALARM
                    ringtone.play()
                    
                    // Fire a notification event so user knows WHY it alarmed
                    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                        com.example.shield.SystemNotificationEventBus.emitEvent(
                            com.example.shield.SystemEvent.IncomingCallSuspicious(
                                phoneNumber = sender,
                                reason = "DND BYPASS: Important Forwarded SMS."
                            )
                        )
                    }
                    
                    // Stop after 3 seconds
                    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                        kotlinx.coroutines.delay(3000)
                        ringtone.stop()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("SmsProcessor", "Failed to play DND bypass alarm: \\${e.message}")
                }
            }
        }

        val endTime = System.currentTimeMillis()
        val durationMs = Math.max(0L, endTime - startTime)

        if (!isSimulation) {
            var logStatus = finalStatus
            if (finalStatus == "SUCCESS" && targetNumbers.isNotEmpty()) {
                for (targetNumber in targetNumbers) {
                    repo.insertLog(
                        SmsLogEntity(
                            timestamp = timestamp,
                            sender = sender,
                            message = body,
                            targetNumber = targetNumber,
                            status = logStatus
                        )
                    )
                }
            } else if (finalStatus != "SUCCESS") {
                 repo.insertLog(
                    SmsLogEntity(
                        timestamp = timestamp,
                        sender = sender,
                        message = body,
                        targetNumber = "Multiple/None",
                        status = finalStatus
                    )
                )
            }
        }

        return@withContext ProcessingResult(finalStatus, durationMs)
    }

    private fun forwardSms(context: Context, targetNumber: String, message: String, index: Int): String {
        return try {
            val data = androidx.work.Data.Builder()
                .putString("targetNumber", targetNumber)
                .putString("message", message)
                .build()

            val constraints = androidx.work.Constraints.Builder()
                .build()

            val request = androidx.work.OneTimeWorkRequestBuilder<com.example.shield.SmsWorker>()
                .setConstraints(constraints)
                .setInputData(data)
                // Added initial delay for throttling bulk sms (staggered)
                .setInitialDelay(2000L * (index + 1), java.util.concurrent.TimeUnit.MILLISECONDS)
                .setBackoffCriteria(
                    androidx.work.BackoffPolicy.EXPONENTIAL,
                    10000L,
                    java.util.concurrent.TimeUnit.MILLISECONDS
                )
                .build()

            androidx.work.WorkManager.getInstance(context).enqueue(request)
            "SUCCESS"
        } catch (e: Exception) {
            android.util.Log.e("SmsProcessor", "Failed to enqueue SMS", e)
            "FAILED"
        }
    }
}
