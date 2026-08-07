package com.example.shield

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.launch

object ForwardingManager {

    fun forwardMessage(
        context: Context,
        title: String,
        message: String,
        type: String,
        webhookUrl: String?,
        forwardPhone: String?
    ) {
        // Forward via SMS if configured
        if (!forwardPhone.isNullOrBlank()) {
            sendSms(context, forwardPhone, "[$type] $title: $message")
        }

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            val settingsRepo = (context.applicationContext as com.example.ShieldApplication).container.settingsRepository
            val advancedWebhooks = (context.applicationContext as com.example.ShieldApplication).container.webhookRepository.getAllWebhooksSync()
            
            // Broadcast Intent for Tasker / MacroDroid / other automation tools
            val allowAutomation = settingsRepo.getBooleanSync(androidx.datastore.preferences.core.booleanPreferencesKey("allow_external_automation"), false)
            if (allowAutomation) {
                try {
                    val intent = android.content.Intent("com.example.shield.RULE_TRIGGERED")
                    intent.putExtra("type", type)
                    intent.putExtra("title", title)
                    intent.putExtra("message", message)
                    intent.putExtra("rule_name", type)
                    context.sendBroadcast(intent)
                    Log.d("ForwardingManager", "Broadcasted automation intent: com.example.shield.RULE_TRIGGERED")
                } catch (e: Exception) {
                    Log.e("ForwardingManager", "Failed to broadcast automation intent", e)
                }
            }

            for (webhook in advancedWebhooks) {
                sendWebhook(context, webhook.url, title, message, type, webhook.method, webhook.headersJson, webhook.customPayload)
            }
            
            val hasLegacyWebhook = !webhookUrl.isNullOrBlank() && webhookUrl.startsWith("http")
            if (hasLegacyWebhook) {
                val webhookFilter = settingsRepo.getStringSync(com.example.data.repository.SettingsRepository.WEBHOOK_FILTER)
                
                val shouldForward = when (webhookFilter) {
                    "ALL" -> true
                    "OTP" -> type.equals("OTP", ignoreCase = true)
                    "TRANSACTION" -> type.equals("TRANSACTION", ignoreCase = true)
                    "MISSED_CALL" -> type.equals("MISSED_CALL", ignoreCase = true)
                    else -> true
                }
                
                if (shouldForward) {
                    sendWebhook(context, webhookUrl!!, title, message, type, "POST", "{}", "")
                }
            }
        }
    }

    private fun sendSms(context: Context, phoneNumber: String, content: String) {
        try {
            val data = Data.Builder()
                .putString("targetNumber", phoneNumber)
                .putString("message", content)
                .build()

            val constraints = Constraints.Builder()
                .build()

            val request = OneTimeWorkRequestBuilder<SmsWorker>()
                .setConstraints(constraints)
                .setInputData(data)
                .setBackoffCriteria(
                    androidx.work.BackoffPolicy.EXPONENTIAL,
                    10000L,
                    java.util.concurrent.TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueue(request)
            Log.d("ForwardingManager", "Forwarded SMS queued")
        } catch (e: Exception) {
            Log.e("ForwardingManager", "Failed to queue forward SMS", e)
        }
    }

    private fun sendWebhook(context: Context, url: String, title: String, message: String, type: String, method: String, headersJson: String, customPayload: String) {
        val data = Data.Builder()
            .putString("url", url)
            .putString("title", title)
            .putString("message", message)
            .putString("type", type)
            .putString("method", method)
            .putString("headersJson", headersJson)
            .putString("customPayload", customPayload)
            .build()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<WebhookWorker>()
            .setConstraints(constraints)
            .setInputData(data)
            .setBackoffCriteria(
                androidx.work.BackoffPolicy.EXPONENTIAL,
                10000L,
                java.util.concurrent.TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueue(request)
        Log.d("ForwardingManager", "Webhook enqueued in WorkManager")
    }
}
