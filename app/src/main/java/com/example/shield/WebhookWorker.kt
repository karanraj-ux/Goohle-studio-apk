package com.example.shield

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WebhookWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val client = OkHttpClient()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (androidx.core.content.ContextCompat.checkSelfPermission(applicationContext, android.Manifest.permission.INTERNET) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.e("WebhookWorker", "Permission denied, silently exiting.")
            return@withContext Result.failure()
        }
        val url = inputData.getString("url") ?: return@withContext Result.failure()
        val type = inputData.getString("type") ?: ""
        val title = inputData.getString("title") ?: ""
        val message = inputData.getString("message") ?: ""
        val method = inputData.getString("method") ?: "POST"
        val customPayloadTemplate = inputData.getString("customPayload")
        val headersJsonStr = inputData.getString("headersJson") ?: "{}"

        val requestBodyStr = if (!customPayloadTemplate.isNullOrBlank()) {
            customPayloadTemplate
                .replace("{sender}", title)
                .replace("{message}", message)
                .replace("{type}", type)
        } else {
            val json = JSONObject().apply {
                put("type", type)
                put("title", title)
                put("sender", title) // Map title to sender for clarity
                put("message", message)
                put("text", message)
                put("source", "KjAiShield")
                put("device_timestamp", System.currentTimeMillis())
                put("timestamp", System.currentTimeMillis())
            }
            json.toString()
        }

        val requestBuilder = Request.Builder().url(url)

        try {
            val headersObj = JSONObject(headersJsonStr)
            headersObj.keys().forEach { key ->
                requestBuilder.addHeader(key, headersObj.getString(key))
            }
        } catch (e: Exception) {
            Log.e("WebhookWorker", "Failed to parse headers JSON", e)
        }

        if (method.equals("POST", ignoreCase = true)) {
            val reqBody = requestBodyStr.toRequestBody("application/json; charset=utf-8".toMediaType())
            requestBuilder.post(reqBody)
        } else if (method.equals("PUT", ignoreCase = true)) {
            val reqBody = requestBodyStr.toRequestBody("application/json; charset=utf-8".toMediaType())
            requestBuilder.put(reqBody)
        } else if (method.equals("GET", ignoreCase = true)) {
            requestBuilder.get()
        }

        val request = requestBuilder.build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Log.d("WebhookWorker", "Webhook successfully sent")
                Result.success()
            } else {
                Log.e("WebhookWorker", "Webhook failed: ${response.code}")
                // Retry if it's a server error
                if (response.code in 500..599) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
        } catch (e: Exception) {
            Log.e("WebhookWorker", "Exception while sending webhook", e)
            Result.retry() // Retry on network failure
        }
    }
}
