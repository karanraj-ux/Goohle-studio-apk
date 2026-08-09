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
        if (androidx.core.content.ContextCompat.checkSelfPermission(applicationContext, android.Manifest.permission.SEND_SMS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.e("SmsWorker", "Permission denied, silently exiting.")
            return@withContext Result.failure()
        }
        val targetNumber = inputData.getString("targetNumber") ?: return@withContext Result.failure()
        val message = inputData.getString("message") ?: return@withContext Result.failure()

        try {
            val sm: SmsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                applicationContext.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
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
