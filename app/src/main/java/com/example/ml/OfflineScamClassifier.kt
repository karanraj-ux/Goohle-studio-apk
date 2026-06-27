package com.example.ml

import android.content.Context
import android.util.Log
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.io.FileInputStream
import org.tensorflow.lite.Interpreter

/**
 * Offline Scam Classifier
 * Uses TFLite on-device model for zero-latency spam detection.
 */
class OfflineScamClassifier(private val context: Context) {

    private var tflite: Interpreter? = null

    init {
        try {
            val tfliteModel = loadModelFile(context, "scam_detector.tflite")
            val options = Interpreter.Options().apply {
                setNumThreads(2)
            }
            tflite = Interpreter(tfliteModel, options)
            Log.d("OfflineScamClassifier", "TFLite model initialized successfully.")
        } catch (e: Exception) {
            Log.e("OfflineScamClassifier", "Failed to initialize TFLite. Falling back to heuristic mode.", e)
        }
    }

    /**
     * Classifies a message locally on the device using ML.
     * Returns true if it's highly likely to be a scam.
     */
    fun isScam(message: String): Boolean {
        if (tflite != null) {
            try {
                val input = preprocessText(message)
                val output = Array(1) { FloatArray(1) }
                tflite?.run(input, output)
                Log.d("OfflineScamClassifier", "Model inference completed. Score: ${output[0][0]}")
                return output[0][0] > 0.85f
            } catch (e: Exception) {
                Log.e("OfflineScamClassifier", "Inference failed", e)
            }
        }
        
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

    /**
     * Helper to match the Python model's preprocessing.
     * This tokenizes the text into a fixed-length float array.
     */
    private fun preprocessText(text: String): Array<FloatArray> {
        // Adjust the max length according to your trained Python model's sequence length (e.g., 256)
        val maxLength = 256
        val floatArray = FloatArray(maxLength)
        
        val normalized = text.lowercase().replace(Regex("[^a-z0-9 ]"), " ")
        val tokens = normalized.split(Regex("\\s+")).filter { it.isNotEmpty() }
        
        // Basic example: character/ascii based or simple hashing tokenization
        // Update this to use your actual wordpiece / BPE vocabulary file if required.
        for (i in 0 until minOf(tokens.size, maxLength)) {
            // Dummy token logic (replace with your tokenizer map)
            floatArray[i] = tokens[i].hashCode().toFloat() % 10000f
        }
        
        return arrayOf(floatArray)
    }

    private fun loadModelFile(context: Context, modelName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
}
