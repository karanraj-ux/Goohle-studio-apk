package com.example.shield

object OtpDetector {
    private val pattern = Regex("(?i)\\b(OTP|code|pin|verification)\\b.*\\b\\d{4,8}\\b|\\b\\d{4,8}\\b.*\\b(OTP|code|pin|verification)\\b")
    
    fun containsOtp(message: String): Boolean {
        return pattern.containsMatchIn(message)
    }

    fun extractOtp(message: String): String? {
        val match = Regex("\\b\\d{4,8}\\b").find(message)
        return match?.value
    }
}
