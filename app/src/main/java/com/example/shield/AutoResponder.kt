package com.example.shield

import android.content.Context
import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.network.generateContentWithHistory

object AutoResponder {

    fun handleMissedCall(context: Context, contactName: String, phoneNumber: String) {
        (context.applicationContext as com.example.ShieldApplication).applicationScope.launch(Dispatchers.IO) {
            val settingsRepo = (context.applicationContext as com.example.ShieldApplication).container.settingsRepository
            val autoRespondMissedCall = settingsRepo.getBooleanSync(com.example.data.repository.SettingsRepository.AUTO_RESPOND_MISSED_CALL, false)
            
            if (autoRespondMissedCall && phoneNumber.isNotBlank() && isSafeToReply(context, phoneNumber)) {
                val replyMessage = generateAiReply(context, "MISSED_CALL", contactName, "")
                sendSms(context, phoneNumber, replyMessage)
            }
        }
    }

    fun handleIncomingSms(context: Context, sender: String, message: String) {
        (context.applicationContext as com.example.ShieldApplication).applicationScope.launch(Dispatchers.IO) {
            val settingsRepo = (context.applicationContext as com.example.ShieldApplication).container.settingsRepository
            val autoRespondSms = settingsRepo.getBooleanSync(com.example.data.repository.SettingsRepository.AUTO_RESPOND_SMS, false)
            
            if (autoRespondSms && sender.isNotBlank() && isSafeToReply(context, sender)) {
                val replyMessage = generateAiReply(context, "SMS", sender, message)
                sendSms(context, sender, replyMessage)
            }
        }
    }

    private fun isSafeToReply(context: Context, phoneNumber: String): Boolean {
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

    private suspend fun generateAiReply(context: Context, eventType: String, sender: String, message: String): String {
        val prefs = com.example.utils.SecurityUtils.getEncryptedPrefs(context, "kj_ai_prefs")
        val engineType = prefs.getString("engine_type", "LOCAL") ?: "LOCAL"
        val systemPrompt = prefs.getString("system_prompt", "Summarize this long email notification into 3 bullet points.") ?: ""
        val customApiKey = prefs.getString("custom_api_key", "") ?: ""
        var selectedModel = prefs.getString("selected_model", "") ?: ""
        
        // Auto-detect provider based on API key prefix
        var provider = prefs.getString("api_provider", "OpenAI")
        if (customApiKey.startsWith("gsk_")) {
            provider = "Groq"
            if (selectedModel.isBlank()) selectedModel = "llama3-8b-8192"
        } else if (customApiKey.startsWith("sk-ant")) {
            provider = "Anthropic"
            if (selectedModel.isBlank()) selectedModel = "claude-3-haiku-20240307"
        } else if (customApiKey.startsWith("AIza")) {
            provider = "Google Gemini"
            if (selectedModel.isBlank()) selectedModel = "gemini-1.5-flash"
        } else if (customApiKey.startsWith("sk-")) {
            provider = "OpenAI"
            if (selectedModel.isBlank()) selectedModel = "gpt-4o-mini"
        }

        // Use the system configuration to process the incoming text
        return when (engineType) {
            "API" -> {
                Log.d("AutoResponder", "Using BYOK Cloud API ($provider) with prompt: $systemPrompt")
                
                val promptText = if (eventType == "MISSED_CALL") {
                    "You received a missed call from $sender. Generate an appropriate auto-reply message."
                } else {
                    "You received an SMS from $sender: '$message'. Generate an appropriate auto-reply message."
                }

                if (provider == "Google Gemini" || provider == "Gemini") {
                    generateContentWithHistory(
                        history = emptyList(),
                        prompt = promptText,
                        contextData = systemPrompt,
                        customModel = selectedModel,
                        customApiKey = customApiKey,
                        context = context
                    )
                } else if (provider == "OpenAI") {
                    callOpenAiCompatibleApi("https://api.openai.com/v1/chat/completions", promptText, systemPrompt, customApiKey, selectedModel)
                } else if (provider == "Groq") {
                    callOpenAiCompatibleApi("https://api.groq.com/openai/v1/chat/completions", promptText, systemPrompt, customApiKey, selectedModel)
                } else if (provider == "Anthropic") {
                    callAnthropicApi(promptText, systemPrompt, customApiKey, selectedModel)
                } else {
                    "[$provider] I received your message. (Auto-reply via API)"
                }
            }
            else -> {
                when (eventType) {
                    "MISSED_CALL" -> "Hi, I'm currently unavailable. KJ AI personal assistant speaking. How can I help you?"
                    "SMS" -> "Hi, I received your message. I'm currently away. (Auto-reply via KJ Engine)"
                    else -> "Hello! I am currently away."
                }
            }
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

    private suspend fun callOpenAiCompatibleApi(url: String, prompt: String, systemInstruction: String, apiKey: String, model: String): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext "Error: API Key is missing."
        val client = okhttp3.OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        val json = org.json.JSONObject()
        json.put("model", model)
        val messages = org.json.JSONArray()
        val sysMsg = org.json.JSONObject()
        sysMsg.put("role", "system")
        sysMsg.put("content", systemInstruction)
        messages.put(sysMsg)
        val userMsg = org.json.JSONObject()
        userMsg.put("role", "user")
        userMsg.put("content", prompt)
        messages.put(userMsg)
        json.put("messages", messages)

        val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = okhttp3.Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext "Error: ${response.code}"
                val responseBody = response.body?.string() ?: return@withContext "Error: Empty response"
                val responseJson = org.json.JSONObject(responseBody)
                responseJson.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
            }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    private suspend fun callAnthropicApi(prompt: String, systemInstruction: String, apiKey: String, model: String): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext "Error: Anthropic API Key is missing."
        val client = okhttp3.OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        val json = org.json.JSONObject()
        json.put("model", model)
        json.put("max_tokens", 1024)
        json.put("system", systemInstruction)
        val messages = org.json.JSONArray()
        val userMsg = org.json.JSONObject()
        userMsg.put("role", "user")
        userMsg.put("content", prompt)
        messages.put(userMsg)
        json.put("messages", messages)

        val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = okhttp3.Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext "Error: ${response.code}"
                val responseBody = response.body?.string() ?: return@withContext "Error: Empty response"
                val responseJson = org.json.JSONObject(responseBody)
                responseJson.getJSONArray("content").getJSONObject(0).getString("text")
            }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}
