with open("app/src/main/java/com/example/SmsProcessor.kt", "r") as f:
    content = f.read()

old_forward = """    private fun forwardSms(context: Context, targetNumber: String, message: String, index: Int, explicitDelayMs: Long = 0): String {
        return try {
            val data = androidx.work.Data.Builder()
                .putString("targetNumber", targetNumber)
                .putString("message", message)
                .build()
            val constraints = androidx.work.Constraints.Builder()
                .build()
            val request = androidx.work.OneTimeWorkRequestBuilder<com.example.shield.SmsWorker>()
                .setConstraints(constraints)
                .setInputData(data)
                // Added initial delay for throttling bulk sms (staggered)
                .setInitialDelay(if (explicitDelayMs > 0) explicitDelayMs else 2000L * (index + 1), java.util.concurrent.TimeUnit.MILLISECONDS)
                .setBackoffCriteria(
                    androidx.work.BackoffPolicy.EXPONENTIAL,
                    10000L,
                    java.util.concurrent.TimeUnit.MILLISECONDS
                )
                .build()
            androidx.work.WorkManager.getInstance(context).enqueue(request)
            "SUCCESS"
        } catch (e: Exception) {
            android.util.Log.e("SmsProcessor", "Failed to enqueue SMS", e)
            "FAILED"
        }
    }"""

new_forward = """    private fun forwardSms(context: Context, targetNumber: String, message: String, index: Int, explicitDelayMs: Long = 0): String {
        return try {
            if (explicitDelayMs > 0) {
                // For custom rules with delay, use the worker
                val data = androidx.work.Data.Builder()
                    .putString("targetNumber", targetNumber)
                    .putString("message", message)
                    .build()
                val request = androidx.work.OneTimeWorkRequestBuilder<com.example.shield.SmsWorker>()
                    .setInputData(data)
                    .setInitialDelay(explicitDelayMs, java.util.concurrent.TimeUnit.MILLISECONDS)
                    .build()
                androidx.work.WorkManager.getInstance(context).enqueue(request)
                return "SUCCESS"
            }

            // Direct instant forwarding for normal SMS
            val sm: android.telephony.SmsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                context.getSystemService(android.telephony.SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                android.telephony.SmsManager.getDefault()
            }
            val parts = sm.divideMessage(message)
            if (parts.size > 1) {
                sm.sendMultipartTextMessage(targetNumber, null, parts, null, null)
            } else {
                sm.sendTextMessage(targetNumber, null, message, null, null)
            }
            android.util.Log.d("SmsProcessor", "Directly forwarded SMS to $targetNumber")
            "SUCCESS"
        } catch (e: Exception) {
            android.util.Log.e("SmsProcessor", "Failed to forward SMS directly", e)
            "FAILED"
        }
    }"""

content = content.replace(old_forward, new_forward)

with open("app/src/main/java/com/example/SmsProcessor.kt", "w") as f:
    f.write(content)
