package com.example

import android.content.Context
import android.telephony.SmsManager
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.SmsLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ProcessingResult(val status: String, val durationMs: Long)

object SmsProcessor {
    suspend fun processReceivedMessage(context: Context, sender: String, body: String, timestamp: Long = System.currentTimeMillis(), isSimulation: Boolean = false): ProcessingResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val prefs = context.getSharedPreferences("sms_forwarder_prefs", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("is_enabled", false)

        var targetNumbersStr = prefs.getString("target_numbers", "") ?: ""
        if (targetNumbersStr.isBlank()) {
            val legacy = prefs.getString("target_number", "") ?: ""
            if (legacy.isNotBlank()) targetNumbersStr = legacy
        }

        var sendersStr = prefs.getString("senders", "") ?: ""
        if (sendersStr.isBlank()) {
            val legacy = prefs.getString("sender_filter", "") ?: ""
            if (legacy.isNotBlank()) sendersStr = legacy
        }

        val keywordFilter = prefs.getString("keyword_filter", "") ?: ""

        val targetNumbers = targetNumbersStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val senders = sendersStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        val db = AppDatabase.getDatabase(context)

        if (!isEnabled && !isSimulation) {
            return@withContext ProcessingResult("IGNORED", 0L)
        }

        if (targetNumbers.isEmpty()) {
            return@withContext ProcessingResult("FAILED_NO_TARGET", 0L)
        }

        val senderMatches = senders.isEmpty() || senders.any { sender.contains(it, ignoreCase = true) }
        val keywordMatches = keywordFilter.isBlank() || body.contains(keywordFilter, ignoreCase = true)

        var finalStatus = "IGNORED"

        if (senderMatches && keywordMatches) {
            finalStatus = "SUCCESS"
            val fwdMsg = "Fwd from $sender: $body"
            
            for (targetNumber in targetNumbers) {
                if (isSimulation) {
                    Log.d("SmsProcessor", "Simulated sending to $targetNumber")
                } else {
                    val res = forwardSms(context, targetNumber, fwdMsg)
                    if (res != "SUCCESS") {
                        finalStatus = "FAILED"
                    }
                }
            }
        }

        val endTime = System.currentTimeMillis()
        val durationMs = Math.max(0L, endTime - startTime)

        if (!isSimulation) {
            var logStatus = finalStatus
            if (finalStatus == "SUCCESS" && targetNumbers.isNotEmpty()) {
                for (targetNumber in targetNumbers) {
                    db.smsLogDao().insert(
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
                 db.smsLogDao().insert(
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

    private fun forwardSms(context: Context, targetNumber: String, message: String): String {
        return try {
            val sm: SmsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            // Send the physical text message
            sm.sendTextMessage(targetNumber, null, message, null, null)
            "SUCCESS"
        } catch (e: Exception) {
            Log.e("SmsProcessor", "Failed to forward SMS", e)
            "FAILED"
        }
    }
}
