package com.example.shield

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class ShieldNotificationService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        
        // Skip self
        if (packageName == applicationContext.packageName) return
        
        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        
        val fullText = "$title $text"
        
        if (ScamDictionary.isScam(fullText)) {
            Log.d("ShieldService", "Scam detected: $title")
            cancelNotification(sbn.key)
            showScamWarning(title)
            com.example.widget.WidgetUpdater.updateWidgetState(this, "SCAM", "Blocked scam from $title")
        } else if (OtpDetector.containsOtp(fullText)) {
            Log.d("ShieldService", "OTP detected: $title")
            cancelNotification(sbn.key)
            showSecureOtpNotification(title, text)
            com.example.widget.WidgetUpdater.updateWidgetState(this, "OTP", text)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Handle if needed
    }

    private fun showScamWarning(originalTitle: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createChannel(nm)
        
        val builder = NotificationCompat.Builder(this, "shield_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Threat Blocked \uD83D\uDEE1\uFE0F")
            .setContentText("A potential scam message from $originalTitle was intercepted.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setColor(android.graphics.Color.RED)
            .setAutoCancel(true)
            
        nm.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    private fun showSecureOtpNotification(title: String, originalText: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createChannel(nm)
        
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // We can pass the securely hidden text here to show it in the app
            putExtra("SECURE_OTP_TEXT", originalText)
            putExtra("SECURE_OTP_TITLE", title)
        }
        val pendingIntent = PendingIntent.getActivity(this, System.currentTimeMillis().toInt(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val builder = NotificationCompat.Builder(this, "shield_channel")
            .setSmallIcon(android.R.drawable.ic_secure)
            .setContentTitle("Secure OTP ($title)")
            .setContentText("Tap to authenticate and view securely.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            
        nm.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    private fun createChannel(nm: NotificationManager) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "shield_channel",
                "Shield Security",
                NotificationManager.IMPORTANCE_HIGH
            )
            nm.createNotificationChannel(channel)
        }
    }
}
