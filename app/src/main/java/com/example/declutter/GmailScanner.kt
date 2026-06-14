package com.example.declutter

import android.accounts.Account
import android.content.Context
import com.example.data.AppDatabase
import com.example.data.SubscriptionEntity
import com.google.android.gms.auth.GoogleAuthUtil
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface GmailApiService {
    @GET("gmail/v1/users/me/messages")
    suspend fun getMessages(
        @Header("Authorization") authHeader: String,
        @Query("q") query: String,
        @Query("maxResults") maxResults: Int = 100
    ): MessageListResponse

    @GET("gmail/v1/users/me/messages/{id}")
    suspend fun getMessage(
        @Header("Authorization") authHeader: String,
        @Path("id") id: String,
        @Query("format") format: String = "metadata",
        @Query("metadataHeaders") metadataHeaders: List<String> = listOf("From", "List-Unsubscribe")
    ): Message
}

data class MessageListResponse(val messages: List<MessageId>?)
data class MessageId(val id: String, val threadId: String)
data class Message(val id: String, val payload: MessagePayload?)
data class MessagePayload(val headers: List<HeaderInfo>?)
data class HeaderInfo(val name: String, val value: String)

object GmailScanner {
    suspend fun scanGmail(context: Context, account: Account) = withContext(Dispatchers.IO) {
        try {
            val scope = "oauth2:https://www.googleapis.com/auth/gmail.readonly"
            val token = GoogleAuthUtil.getToken(context, account, scope)
            val authHeader = "Bearer $token"

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl("https://gmail.googleapis.com/")
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            val service = retrofit.create(GmailApiService::class.java)

            // Find newsletters by searching for "unsubscribe"
            val listResponse = service.getMessages(authHeader, query = "unsubscribe")
            val messages = listResponse.messages ?: return@withContext

            val db = AppDatabase.getDatabase(context)

            val regexSenderEmail = Regex("<(.*?)>")
            
            for (msg in messages) {
                val messageDetails = service.getMessage(authHeader, msg.id)
                val headers = messageDetails.payload?.headers ?: continue
                
                val fromHeader = headers.find { it.name.equals("From", ignoreCase = true) }?.value ?: continue
                
                // Only consider it a newsletter if it has a list-unsubscribe header
                val unsubscribeHeader = headers.find { it.name.equals("List-Unsubscribe", ignoreCase = true) }?.value
                if (unsubscribeHeader != null) {
                    // Extract name from "Sender Name <email@domain.com>"
                    var name = fromHeader
                    val emailMatch = regexSenderEmail.find(fromHeader)
                    if (emailMatch != null) {
                        name = fromHeader.replace(emailMatch.value, "").trim().trim('"')
                        if (name.isEmpty()) name = emailMatch.groupValues[1]
                    }

                    val sub = SubscriptionEntity(
                        name = name,
                        amount = "0.00", // Free or unknown
                        dateDetected = System.currentTimeMillis(),
                        source = "Email",
                        isNewsletter = true
                    )
                    db.subscriptionDao().insert(sub)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
