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
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECEIVE_SMS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return
        }
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val pendingResult = goAsync()
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            
            (context.applicationContext as com.example.ShieldApplication).applicationScope.launch(Dispatchers.IO) {
                try {
                    val sender = messages.firstOrNull()?.displayOriginatingAddress ?: ""
                    val body = messages.joinToString("") { it.displayMessageBody ?: "" }
                    
                    val subscription = intent.getIntExtra("subscription", -1)
                    val slot = intent.getIntExtra("slot", -1)
                    val phone = intent.getIntExtra("phone", -1)
                    // The slot index is typically 0 for SIM 1, 1 for SIM 2
                    val slotIndex = if (slot != -1) slot else if (phone != -1) phone else subscription
                    
                    if (sender.isNotEmpty() && body.isNotEmpty()) {
                        SmsProcessor.processReceivedMessage(context, sender, body, slotIndex)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}

