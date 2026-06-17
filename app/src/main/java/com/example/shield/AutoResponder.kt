package com.example.shield

import android.content.Context
import android.telephony.SmsManager
import android.util.Log

object AutoResponder {

    fun handleMissedCall(context: Context, contactName: String, phoneNumber: String) {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val autoRespondMissedCall = prefs.getBoolean("auto_respond_missed_call", false)
        
        if (autoRespondMissedCall && phoneNumber.isNotBlank()) {
            val replyMessage = generateAiReply(context, "MISSED_CALL", contactName, "")
            sendSms(context, phoneNumber, replyMessage)
        }
    }

    fun handleIncomingSms(context: Context, sender: String, message: String) {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val autoRespondSms = prefs.getBoolean("auto_respond_sms", false)
        
        if (autoRespondSms && sender.isNotBlank()) {
            // Assume it's from unknown if logic dictates, for now we respond to all if toggled
            val replyMessage = generateAiReply(context, "SMS", sender, message)
            sendSms(context, sender, replyMessage)
        }
    }

    private fun generateAiReply(context: Context, eventType: String, sender: String, message: String): String {
        val prefs = context.getSharedPreferences("kj_ai_prefs", Context.MODE_PRIVATE)
        val engineType = prefs.getString("engine_type", "LOCAL") ?: "LOCAL"
        val systemPrompt = prefs.getString("system_prompt", "Summarize this long email notification into 3 bullet points.") ?: ""
        
        // Use the system configuration to process the incoming text
        return when (engineType) {
            "SLM" -> {
                Log.d("AutoResponder", "Using experimental SLM Bridge with prompt: $systemPrompt")
                "[Local SLM] Based on '$systemPrompt', processing offline: I am currently unavailable."
            }
            "API" -> {
                val provider = prefs.getString("api_provider", "OpenAI")
                Log.d("AutoResponder", "Using BYOK Cloud API ($provider) with prompt: $systemPrompt")
                "[$provider] I received your message. (Auto-reply via API)"
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
            val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            val parts = smsManager.divideMessage(content)
            smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
            Log.d("AutoResponder", "Auto-responded via SMS to $phoneNumber")
        } catch (e: Exception) {
            Log.e("AutoResponder", "Failed to auto-respond via SMS", e)
        }
    }
}
