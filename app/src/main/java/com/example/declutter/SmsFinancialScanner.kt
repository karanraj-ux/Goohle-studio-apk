package com.example.declutter

import android.content.Context
import android.provider.Telephony
import com.example.data.AppDatabase
import com.example.data.ExpenseEntity
import com.example.data.SubscriptionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SmsFinancialScanner {
    suspend fun scanSmsForSubscriptions(context: Context) = withContext(Dispatchers.IO) {
        val finRepo = (context.applicationContext as com.example.ShieldApplication).container.financialRepository
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
            
            // Look for valid transactions/expenses (debits only)
            val expenseRegex = Regex("(?i)(debited|spent|paid|payment of|sent)[^\\d]*([rs\$€£]?\\s?\\d+[.,]?\\d*)")

            finRepo.clearSubscriptions()
            finRepo.clearExpenses()

            cursor?.use { c ->
                val bodyIndex = c.getColumnIndex(Telephony.Sms.BODY)
                val dateIndex = c.getColumnIndex(Telephony.Sms.DATE)
                val addressIndex = c.getColumnIndex(Telephony.Sms.ADDRESS)
                
                while (c.moveToNext()) {
                    val body = c.getString(bodyIndex) ?: continue
                    val matchSub = autoDebitRegex.find(body)
                    
                    val date = c.getLong(dateIndex)
                    var sender = c.getString(addressIndex) ?: "Unknown"
                    sender = cleanSenderName(sender)

                    if (matchSub != null) {
                        val amount = matchSub.groupValues.getOrNull(2)?.trim() ?: "Unknown Amount"
                        
                        val sub = SubscriptionEntity(
                            name = sender,
                            amount = amount,
                            dateDetected = date,
                            source = "SMS",
                            isNewsletter = false
                        )
                        // Ignore conflicts (unique index on name) to just keep latest
                        try { finRepo.insertSubscription(sub) } catch (e: Exception) {}
                    }
                    
                    // For standard expenses
                    val matchExp = expenseRegex.find(body)
                    if (matchExp != null && matchSub == null) { // if it's not a subscription
                        val amountStr = matchExp.groupValues.getOrNull(2)?.trim() ?: ""
                        val amountVal = parseAmount(amountStr)
                        if (amountVal > 0) {
                            val exp = ExpenseEntity(
                                merchant = sender,
                                amountStr = amountStr,
                                amountVal = amountVal,
                                dateDetected = date,
                                source = "SMS"
                            )
                            finRepo.insertExpense(exp)
                        }
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
    
    private fun parseAmount(amountStr: String): Double {
        val cleanStr = amountStr.replace(Regex("[^\\d.,]"), "")
        if (cleanStr.isEmpty()) return 0.0

        return try {
            val lastComma = cleanStr.lastIndexOf(',')
            val lastPeriod = cleanStr.lastIndexOf('.')

            val format = if (lastComma > lastPeriod) {
                // European format (e.g., 1.000,50)
                java.text.NumberFormat.getInstance(java.util.Locale.GERMANY)
            } else if (lastPeriod > lastComma) {
                // US/Asian format (e.g., 1,000.50)
                java.text.NumberFormat.getInstance(java.util.Locale.US)
            } else {
                if (lastComma != -1) {
                    // Decide based on decimal position
                    if (cleanStr.length - lastComma == 3) {
                         java.text.NumberFormat.getInstance(java.util.Locale.GERMANY)
                    } else {
                         java.text.NumberFormat.getInstance(java.util.Locale.US)
                    }
                } else {
                    java.text.NumberFormat.getInstance(java.util.Locale.US)
                }
            }
            format.parse(cleanStr)?.toDouble() ?: 0.0
        } catch (e: Exception) {
            0.0
        }
    }
}
