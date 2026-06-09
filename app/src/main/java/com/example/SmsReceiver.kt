package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log

class SmsReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "SmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val prefs = context.getSharedPreferences("sms_forwarder_prefs", Context.MODE_PRIVATE)
            val isEnabled = prefs.getBoolean("is_enabled", false)
            val targetNumber = prefs.getString("target_number", "") ?: ""
            val senderFilter = prefs.getString("sender_filter", "") ?: ""
            val keywordFilter = prefs.getString("keyword_filter", "") ?: ""

            if (!isEnabled || targetNumber.isBlank()) {
                Log.d(TAG, "Forwarding disabled or target number empty.")
                return
            }

            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (sms in messages) {
                val sender = sms.displayOriginatingAddress ?: ""
                val body = sms.displayMessageBody ?: ""

                Log.d(TAG, "Received SMS from: $sender")

                val senderMatches = senderFilter.isBlank() || sender.contains(senderFilter, ignoreCase = true)
                val keywordMatches = keywordFilter.isBlank() || body.contains(keywordFilter, ignoreCase = true)

                if (senderMatches && keywordMatches) {
                    Log.d(TAG, "Match found! Forwarding to $targetNumber")
                    forwardSms(context, targetNumber, "Fwd from $sender: $body")
                } else {
                    Log.d(TAG, "SMS ignored. SenderMatches: $senderMatches, KeywordMatches: $keywordMatches")
                }
            }
        }
    }

    private fun forwardSms(context: Context, targetNumber: String, message: String) {
        try {
            val sm: SmsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            sm.sendTextMessage(targetNumber, null, message, null, null)
            Log.d(TAG, "Successfully forwarded SMS to $targetNumber")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to forward SMS", e)
        }
    }
}
