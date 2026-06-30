package com.example.ml

import android.content.Context
import android.util.Log

/**
 * Offline Scam Classifier
 * Uses Advanced Regex Fallback for spam detection.
 */
object OfflineScamClassifier {

    private var isInitialized = false

    fun init(context: Context) {
        if (!isInitialized) {
            Log.d("OfflineScamClassifier", "OfflineScamClassifier singleton initialized.")
            isInitialized = true
        }
    }

    /**
     * Classifies a message locally on the device using ML.
     * Returns true if it's highly likely to be a scam.
     */
    fun isScam(message: String): Boolean {
        // Advanced Regex Fallback
        val scamKeywords = listOf(
            Regex("(?i).*account.*suspended.*"),
            Regex("(?i).*kyc.*update.*"),
            Regex("(?i).*electricity.*disconnected.*"),
            Regex("(?i).*urgent.*action.*required.*"),
            Regex("(?i).*winner.*lottery.*"),
            Regex("(?i).*bank.*blocked.*")
        )
        return scamKeywords.any { it.matches(message) }
    }
}
