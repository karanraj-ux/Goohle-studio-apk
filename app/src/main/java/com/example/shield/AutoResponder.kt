package com.example.shield

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AutoResponder {
    fun handleMissedCall(context: Context, phoneNumber: String, contactName: String) {
        (context.applicationContext as com.example.ShieldApplication).applicationScope.launch(Dispatchers.IO) {
            try {
            val settingsRepo = (context.applicationContext as com.example.ShieldApplication).container.settingsRepository
            val autoRespondMissedCall = settingsRepo.getBooleanSync(com.example.data.repository.SettingsRepository.AUTO_RESPOND_MISSED_CALL, false)
            
            if (autoRespondMissedCall && phoneNumber.isNotBlank() && isSafeToReply(context, phoneNumber)) {
                val replyMessage = generateReply(context, "MISSED_CALL", contactName, "")
                sendSms(context, phoneNumber, replyMessage)
            }
            } catch (e: Exception) {
                Log.e("AutoResponder", "Crash prevented in handleMissedCall", e)
            }
        }
    }

    fun handleIncomingSms(context: Context, sender: String, message: String) {
        (context.applicationContext as com.example.ShieldApplication).applicationScope.launch(Dispatchers.IO) {
            try {
            val settingsRepo = (context.applicationContext as com.example.ShieldApplication).container.settingsRepository
            val autoRespondSms = settingsRepo.getBooleanSync(com.example.data.repository.SettingsRepository.AUTO_RESPOND_SMS, false)
            
            if (autoRespondSms && sender.isNotBlank() && isSafeToReply(context, sender)) {
                val replyMessage = generateReply(context, "SMS", sender, message)
                sendSms(context, sender, replyMessage)
            }
            } catch (e: Exception) {
                Log.e("AutoResponder", "Crash prevented in handleIncomingSms", e)
            }
        }
    }

    private fun isSafeToReply(context: Context, phoneNumber: String): Boolean {
        // Check Relationship Tier
        val tier = com.example.calls.CallHandlingManager.getRelationshipTier(context, phoneNumber)
        if (tier == "Blocked" || tier == "Muted") {
            Log.d("AutoResponder", "Loop Prevention: Caller is $tier. No auto-reply sent to $phoneNumber.")
            return false
        }

        // Basic loop/bot avoidance: Ignore short codes or empty numbers
        val strippedNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
        if (strippedNumber.length < 7) {
            Log.w("AutoResponder", "Loop Prevention: Ignoring short code or invalid number: $phoneNumber")
            return false
        }
        val prefs = context.getSharedPreferences("auto_reply_history", Context.MODE_PRIVATE)
        val currentTime = System.currentTimeMillis()
        val lastReplyTime = prefs.getLong("time_$strippedNumber", 0L)
        val replyCount = prefs.getInt("count_$strippedNumber", 0)
        // Reset rate limit window every 1 hour (3600000 ms)
        if (currentTime - lastReplyTime > 3600000) {
            prefs.edit()
                .putLong("time_$strippedNumber", currentTime)
                .putInt("count_$strippedNumber", 1)
                .apply()
            return true
        }
        // Limit to 3 auto-replies per hour to same number to avoid loops (zero credit loss)
        if (replyCount >= 3) {
            Log.w("AutoResponder", "Loop Prevention: Rate limit exceeded for auto-replies to $strippedNumber")
            return false
        }
        
        prefs.edit()
            .putInt("count_$strippedNumber", replyCount + 1)
            .putLong("time_$strippedNumber", currentTime)
            .apply()
        return true
    }
    private suspend fun generateReply(context: Context, eventType: String, sender: String, message: String): String {
        val settingsRepo = (context.applicationContext as com.example.ShieldApplication).container.settingsRepository
        val customBusyMsg = settingsRepo.getStringSync(com.example.data.repository.SettingsRepository.BUSY_REPLY_MSG, "")
        
        if (customBusyMsg.isNotBlank()) {
            return customBusyMsg
        }
        
        return when (eventType) {
            "MISSED_CALL" -> "Hi, I'm currently unavailable or focused. I will call you back later."
            "SMS" -> "Hi, I'm currently away. I've received your message and will respond when I can."
            else -> "Hello! I am currently away."
        }
    }
    private fun sendSms(context: Context, phoneNumber: String, content: String) {
        try {
            val data = androidx.work.Data.Builder()
                .putString("targetNumber", phoneNumber)
                .putString("message", content)
                .build()

            val request = androidx.work.OneTimeWorkRequestBuilder<SmsWorker>()
                .setInputData(data)
                .setBackoffCriteria(
                    androidx.work.BackoffPolicy.EXPONENTIAL,
                    10000L,
                    java.util.concurrent.TimeUnit.MILLISECONDS
                )
                .build()

            androidx.work.WorkManager.getInstance(context).enqueue(request)
            Log.d("AutoResponder", "Auto-responded via SMS to $phoneNumber")
        } catch (e: Exception) {
            Log.e("AutoResponder", "Failed to auto-respond via SMS", e)
        }
    }
}
