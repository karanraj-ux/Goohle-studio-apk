package com.example.shield

import android.content.Context

object MerchantDetector {
    private val DEFAULT_MERCHANT_KEYWORDS = listOf(
        "received rs", "credited to your account", "credited with", "payment received",
        "sent you rs", "amount rec", "debited", "credited", "phonepe", "paytm", "gpay", "upi"
    )

    fun getKeywords(context: Context): List<String> {
        val settingsRepo = (context.applicationContext as com.example.ShieldApplication).container.settingsRepository
        val custom = settingsRepo.getStringSync(com.example.data.repository.SettingsRepository.MERCHANT_KEYWORDS, "")
        return if (custom.isNotBlank()) {
            custom.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            DEFAULT_MERCHANT_KEYWORDS
        }
    }

    fun isMerchantOrBankAlert(context: Context, message: String): Boolean {
        val keywords = getKeywords(context).map { it.toRegex(RegexOption.IGNORE_CASE) }
        return keywords.any { it.containsMatchIn(message) }
    }
}
