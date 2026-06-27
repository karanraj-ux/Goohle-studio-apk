package com.example.shield

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.telephony.TelephonyManager
import android.telecom.TelecomManager
import android.telecom.PhoneAccountHandle
import androidx.core.app.NotificationCompat
import java.util.concurrent.ConcurrentHashMap

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ShieldCoreService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var emergencyReceiver: BroadcastReceiver? = null
    private var lastPhoneState: String? = null
    
    // Default constants, these are now configurable via prefs
    private val DEFAULT_EMERGENCY_TIMEFRAME_MS = 5 * 60 * 1000L
    private val DEFAULT_EMERGENCY_THRESHOLD = 2

    override fun onCreate() {
        super.onCreate()
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            startForeground(1, createNotification(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, createNotification())
        }
        registerEmergencyCallReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP_SERVICE") {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        unregisterEmergencyCallReceiver()
    }

    private fun registerEmergencyCallReceiver() {
        if (emergencyReceiver == null) {
            emergencyReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val settingsRepo = (context.applicationContext as com.example.ShieldApplication).container.settingsRepository
                    if (settingsRepo.getBooleanSync(com.example.data.repository.SettingsRepository.MASTER_KILL_SWITCH, false)) return
                    
                    if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
                        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                        
                        if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                            if (!incomingNumber.isNullOrEmpty()) {
                                handleIncomingRinging(context, incomingNumber, lastPhoneState)
                            }
                        } else if (state == TelephonyManager.EXTRA_STATE_IDLE && lastPhoneState == TelephonyManager.EXTRA_STATE_RINGING) {
                            if (!incomingNumber.isNullOrEmpty()) {
                                handleMissedCall(context, incomingNumber)
                            }
                        }
                        lastPhoneState = state
                    }
                }
            }
            val filter = IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
            registerReceiver(emergencyReceiver, filter)
        }
    }

    private fun unregisterEmergencyCallReceiver() {
        emergencyReceiver?.let {
            unregisterReceiver(it)
            emergencyReceiver = null
        }
    }

    private fun handleMissedCall(context: Context, number: String) {
        serviceScope.launch(Dispatchers.IO) {
            val settingsRepo = (context.applicationContext as com.example.ShieldApplication).container.settingsRepository
            val webhookUrl = settingsRepo.getStringSync(com.example.data.repository.SettingsRepository.WEBHOOK_URL, "")
            if (!webhookUrl.isNullOrBlank()) {
                 com.example.shield.ForwardingManager.forwardMessage(
                     context, 
                     "Missed Call", 
                     "Missed call from $number", 
                     "MISSED_CALL", 
                     webhookUrl, 
                     null
                 )
            }
        }
    }

    private fun handleIncomingRinging(context: Context, number: String, prevPhoneState: String?) {
        serviceScope.launch(Dispatchers.IO) {
            val settingsRepo = (context.applicationContext as com.example.ShieldApplication).container.settingsRepository
            
            val isAutoForwardEnabled = settingsRepo.getBooleanSync(com.example.data.repository.SettingsRepository.AUTO_FORWARD_CALLS, false)
            val forwardTarget = settingsRepo.getStringSync(com.example.data.repository.SettingsRepository.CALL_FORWARD_TARGET, "")?.trim() ?: ""
            val vipCallersStr = settingsRepo.getStringSync(com.example.data.repository.SettingsRepository.VIP_CALLERS, "") ?: ""
            val vipList = vipCallersStr.split(",").map { it.trim().removePrefix("+") }.filter { it.isNotEmpty() }
            val normalizedNumber = number.trim().removePrefix("+")
            
            val isVip = vipList.any { it.isNotEmpty() && (normalizedNumber.endsWith(it) || it.endsWith(normalizedNumber)) }

            if (!isVip && isAutoForwardEnabled && forwardTarget.isNotEmpty()) {
                // Not a VIP and auto-forward is enabled -> Mute and Forward
                try {
                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                    audioManager.setStreamVolume(android.media.AudioManager.STREAM_RING, 0, 0)
                    android.util.Log.d("ShieldCoreService", "Muted non-VIP incoming call.")
                } catch (e: Exception) {
                    android.util.Log.e("ShieldCoreService", "Could not mute call: ${e.message}")
                }
                
                // Activate Call Forwarding (MMI)
                activateCallForwarding(context, forwardTarget)
                
                if (settingsRepo.getBooleanSync(com.example.data.repository.SettingsRepository.ALERT_FORWARD_TARGET, false)) {
                     try {
                         val msg = "I have activated call forwarding to your number. Expect calls from non-VIPs to reach you."
                         sendSmsWithSim(context, forwardTarget, msg)
                         android.util.Log.d("ShieldCoreService", "Sent SMS alert to forward target: $forwardTarget")
                     } catch (e: Exception) {
                         android.util.Log.e("ShieldCoreService", "Failed to send forward alert SMS: ${e.message}")
                     }
                }
                
                // Keep Call Forwarding ON for duration, then deactivate
                val durationMinutes = settingsRepo.getIntSync(com.example.data.repository.SettingsRepository.AUTO_FORWARD_DURATION, 5).toLong()
                serviceScope.launch(Dispatchers.IO) {
                    kotlinx.coroutines.delay(durationMinutes * 60 * 1000L)
                    deactivateCallForwarding(context)
                }
            }

            if (settingsRepo.getBooleanSync(com.example.data.repository.SettingsRepository.DETECT_BUSY, false) && prevPhoneState == TelephonyManager.EXTRA_STATE_OFFHOOK) {
                // We are busy in another call!
                val replyMsg = settingsRepo.getStringSync(com.example.data.repository.SettingsRepository.BUSY_REPLY_MSG, "I am currently in another call. I will call you back later.") ?: "I am busy right now."
                try {
                    sendSmsWithSim(context, number, replyMsg)
                    android.util.Log.d("ShieldCoreService", "Sent Busy Auto-Reply to $number")
                } catch (e: Exception) {
                    android.util.Log.e("ShieldCoreService", "Failed to send busy reply: ${e.message}")
                }
            }

            val now = System.currentTimeMillis()
            val prefs = com.example.utils.SecurityUtils.getEncryptedPrefs(context, "emergency_calls_tracker")
            
            val allEntries = prefs.all
            val editor = prefs.edit()
            var currentHistory = mutableListOf<Long>()
            
            val timeframeMinutes = settingsRepo.getIntSync(com.example.data.repository.SettingsRepository.DND_TIMEFRAME_MINUTES, 5)
            val timeframeMs = timeframeMinutes * 60 * 1000L
            val emergencyThreshold = settingsRepo.getIntSync(com.example.data.repository.SettingsRepository.DND_THRESHOLD_CALLS, 2)
            
            allEntries.forEach { (key, value) ->
                if (value is String) {
                    val timestamps = value.split(",").mapNotNull { it.toLongOrNull() }.filter { it >= now - timeframeMs }.toMutableList()
                    if (key == number) {
                        currentHistory = timestamps
                    } else {
                        if (timestamps.isEmpty()) {
                            editor.remove(key)
                        } else {
                            editor.putString(key, timestamps.joinToString(","))
                        }
                    }
                }
            }
            
            currentHistory.add(now)
            
            // Only trigger alarm threshold for VIPs or if no auto-forwarding is enabled.
            // If it's a blocked/forwarded non-VIP call, we probably don't want to blast an alarm, unless the user explicitly wants emergency override for everyone.
            // The prompt implies multiple calls from VIP drops DND.
            if ((isVip || !isAutoForwardEnabled) && currentHistory.size >= emergencyThreshold) {
                // FIRE DND BYPASS / ALARM!
                triggerEmergencyBypass(context, number)
                editor.remove(number)
            } else {
                editor.putString(number, currentHistory.joinToString(","))
            }
            editor.apply()
        }
    }

    private fun activateCallForwarding(context: Context, targetNumber: String) {
        try {
            val encodedHash = android.net.Uri.encode("#")
            val mmiCode = "**21*$targetNumber$encodedHash"
            val rawMmi = "**21*$targetNumber#"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                var tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                
                val settingsRepo = (context.applicationContext as com.example.ShieldApplication).container.settingsRepository
                val selectedSimId = settingsRepo.getStringSync(com.example.data.repository.SettingsRepository.SELECTED_SIM_ID, "")
                if (selectedSimId.isNotBlank() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val subManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as android.telephony.SubscriptionManager
                    if (androidx.core.app.ActivityCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        val activeSubs = subManager.activeSubscriptionInfoList
                        val subInfo = activeSubs?.find { it.iccId == selectedSimId || it.subscriptionId.toString() == selectedSimId }
                        if (subInfo != null) {
                            tm = tm.createForSubscriptionId(subInfo.subscriptionId)
                        }
                    }
                }

                val handler = android.os.Handler(android.os.Looper.getMainLooper())
                if (androidx.core.app.ActivityCompat.checkSelfPermission(context, android.Manifest.permission.CALL_PHONE) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    tm.sendUssdRequest(rawMmi, object : TelephonyManager.UssdResponseCallback() {
                        override fun onReceiveUssdResponse(telephonyManager: TelephonyManager?, request: String?, response: CharSequence?) {
                            android.util.Log.d("ShieldCoreService", "USSD Response: $response")
                            serviceScope.launch {
                                SystemNotificationEventBus.emitEvent(
                                    SystemEvent.IncomingCallSuspicious(
                                        phoneNumber = "System",
                                        reason = "Forwarding enabled: $response"
                                    )
                                )
                            }
                        }

                        override fun onReceiveUssdResponseFailed(telephonyManager: TelephonyManager?, request: String?, failureCode: Int) {
                            android.util.Log.e("ShieldCoreService", "USSD Failed with code: $failureCode")
                            serviceScope.launch {
                                SystemNotificationEventBus.emitEvent(
                                    SystemEvent.IncomingCallSuspicious(
                                        phoneNumber = "System",
                                        reason = "Forwarding failed. Relying on muted ringer and SMS auto-reply."
                                    )
                                )
                            }
                        }
                    }, handler)
                }
            } else {
                val intent = Intent(Intent.ACTION_CALL)
                intent.data = android.net.Uri.parse("tel:$mmiCode")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                
                attachSelectedSimHandle(context, intent)
                
                context.startActivity(intent)
                android.util.Log.d("ShieldCoreService", "Activated Call Forwarding to $targetNumber")
                
                serviceScope.launch {
                    SystemNotificationEventBus.emitEvent(
                        SystemEvent.IncomingCallSuspicious(
                            phoneNumber = "System",
                            reason = "Auto-enabled call forwarding to $targetNumber for 5 minutes."
                        )
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ShieldCoreService", "Failed to activate call forwarding: ${e.message}")
        }
    }

    private fun deactivateCallForwarding(context: Context) {
        try {
            val encodedHash = android.net.Uri.encode("#")
            val mmiCode = "##21$encodedHash"
            val intent = Intent(Intent.ACTION_CALL)
            intent.data = android.net.Uri.parse("tel:$mmiCode")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            attachSelectedSimHandle(context, intent)
            
            context.startActivity(intent)
            android.util.Log.d("ShieldCoreService", "Deactivated Call Forwarding")
            
            serviceScope.launch {
                SystemNotificationEventBus.emitEvent(
                    SystemEvent.IncomingCallSuspicious(
                        phoneNumber = "System",
                        reason = "Auto-disabled call forwarding."
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("ShieldCoreService", "Failed to deactivate call forwarding: ${e.message}")
        }
    }

    private fun attachSelectedSimHandle(context: Context, intent: Intent) {
        try {
            val settingsRepo = (context.applicationContext as com.example.ShieldApplication).container.settingsRepository
            val selectedSimId = settingsRepo.getStringSync(com.example.data.repository.SettingsRepository.SELECTED_SIM_ID, "")
            if (selectedSimId.isNotBlank()) {
                val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                if (androidx.core.app.ActivityCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    val handles = telecomManager.callCapablePhoneAccounts
                    val selectedHandle = handles.find { it.id == selectedSimId }
                    if (selectedHandle != null) {
                        intent.putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, selectedHandle)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ShieldCoreService", "Failed to attach SIM handle: ${e.message}")
        }
    }

    private fun sendSmsWithSim(context: Context, targetNumber: String, message: String) {
        var sm: android.telephony.SmsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            context.getSystemService(android.telephony.SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            android.telephony.SmsManager.getDefault()
        }
        val settingsRepo = (context.applicationContext as com.example.ShieldApplication).container.settingsRepository
        val selectedSimId = settingsRepo.getStringSync(com.example.data.repository.SettingsRepository.SELECTED_SIM_ID, "")
        if (selectedSimId.isNotBlank() && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
            val subManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as android.telephony.SubscriptionManager
            if (androidx.core.app.ActivityCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                val activeSubs = subManager.activeSubscriptionInfoList
                val subInfo = activeSubs?.find { it.iccId == selectedSimId || it.subscriptionId.toString() == selectedSimId }
                if (subInfo != null) {
                    sm = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        context.getSystemService(android.telephony.SmsManager::class.java).createForSubscriptionId(subInfo.subscriptionId)
                    } else {
                        @Suppress("DEPRECATION")
                        android.telephony.SmsManager.getSmsManagerForSubscriptionId(subInfo.subscriptionId)
                    }
                }
            }
        }
        val parts = sm.divideMessage(message)
        if (parts.size > 1) {
            sm.sendMultipartTextMessage(targetNumber, null, parts, null, null)
        } else {
            sm.sendTextMessage(targetNumber, null, message, null, null)
        }
    }

    private fun triggerEmergencyBypass(context: Context, number: String) {
        serviceScope.launch {
            val settingsRepo = (context.applicationContext as com.example.ShieldApplication).container.settingsRepository
            if (settingsRepo.getBooleanSync(com.example.data.repository.SettingsRepository.OVERRIDE_DND, false)) {
                try {
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        if (notificationManager.isNotificationPolicyAccessGranted) {
                            val currentFilter = notificationManager.currentInterruptionFilter
                            if (currentFilter != android.app.NotificationManager.INTERRUPTION_FILTER_ALL) {
                                notificationManager.setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_ALL)
                                android.util.Log.d("ShieldCoreService", "DND successfully overridden.")
                            }
                        } else {
                            android.util.Log.e("ShieldCoreService", "Cannot override DND. Permission not granted.")
                        }
                    }

                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                    val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_RING)
                    audioManager.setStreamVolume(android.media.AudioManager.STREAM_RING, maxVolume, 0)
                    
                    val ringtoneUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE)
                    val ringtone = android.media.RingtoneManager.getRingtone(context, ringtoneUri)
                    ringtone.streamType = android.media.AudioManager.STREAM_RING
                    ringtone.play()
                    
                    // Stop after 5 seconds
                    kotlinx.coroutines.delay(5000)
                    ringtone.stop()
                } catch (e: Exception) {
                    android.util.Log.e("ShieldCoreService", "Failed to bypass DND: ${e.message}")
                }
            }

            SystemNotificationEventBus.emitEvent(
                SystemEvent.IncomingCallSuspicious(
                    phoneNumber = number,
                    reason = "DND BYPASS: This number has called multiple times in ${settingsRepo.getIntSync(com.example.data.repository.SettingsRepository.DND_TIMEFRAME_MINUTES, 5)} minutes! " + if (settingsRepo.getBooleanSync(com.example.data.repository.SettingsRepository.OVERRIDE_DND, false)) "(Alarm sound played)" else ""
                )
            )
        }
    }

    private fun createNotification(): Notification {
        val channelId = "shield_core_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Shield Background Core",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps KJ Shield and Auto Call active"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
        
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("KJ Shield Active")
            .setContentText("Monitoring for emergency consecutive calls.")
            .setSmallIcon(android.R.drawable.ic_dialog_info) 
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
