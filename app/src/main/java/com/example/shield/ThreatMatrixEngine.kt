package com.example.shield

import android.content.Context
import android.content.Intent
import android.service.notification.StatusBarNotification
import android.telephony.SmsManager
import android.util.Log

enum class ThreatType {
    SAFE, UPI_FRAUD, PHISHING
}

object ThreatMatrixEngine {
    private var activeCallNumber: String? = null
    private var callStartTime: Long = 0L
    private var isCallActive: Boolean = false

    fun onCallAnswered(context: Context, number: String) {
        val isContact = com.example.calls.CallHandlingManager.isNumberInContacts(context, number)
        if (isContact) {
            Log.d("ThreatMatrixEngine", "Call answered from saved contact. Safe.")
            return
        }
        
        Log.d("ThreatMatrixEngine", "Unsaved call answered. Monitoring for threats.")
        activeCallNumber = number
        callStartTime = System.currentTimeMillis()
        isCallActive = true
    }
    
    fun onWhatsAppCallStarted(context: Context, callerInfo: String) {
        // Scammers use WhatsApp VoIP. Treat as active unsaved call for threat monitoring.
        Log.d("ThreatMatrixEngine", "WhatsApp Call detected from: $callerInfo. Monitoring for threats.")
        activeCallNumber = callerInfo
        callStartTime = System.currentTimeMillis()
        isCallActive = true
    }

    fun onCallEnded() {
        activeCallNumber = null
        callStartTime = 0L
        isCallActive = false
        Log.d("ThreatMatrixEngine", "Call ended. Threat monitoring paused.")
    }

    fun evaluateMessageThreat(body: String): ThreatType {
        val lowerBody = body.lowercase()
        
        // 1. Phishing Matrix
        val hasUrl = listOf("http", "bit.ly", "ngrok", "wa.me", ".com/", ".in/").any { lowerBody.contains(it) }
        val urgencyKeywords = listOf("kyc", "blocked", "suspended", "electricity", "pan card", "claim", "disconnect", "update now", "prize", "winner", "reward", "urgent")
        val hasUrgency = urgencyKeywords.any { lowerBody.contains(it) }
        
        if (hasUrl && hasUrgency) return ThreatType.PHISHING

        // 2. Reverse UPI Fraud Matrix
        val hasMoneyKeywords = listOf("receive", "claim", "cashback", "reward", "refund", "won")
        val hasUpiKeywords = listOf("upi pin", "enter pin", "upi id")
        
        if (hasMoneyKeywords.any { lowerBody.contains(it) } && hasUpiKeywords.any { lowerBody.contains(it) }) {
            return ThreatType.UPI_FRAUD
        }

        return ThreatType.SAFE
    }

    fun onSmsReceived(context: Context, sender: String, body: String): Boolean {
        // 1. Check for standalone Phishing/UPI threats (Ghost Wipe scenario)
        val standaloneThreat = evaluateMessageThreat(body)
        if (standaloneThreat != ThreatType.SAFE) {
            Log.w("ThreatMatrixEngine", "High Risk $standaloneThreat detected in SMS from $sender!")
            handleAutomatedDefense(context, sender, standaloneThreat)
            return true // Indicate it should be Ghost Wiped / swallowed
        }

        // 2. Check for contextual threats during an active call
        if (!isCallActive || activeCallNumber == null) return false

        val callDurationMillis = System.currentTimeMillis() - callStartTime
        val callDurationMinutes = callDurationMillis / (1000 * 60)

        var threatScore = 20
        if (callDurationMinutes >= 4) {
            threatScore += 15 
        }

        val isBankHeader = sender.matches(Regex("[a-zA-Z]{2}-[a-zA-Z0-9]{6}")) || sender.any { it.isLetter() }
        val financialKeywords = listOf("otp", "upi", "debit", "credited", "kyc", "suspended", "blocked")
        
        val containsFinancialKeyword = financialKeywords.any { body.lowercase().contains(it) }

        if (containsFinancialKeyword && isBankHeader) {
            threatScore += 50
        } else if (containsFinancialKeyword) {
            threatScore += 30
        }

        Log.d("ThreatMatrixEngine", "Calculated Threat Score: $threatScore")

        if (threatScore >= 85) {
            triggerIntervention(context)
        }
        
        return false
    }
    
    fun checkNotificationForThreat(context: Context, title: String, text: String, packageName: String): Boolean {
        val fullText = "$title $text"
        val threat = evaluateMessageThreat(fullText)
        
        if (threat != ThreatType.SAFE) {
            Log.w("ThreatMatrixEngine", "High Risk $threat detected in Notification from $packageName!")
            val sender = title.takeIf { it.isNotBlank() } ?: packageName
            handleAutomatedDefense(context, sender, threat)
            return true // Should Ghost Wipe
        }
        return false
    }

    private fun handleAutomatedDefense(context: Context, sender: String, threatType: ThreatType) {
        // 1. Alert the Guardian silently
        GuardianProtocol.alertThreatBlocked(context, threatType.name, sender)
        
        // 2. Auto-reply to the scammer (if it's a standard phone number)
        val isPhoneNumber = sender.replace(Regex("[^0-9+]"), "").length >= 10
        if (isPhoneNumber) {
            val autoReply = "⚠️ This device is protected by Shield Security. Your $threatType attempt has been blocked and flagged."
            try {
                val sm: SmsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }
                sm.sendTextMessage(sender, null, autoReply, null, null)
                Log.d("ThreatMatrixEngine", "Sent Automated Defense Reply to $sender")
            } catch (e: Exception) {
                Log.e("ThreatMatrixEngine", "Failed to send auto-reply", e)
            }
        }
    }

    private fun triggerIntervention(context: Context) {
        Log.w("ThreatMatrixEngine", "CRITICAL THREAT DETECTED. TRIGGERING INTERVENTION.")
        val intent = Intent(context, ShieldInterventionActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("THREAT_NUMBER", activeCallNumber)
        }
        context.startActivity(intent)

        GuardianProtocol.alertGuardian(context, activeCallNumber ?: "Unknown", "Received financial SMS during long call.")
    }
}
