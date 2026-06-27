package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val pendingResult = goAsync()
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            
            (context.applicationContext as com.example.ShieldApplication).applicationScope.launch(Dispatchers.IO) {
                try {
                    val sender = messages.firstOrNull()?.displayOriginatingAddress ?: ""
                    val body = messages.joinToString("") { it.displayMessageBody ?: "" }
                    if (sender.isNotEmpty() && body.isNotEmpty()) {
                        SmsProcessor.processReceivedMessage(context, sender, body)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}

