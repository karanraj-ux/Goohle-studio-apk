package com.example.shield

import android.content.Context
import android.telephony.SmsManager
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SmsWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val targetNumber = inputData.getString("targetNumber") ?: return@withContext Result.failure()
        val message = inputData.getString("message") ?: return@withContext Result.failure()

        try {
            var sm: SmsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                applicationContext.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            
            val settingsRepo = (applicationContext as com.example.ShieldApplication).container.settingsRepository
            val selectedSimId = settingsRepo.getStringSync(com.example.data.repository.SettingsRepository.SELECTED_SIM_ID, "")
            if (selectedSimId.isNotBlank()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                    val subManager = applicationContext.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as android.telephony.SubscriptionManager
                    if (androidx.core.app.ActivityCompat.checkSelfPermission(applicationContext, android.Manifest.permission.READ_PHONE_STATE) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        val activeSubs = subManager.activeSubscriptionInfoList
                        val subInfo = activeSubs?.find { it.iccId == selectedSimId || it.subscriptionId.toString() == selectedSimId }
                        if (subInfo != null) {
                            sm = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                applicationContext.getSystemService(SmsManager::class.java).createForSubscriptionId(subInfo.subscriptionId)
                            } else {
                                @Suppress("DEPRECATION")
                                SmsManager.getSmsManagerForSubscriptionId(subInfo.subscriptionId)
                            }
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
            Log.d("SmsWorker", "Successfully sent SMS to $targetNumber")
            Result.success()
        } catch (e: Exception) {
            Log.e("SmsWorker", "Failed to forward SMS", e)
            Result.retry()
        }
    }
}
