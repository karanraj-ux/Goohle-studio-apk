package com.example.network

import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.IOException
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val role: String? = null,
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content? = null
)

@JsonClass(generateAdapter = true)
data class ModelListResponse(
    val models: List<AiModel>? = null
)

@JsonClass(generateAdapter = true)
data class AiModel(
    val name: String,
    val version: String,
    val displayName: String,
    val description: String,
    val inputTokenLimit: Int?,
    val outputTokenLimit: Int?,
    val supportedGenerationMethods: List<String>?
)

class RateLimitInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        var response: Response? = null
        var retryCount = 0
        var backoffDelay = 1000L // 1 second initial delay

        while (retryCount < 3) {
            try {
                response = chain.proceed(request)
                if (response.code != 429) {
                    return response
                }
                
                // If 429 Too Many Requests, backoff and retry
                response.close()
                Thread.sleep(backoffDelay)
                backoffDelay *= 2 // Exponential backoff (1s, 2s, 4s)
                retryCount++
            } catch (e: IOException) {
                if (retryCount >= 2) throw e
                Thread.sleep(backoffDelay)
                backoffDelay *= 2
                retryCount++
            }
        }
        
        return response ?: chain.proceed(request)
    }
}

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse

    @GET("v1beta/models")
    suspend fun getModels(
        @Query("key") apiKey: String
    ): ModelListResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(RateLimitInterceptor())
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

suspend fun detectAvailableModels(apiKey: String): List<AiModel> = withContext(Dispatchers.IO) {
    if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
        return@withContext emptyList()
    }
    try {
        val response = RetrofitClient.service.getModels(apiKey)
        response.models?.filter { it.supportedGenerationMethods?.contains("generateContent") == true } ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }
}

suspend fun generateContentWithHistory(
    history: List<com.example.data.ChatMessageEntity>,
    prompt: String,
    contextData: String = "",
    customModel: String = "gemini-2.5-flash",
    customApiKey: String = "",
    provider: String = "Google Gemini",
    context: android.content.Context? = null
): String = withContext(Dispatchers.IO) {
    if (context != null) {
        val prefs = context.getSharedPreferences("global_api_limits", android.content.Context.MODE_PRIVATE)
        val today = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(java.util.Date())
        val count = prefs.getInt("count_$today", 0)
        
        // Zero Credit Loss Loop Avoidance: Global limit of 100 API calls per day
        if (count >= 100) {
            return@withContext "Error: Daily API Safety Limit Exceeded (100). This protects your API Key from unbounded billing. You can reset this in settings or try again tomorrow."
        }
        prefs.edit().putInt("count_$today", count + 1).apply()
    }

    val apiKey = if (customApiKey.isNotBlank()) customApiKey else BuildConfig.GEMINI_API_KEY
    if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
        return@withContext "Error: API Key is missing. Please configure it in the Secrets panel or app settings."
    }

    val currentTime = java.util.Date(System.currentTimeMillis()).toString()
    val baseInstruction = "You are KJ, a Retrospective AI personal assistant for a smart SMS forwarding and call handling app. You have read-only access to the user's local SQLite database logs (SMS, calls, finances). Always provide precise factual answers based ONLY on the provided App Context & Current Logs. Be helpful, concise and smart. Do not invent rules or logs, check the provided context.\nThe current system time is: $currentTime."
    val fullInstruction = if (contextData.isNotBlank()) "$baseInstruction\n\nApp Context & Current Logs:\n$contextData" else baseInstruction

    when (provider) {
        "OpenAI", "Groq", "DeepSeek", "Custom MCP" -> {
            val url = when (provider) {
                "Groq" -> "https://api.groq.com/openai/v1/chat/completions"
                "DeepSeek" -> "https://api.deepseek.com/v1/chat/completions"
                else -> "https://api.openai.com/v1/chat/completions" // OpenAI and Custom MCP (fallback for now)
            }
            return@withContext callOpenAiCompatibleApi(url, history, prompt, fullInstruction, apiKey, customModel)
        }
        "Anthropic" -> {
            return@withContext callAnthropicApi(history, prompt, fullInstruction, apiKey, customModel)
        }
        else -> {
            // Google Gemini
            val contents = mutableListOf<Content>()
            
            // Add history (limit to last 20 messages to avoid token bloat)
            val recentHistory = history.takeLast(20)
            for (msg in recentHistory) {
                val role = if (msg.isUser) "user" else "model"
                contents.add(Content(role = role, parts = listOf(Part(text = msg.text))))
            }
            
            // Add current prompt
            contents.add(Content(role = "user", parts = listOf(Part(text = prompt))))

            val request = GenerateContentRequest(
                contents = contents,
                systemInstruction = Content(
                    parts = listOf(Part(text = fullInstruction))
                )
            )

            try {
                val modelParam = if (customModel.startsWith("models/")) customModel.removePrefix("models/") else customModel
                val response = RetrofitClient.service.generateContent(modelParam, apiKey, request)
                return@withContext response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "I am not sure how to respond to that."
            } catch (e: Exception) {
                return@withContext "Error: ${e.message}"
            }
        }
    }
}

private suspend fun callOpenAiCompatibleApi(url: String, history: List<com.example.data.ChatMessageEntity>, prompt: String, systemInstruction: String, apiKey: String, model: String): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
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
    
    history.takeLast(20).forEach { msg ->
        val msgObj = org.json.JSONObject()
        msgObj.put("role", if (msg.isUser) "user" else "assistant")
        msgObj.put("content", msg.text)
        messages.put(msgObj)
    }

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
            if (!response.isSuccessful) return@withContext "Error: HTTP ${response.code} ${response.message}"
            val responseBody = response.body?.string() ?: return@withContext "Error: Empty response"
            val responseJson = org.json.JSONObject(responseBody)
            responseJson.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
        }
    } catch (e: Exception) {
        "Error: ${e.message}"
    }
}

private suspend fun callAnthropicApi(history: List<com.example.data.ChatMessageEntity>, prompt: String, systemInstruction: String, apiKey: String, model: String): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    if (apiKey.isBlank()) return@withContext "Error: Anthropic API Key is missing."
    val client = okhttp3.OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    val json = org.json.JSONObject()
    json.put("model", model)
    json.put("max_tokens", 4096)
    json.put("system", systemInstruction)
    val messages = org.json.JSONArray()
    
    history.takeLast(20).forEach { msg ->
        val msgObj = org.json.JSONObject()
        msgObj.put("role", if (msg.isUser) "user" else "assistant")
        msgObj.put("content", msg.text)
        messages.put(msgObj)
    }

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
            if (!response.isSuccessful) return@withContext "Error: HTTP ${response.code} ${response.message}"
            val responseBody = response.body?.string() ?: return@withContext "Error: Empty response"
            val responseJson = org.json.JSONObject(responseBody)
            responseJson.getJSONArray("content").getJSONObject(0).getString("text")
        }
    } catch (e: Exception) {
        "Error: ${e.message}"
    }
}
