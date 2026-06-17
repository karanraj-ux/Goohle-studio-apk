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

    companion object {
        fun simulateNotificationProcessing(context: Context, title: String, text: String) {
            val fullText = "$title $text"
            if (com.example.shield.ScamDictionary.isScam(fullText)) {
                android.util.Log.d("ShieldService", "Simulated Scam Intercepted: $title")
                com.example.widget.WidgetUpdater.updateWidgetState(context, "SCAM", "Blocked simulated scam from $title")
            } else if (com.example.shield.OtpDetector.containsOtp(fullText)) {
                android.util.Log.d("ShieldService", "Simulated OTP Intercepted: $title")
                com.example.widget.WidgetUpdater.updateWidgetState(context, "OTP", text)
            } else if (MerchantDetector.isMerchantOrBankAlert(fullText)) {
                android.util.Log.d("ShieldService", "Simulated Merchant/Bank alert detected: $title")
                com.example.widget.WidgetUpdater.updateWidgetState(context, "TXN", text)
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        
        // Skip self
        if (packageName == applicationContext.packageName) return
        
        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        
        val fullText = "$title $text"
        val lowerText = fullText.lowercase()

        // Phase 1: Carrier notification detection
        if (lowerText.contains("is available") || lowerText.contains("now available") || lowerText.contains("back in coverage") || lowerText.contains("missed call")) {
            Log.d("ShieldService", "Carrier Alert detected: $title")
            cancelNotification(sbn.key)
            
            // Extract number or use title
            var extractedNumber = extractNumber(fullText)
            if (extractedNumber.isEmpty()) {
                extractedNumber = extractNumber(title)
            }

            // Phase 4: Handle Missed Call with AI AutoResponder
            if (lowerText.contains("missed call")) {
                AutoResponder.handleMissedCall(this, title, extractedNumber)
            }
            
            val intent = Intent(this, com.example.ui.screens.SmartCallAlertActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("CONTACT_NAME", title)
                putExtra("PHONE_NUMBER", extractedNumber)
                putExtra("MESSAGE", text)
            }
            startActivity(intent)
            return
        }
        
        if (ScamDictionary.isScam(fullText)) {
            Log.d("ShieldService", "Scam detected: $title")
            cancelNotification(sbn.key)
            val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val silentSwallow = prefs.getBoolean("silent_swallow", false)
            if (!silentSwallow) {
                showScamWarning(title)
            }
            com.example.widget.WidgetUpdater.updateWidgetState(this, "SCAM", "Blocked scam from $title")
        } else if (OtpDetector.containsOtp(fullText)) {
            Log.d("ShieldService", "OTP detected: $title")
            cancelNotification(sbn.key)
            showSecureOtpNotification(title, text)
            com.example.widget.WidgetUpdater.updateWidgetState(this, "OTP", text)
            
            // Forward OTP
            val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val webhookUrl = prefs.getString("webhook_url", "")
            val forwardPhone = prefs.getString("forward_phone", "")
            if (!webhookUrl.isNullOrBlank() || !forwardPhone.isNullOrBlank()) {
                ForwardingManager.forwardMessage(this, title, text, "OTP", webhookUrl, forwardPhone)
            }
        } else if (MerchantDetector.isMerchantOrBankAlert(fullText)) {
            Log.d("ShieldService", "Merchant/Bank alert detected: $title")
            // Not hiding this by default unless user wants, but we will forward it
            val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val webhookUrl = prefs.getString("webhook_url", "")
            val forwardPhone = prefs.getString("forward_phone", "")
            if (!webhookUrl.isNullOrBlank() || !forwardPhone.isNullOrBlank()) {
                ForwardingManager.forwardMessage(this, title, text, "TRANSACTION", webhookUrl, forwardPhone)
                com.example.widget.WidgetUpdater.updateWidgetState(this, "TXN", text)
            }
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

    private fun extractNumber(text: String): String {
        val pattern = java.util.regex.Pattern.compile(".*?(\\+?\\d{10,13}).*")
        val matcher = pattern.matcher(text)
        if (matcher.matches()) {
            return matcher.group(1) ?: ""
        }
        return ""
    }
}
