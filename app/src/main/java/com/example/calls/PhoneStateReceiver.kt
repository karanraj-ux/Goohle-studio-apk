package com.example.calls

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PhoneStateReceiver : BroadcastReceiver() {
    companion object {
        private var lastState = TelephonyManager.EXTRA_STATE_IDLE
        private var isIncoming = false
        private var incomingNumber: String? = null
        
        var lastOutgoingNumber: String? = null
        var lastOffhookTime: Long = 0L
    }

    override fun onReceive(context: Context, intent: Intent) {
        @Suppress("DEPRECATION") if (intent.action == Intent.ACTION_NEW_OUTGOING_CALL) {
            lastOutgoingNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER)
            isIncoming = false
        }
        
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            @Suppress("DEPRECATION") val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
            
            Log.d("PhoneStateReceiver", "State: $state, Number: $number")

            if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                isIncoming = true
                incomingNumber = number
                lastState = state
                
                // VIP Divert & DND Bypass logic
                number?.let {
                    (context.applicationContext as com.example.ShieldApplication).applicationScope.launch(Dispatchers.IO) {
                        CallHandlingManager.handleIncomingCall(context, it)
                    }
                }
            } else if (state == TelephonyManager.EXTRA_STATE_OFFHOOK) {
                if (lastState != TelephonyManager.EXTRA_STATE_RINGING) {
                    isIncoming = false
                    lastOffhookTime = System.currentTimeMillis()
                } else {
                    // Call was answered
                    incomingNumber?.let {
                        CallHandlingManager.handleCallAnswered(context, it)
                    }
                }
                lastState = state
            } else if (state == TelephonyManager.EXTRA_STATE_IDLE) {
                if (lastState == TelephonyManager.EXTRA_STATE_RINGING) {
                    // Missed call detected!
                    val missedNumber = incomingNumber
                    Log.d("PhoneStateReceiver", "Missed call from: $missedNumber")
                    missedNumber?.let {
                        (context.applicationContext as com.example.ShieldApplication).applicationScope.launch(Dispatchers.IO) {
                            CallHandlingManager.handleMissedCall(context, it)
                        }
                    }
                } else if (lastState == TelephonyManager.EXTRA_STATE_OFFHOOK && !isIncoming) {
                    val duration = System.currentTimeMillis() - lastOffhookTime
                    if (duration < 5000) { // under 5 seconds (busy/dropped)
                        lastOutgoingNumber?.let { num ->
                            Log.d("PhoneStateReceiver", "Short outgoing call detected: $duration ms to $num")
                            val retryIntent = Intent("com.example.ACTION_SHOW_RETRY_SHEET")
                            retryIntent.putExtra("number", num)
                            retryIntent.setPackage(context.packageName)
                            context.sendBroadcast(retryIntent)
                        }
                    }
                }
                
                isIncoming = false
                incomingNumber = null
                lastState = state
            }
        }
    }
}
