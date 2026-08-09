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
    suspend fun processReceivedMessage(context: Context, sender: String, body: String, slotIndex: Int = -1, timestamp: Long = System.currentTimeMillis(), isSimulation: Boolean = false): ProcessingResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val settingsRepo = (context.applicationContext as com.example.ShieldApplication).container.settingsRepository
        
        val isEnabled = settingsRepo.getBooleanSync(SettingsRepository.SMS_FORWARDING_ENABLED, false)
        
        val isKillSwitchOn = settingsRepo.getBooleanSync(androidx.datastore.preferences.core.booleanPreferencesKey("master_kill_switch"), false)
        
        if (isKillSwitchOn) {
            return@withContext ProcessingResult("KILLED_BY_MASTER_SWITCH", System.currentTimeMillis() - startTime)
        }
        
        // Auto-pause ghost mode for 1 hour if a delivery/service SMS is received.
        val lowerBody = body.lowercase()
        val isServiceMsg = sender.length <= 8 || sender.contains("-") || sender.any { it.isLetter() }
        val hasDeliveryKeyword = listOf("otp", "delivery", "arriving", "arrived", "zomato", "swiggy", "uber", "amazon", "flipkart", "order", "driver").any { lowerBody.contains(it) }
        
        if (isServiceMsg && hasDeliveryKeyword && !isSimulation) {
            val oneHourMs = 60 * 60 * 1000L
            val pauseEndTime = System.currentTimeMillis() + oneHourMs
            kotlinx.coroutines.runBlocking { settingsRepo.updateLong(SettingsRepository.GHOST_MODE_PAUSE_END_TIME, pauseEndTime) }
            Log.d("SmsProcessor", "Delivery/Service SMS detected. Paused Ghost Mode for 1 hour.")
        }
        
        val sendersStr = settingsRepo.getStringSync(SettingsRepository.SENDERS, "")
        val targetNumbersStr = settingsRepo.getStringSync(SettingsRepository.TARGET_NUMBERS, "")
        val smsForwardTarget = settingsRepo.getStringSync(SettingsRepository.SMS_FORWARD_TARGET, "")
        val keywordFilter = settingsRepo.getStringSync(SettingsRepository.KEYWORD_FILTER, "")
        val webhookUrl = settingsRepo.getStringSync(androidx.datastore.preferences.core.stringPreferencesKey("webhook_url"), "")
        
        val targetNumbers = targetNumbersStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val senders = sendersStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val repo = (context.applicationContext as com.example.ShieldApplication).container.smsRepository

        if (!isEnabled && !isSimulation) {
            return@withContext ProcessingResult("IGNORED", 0L)
        }

        val senderMatches = senders.isEmpty() || senders.any { sender.contains(it, ignoreCase = true) }
        val keywordMatches = keywordFilter.isBlank() || body.contains(keywordFilter, ignoreCase = true)
        
        var finalStatus = "IGNORED"

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
                    if (action.startsWith("Forward to ", ignoreCase = true)) {
                        val remaining = action.substring("Forward to ".length).trim()
                        var targetNum = remaining
                        var delayMinutes = 0L
                        if (remaining.contains(" after ")) {
                            val parts = remaining.split(" after ")
                            targetNum = parts[0].trim()
                            val delayStr = parts[1].replace("m", "").trim()
                            delayMinutes = delayStr.toLongOrNull() ?: 0L
                        }
                        
                        forwardSms(context, targetNum, "[$trigger] Fwd from $sender: $body", 0, delayMinutes * 60 * 1000)
                    } else if (action.contains("Forward", ignoreCase = true)) {
                        val allTargets = (targetNumbers + if(smsForwardTarget.isNotEmpty()) listOf(smsForwardTarget) else emptyList()).distinct()
            allTargets.forEachIndexed { index, targetNumber ->
                            forwardSms(context, targetNumber, "[$trigger] Fwd from $sender: $body", index)
                        }
                    }
                }
            }

            // Phase 4: Handle Auto Responder for SMS
            com.example.shield.AutoResponder.handleIncomingSms(context, sender, body)
            
            // Phase 5: Telecom SMS Call-Scheduling
            val telecomKeywords = listOf("is now available", "missed call from")
            if (telecomKeywords.any { body.contains(it, ignoreCase = true) }) {
                // Extract number (basic regex for Indian/international numbers)
                val matcher = java.util.regex.Pattern.compile("\\+?\\d{10,14}").matcher(body)
                if (matcher.find()) {
                    val availableNumber = matcher.group()
                    android.util.Log.d("SmsProcessor", "Telecom notification detected for $availableNumber. Preparing scheduled call.")
                    
                    try {
                        val callIntent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                            data = android.net.Uri.parse("tel:$availableNumber")
                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        val pendingIntent = android.app.PendingIntent.getActivity(
                            context,
                            availableNumber.hashCode(),
                            callIntent,
                            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                        )
                        
                        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                        val notif = androidx.core.app.NotificationCompat.Builder(context, "general")
                            .setSmallIcon(android.R.drawable.ic_menu_call)
                            .setContentTitle("Contact Available")
                            .setContentText("$availableNumber is available. Tap to call.")
                            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                            .addAction(android.R.drawable.ic_menu_call, "Call Now", pendingIntent)
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true)
                            .build()
                        nm.notify(availableNumber.hashCode(), notif)
                    } catch (e: Exception) {
                        android.util.Log.e("SmsProcessor", "Failed to show call notification", e)
                    }

                    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                        com.example.shield.SystemNotificationEventBus.emitEvent(
                            com.example.shield.SystemEvent.IncomingCallSuspicious(
                                phoneNumber = availableNumber,
                                reason = "Ready-to-go scheduled call from telecom SMS."
                            )
                        )
                    }
                }
            }
            
            
            val extractOtps = settingsRepo.getBooleanSync(com.example.data.repository.SettingsRepository.EXTRACT_OTPS, false)
            var fwdMsg = "Fwd from $sender: $body"
            
            if (extractOtps) {
                val otpMatcher = java.util.regex.Pattern.compile("\\b\\d{4,8}\\b").matcher(body)
                if (otpMatcher.find()) {
                    val otp = otpMatcher.group()
                    fwdMsg = "OTP: $otp"
                } else {
                    fwdMsg = "Fwd from $sender: $body" // Fallback if no OTP found
                }
            }

            
            val allTargets = (targetNumbers + if(smsForwardTarget.isNotEmpty()) listOf(smsForwardTarget) else emptyList()).distinct()
            allTargets.forEachIndexed { index, targetNumber ->
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
                val type = "GENERIC_SMS"
                com.example.shield.ForwardingManager.forwardMessage(context, sender, body, type, webhookUrl, null)
            }

            com.example.widget.WidgetUpdater.updateWidgetState(context, "DEFAULT", "Forwarded from $sender")
            
            // DND Bypass for Important Forwarded Messages
            val overrideDnd = settingsRepo.getBooleanSync(androidx.datastore.preferences.core.booleanPreferencesKey("override_dnd"), false)
            if (!isSimulation && overrideDnd) {
                try {
                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                    val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_ALARM)
                    audioManager.setStreamVolume(android.media.AudioManager.STREAM_ALARM, maxVolume, 0)
                    
                    val ringtoneUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                    val ringtone = android.media.RingtoneManager.getRingtone(context, ringtoneUri)
                    val audioAttributes = android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                    ringtone.audioAttributes = audioAttributes
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
                    android.util.Log.e("SmsProcessor", "Failed to play DND bypass alarm: ${e.message}")
                }
            }
        }

        val endTime = System.currentTimeMillis()
        val durationMs = Math.max(0L, endTime - startTime)

        if (!isSimulation) {
            var logStatus = finalStatus
            val allTargetsForLog = (targetNumbers + if(smsForwardTarget.isNotEmpty()) listOf(smsForwardTarget) else emptyList()).distinct()
            if (finalStatus == "SUCCESS" && allTargetsForLog.isNotEmpty()) {
                for (targetNumber in allTargetsForLog) {
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

    private fun forwardSms(context: Context, targetNumber: String, message: String, index: Int, explicitDelayMs: Long = 0): String {
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
                .setInitialDelay(if (explicitDelayMs > 0) explicitDelayMs else 2000L * (index + 1), java.util.concurrent.TimeUnit.MILLISECONDS)
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
