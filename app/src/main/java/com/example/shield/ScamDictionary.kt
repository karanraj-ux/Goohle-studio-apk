package com.example.shield

object ScamDictionary {
    private val DEFAULT_SCAM_KEYWORDS = listOf(
        "electricity.*disconnected",
        "kyc.*suspended",
        "account.*blocked.*click",
        "dear.*customer.*kyc",
        "win.*lottery",
        "prize.*money",
        "urgent.*claim",
        "pan.*card.*update.*link"
    )

    fun getKeywords(context: android.content.Context): List<String> {
        val settingsRepo = (context.applicationContext as com.example.ShieldApplication).container.settingsRepository
        val custom = settingsRepo.getStringSync(com.example.data.repository.SettingsRepository.SCAM_KEYWORDS, "")
        return if (custom.isNotBlank()) {
            custom.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            DEFAULT_SCAM_KEYWORDS
        }
    }

    fun isScam(context: android.content.Context, message: String): Boolean {
        val keywords = getKeywords(context).map { it.toRegex(RegexOption.IGNORE_CASE) }
        return keywords.any { it.containsMatchIn(message) }
    }
}
