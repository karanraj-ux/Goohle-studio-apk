package com.example

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.calls.CallHandlingManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ShieldNotificationListener : NotificationListenerService() {
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("ShieldNotification", "Notification listener connected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn?.let {
            val packageName = it.packageName
            val notification = it.notification
            val extras = notification.extras
            val title = extras.getString(android.app.Notification.EXTRA_TITLE) ?: ""
            val text = extras.getString(android.app.Notification.EXTRA_TEXT) ?: ""
            
            Log.d("ShieldNotification", "Notification from $packageName: $title - $text")

            // Check if it's Truecaller identifying a spam call
            if (packageName == "com.truecaller" || packageName == "com.truecaller.debug") {
                val isSpam = title.contains("Spam", ignoreCase = true) || 
                             text.contains("Spam", ignoreCase = true) ||
                             title.contains("Fraud", ignoreCase = true) ||
                             text.contains("Fraud", ignoreCase = true)
                
                if (isSpam) {
                    Log.d("ShieldNotification", "Truecaller identified incoming spam call. Attempting to reject.")
                    scope.launch {
                        CallHandlingManager.rejectCall(this@ShieldNotificationListener)
                    }
                }
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Not used
    }
}
