package com.example.shield

object ScamDictionary {
    private val scamKeywords = listOf(
        "electricity.*disconnected",
        "kyc.*suspended",
        "account.*blocked.*click",
        "dear.*customer.*kyc",
        "win.*lottery",
        "prize.*money",
        "urgent.*claim",
        "pan.*card.*update.*link"
    ).map { it.toRegex(RegexOption.IGNORE_CASE) }

    fun isScam(message: String): Boolean {
        return scamKeywords.any { it.containsMatchIn(message) }
    }
}
