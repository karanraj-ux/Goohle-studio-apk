package com.example.shield

import android.content.Context
import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.ShieldApplication
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.Q)
class ShieldCallScreeningService : CallScreeningService() {
    
    override fun onScreenCall(callDetails: Call.Details) {
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
            return
        }

        val phoneNumber = callDetails.handle?.schemeSpecificPart ?: ""
        Log.d("ShieldCallScreening", "Incoming call from: $phoneNumber")

        val app = applicationContext as ShieldApplication
        val repo = app.container.settingsRepository
        
        CoroutineScope(Dispatchers.IO).launch {
            val isAutoForwardEnabled = repo.getBooleanSync(SettingsRepository.AUTO_FORWARD_CALLS, false)
            val vipCallersStr = repo.getStringSync(SettingsRepository.VIP_CALLERS, "") ?: ""
            val vipList = vipCallersStr.split(",").map { it.trim().removePrefix("+") }.filter { it.isNotEmpty() }
            val normalizedNumber = phoneNumber.trim().removePrefix("+")
            
            val isVip = vipList.any { it.isNotEmpty() && (normalizedNumber.endsWith(it) || it.endsWith(normalizedNumber)) }
            
            // Scam detection logic could go here, e.g., checking ScamDictionary
            val isScam = com.example.shield.ScamDictionary.isScam(applicationContext, phoneNumber)
            
            if (isScam) {
                Log.d("ShieldCallScreening", "Scam call detected! Rejecting natively.")
                val response = CallResponse.Builder()
                    .setDisallowCall(true)
                    .setRejectCall(true)
                    .setSkipCallLog(false)
                    .setSkipNotification(true)
                    .build()
                respondToCall(callDetails, response)
                return@launch
            }

            if (!isVip && isAutoForwardEnabled) {
                Log.d("ShieldCallScreening", "Non-VIP call with auto-forward enabled. Allowing it to ring silently and we will handle it in ShieldCoreService.")
                // ShieldCoreService will pick up the RINGING intent and forward it via MMI.
                // We could block it here if we just want to reject, but we want to forward via MMI, 
                // which means we need the call to exist or we just let ShieldCoreService do its job.
            }
            
            // Allow the call by default
            respondToCall(callDetails, CallResponse.Builder().build())
        }
    }
}
