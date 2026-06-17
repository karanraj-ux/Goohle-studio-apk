package com.example.shield

object MerchantDetector {
    private val merchantKeywords = listOf(
        "received rs",
        "credited to your account",
        "credited with",
        "payment received",
        "sent you rs",
        "amount rec",
        "debited",
        "credited",
        "phonepe",
        "paytm",
        "gpay",
        "upi"
    ).map { it.toRegex(RegexOption.IGNORE_CASE) }

    fun isMerchantOrBankAlert(message: String): Boolean {
        return merchantKeywords.any { it.containsMatchIn(message) }
    }
}
