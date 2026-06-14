package com.example.declutter

import android.content.Context
import android.provider.Telephony
import com.example.data.AppDatabase
import com.example.data.SubscriptionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SmsFinancialScanner {
    suspend fun scanSmsForSubscriptions(context: Context) = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        val resolver = context.contentResolver
        
        try {
            val cursor = resolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.ADDRESS),
                null,
                null,
                Telephony.Sms.DEFAULT_SORT_ORDER + " LIMIT 1000"
            )

            // Look for common subscription/auto-debit markers
            val autoDebitRegex = Regex("(?i)(auto[- ]debit|subscription|recurring payment|debited for)[^\\d]*([rs\$€£]?\\s?\\d+[.,]?\\d*)")
            
            db.subscriptionDao().clearAll() // For fresh scan

            cursor?.use { c ->
                val bodyIndex = c.getColumnIndex(Telephony.Sms.BODY)
                val dateIndex = c.getColumnIndex(Telephony.Sms.DATE)
                val addressIndex = c.getColumnIndex(Telephony.Sms.ADDRESS)
                
                while (c.moveToNext()) {
                    val body = c.getString(bodyIndex) ?: continue
                    val match = autoDebitRegex.find(body)
                    
                    if (match != null) {
                        val amount = match.groupValues.getOrNull(2)?.trim() ?: "Unknown Amount"
                        var sender = c.getString(addressIndex) ?: "Unknown"
                        val date = c.getLong(dateIndex)
                        
                        // Clean up sender (e.g. AD-HDFCBK -> HDFC Bank)
                        sender = cleanSenderName(sender)
                        
                        val sub = SubscriptionEntity(
                            name = sender,
                            amount = amount,
                            dateDetected = date,
                            source = "SMS",
                            isNewsletter = false
                        )
                        db.subscriptionDao().insert(sub)
                    }
                }
            }
        } catch (e: SecurityException) {
            // Permission likely not granted
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun cleanSenderName(sender: String): String {
        return sender.replace(Regex("^[A-Za-z]{2}-"), "")
    }
}
