package com.example.calls

import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.telecom.TelecomManager
import android.util.Log
import com.example.ShieldApplication
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.runBlocking

class TruecallerNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        // Check if Smart Spam Reader is enabled
        val settingsRepo = (applicationContext as ShieldApplication).container.settingsRepository
        val smartSpamEnabled = runBlocking { settingsRepo.getBooleanSync(SettingsRepository.SMART_SPAM_READER, false) }
        if (!smartSpamEnabled) return

        if (sbn.packageName?.contains("truecaller", ignoreCase = true) == true) {
            val notification = sbn.notification
            val extras = notification.extras
            val title = extras.getString(android.app.Notification.EXTRA_TITLE)?.lowercase() ?: ""
            val text = extras.getString(android.app.Notification.EXTRA_TEXT)?.lowercase() ?: ""
            
            Log.d("TruecallerNL", "Truecaller notification posted: Title=$title, Text=$text")

            val isSpam = title.contains("spam") || text.contains("spam") || title.contains("spammer") || text.contains("spammer")
            
            if (isSpam) {
                Log.d("TruecallerNL", "Spam signature detected via Notification Listener! Firing endCall().")
                rejectCall(settingsRepo)
            }
        }
    }

    private fun rejectCall(settingsRepo: SettingsRepository) {
        try {
            val telecomManager = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            if (androidx.core.app.ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ANSWER_PHONE_CALLS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                @Suppress("DEPRECATION") telecomManager.endCall()
                Log.d("TruecallerNL", "Call successfully rejected via TelecomManager.")
                
                runBlocking { 
                    settingsRepo.incrementSpamBlockedCount() 
                    try {
                        val appDb = (applicationContext as ShieldApplication).container.database
                        appDb.smsLogDao().insert(com.example.data.SmsLogEntity(
                            timestamp = System.currentTimeMillis(),
                            sender = "Unknown Truecaller Caller",
                            message = "Spam Call Blocked (Notification)",
                            targetNumber = "",
                            status = "SPAM_BLOCKED"
                        ))
                    } catch (e: Exception) {
                        Log.e("TruecallerNL", "Failed to log spam block", e)
                    }
                }
            } else {
                Log.e("TruecallerNL", "Missing ANSWER_PHONE_CALLS permission.")
            }
        } catch (e: Exception) {
            Log.e("TruecallerNL", "Failed to reject call", e)
        }
    }
}
