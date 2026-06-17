package com.example.shield

import android.content.Context
import android.telephony.SmsManager
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object ForwardingManager {
    private val client = OkHttpClient()

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

        // Forward via Webhook if configured
        if (!webhookUrl.isNullOrBlank() && webhookUrl.startsWith("http")) {
            sendWebhook(webhookUrl, title, message, type)
        }
    }

    private fun sendSms(context: Context, phoneNumber: String, content: String) {
        try {
            val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            val parts = smsManager.divideMessage(content)
            smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
            Log.d("ForwardingManager", "Forwarded SMS to $phoneNumber")
        } catch (e: Exception) {
            Log.e("ForwardingManager", "Failed to forward SMS", e)
        }
    }

    private fun sendWebhook(url: String, title: String, message: String, type: String) {
        val json = JSONObject().apply {
            put("type", type)
            put("title", title)
            put("message", message)
            put("timestamp", System.currentTimeMillis())
        }

        val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ForwardingManager", "Webhook forwarding failed", e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.close()
                Log.d("ForwardingManager", "Webhook successfully sent to DB/Server")
            }
        })
    }
}
