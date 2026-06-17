package com.example.network

import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
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

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
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

suspend fun generateContentWithHistory(
    history: List<com.example.data.ChatMessageEntity>,
    prompt: String,
    contextData: String = ""
): String = withContext(Dispatchers.IO) {
    val apiKey = BuildConfig.GEMINI_API_KEY
    if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
        return@withContext "Error: Gemini API Key is missing. Please configure it in the Secrets panel."
    }

    val contents = mutableListOf<Content>()
    
    // Add history (limit to last 20 messages to avoid token bloat)
    val recentHistory = history.takeLast(20)
    for (msg in recentHistory) {
        val role = if (msg.isUser) "user" else "model"
        contents.add(Content(role = role, parts = listOf(Part(text = msg.text))))
    }
    
    // Add current prompt
    contents.add(Content(role = "user", parts = listOf(Part(text = prompt))))
    
    val baseInstruction = "You are KJ, a personal assistant for a smart SMS forwarding and call handling app. You are helpful, concise and smart. Provide directly helpful answers to the user's queries about SMS rules or application usage. Do not invent rules or logs, check the provided context."
    val fullInstruction = if (contextData.isNotBlank()) "$baseInstruction\n\nApp Context & Current Logs:\n$contextData" else baseInstruction

    val request = GenerateContentRequest(
        contents = contents,
        systemInstruction = Content(
            parts = listOf(Part(text = fullInstruction))
        )
    )

    try {
        val response = RetrofitClient.service.generateContent(apiKey, request)
        response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "I am not sure how to respond to that."
    } catch (e: Exception) {
        "Error: ${e.message}"
    }
}
