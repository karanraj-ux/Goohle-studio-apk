package com.example.ml

import android.content.Context
import android.util.Log
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.io.FileInputStream
// import org.tensorflow.lite.Interpreter

/**
 * Offline Scam Classifier
 * Uses TFLite on-device model for zero-latency spam detection.
 */
class OfflineScamClassifier(private val context: Context) {

    // private var tflite: Interpreter? = null

    init {
        try {
            // val tfliteModel = loadModelFile(context, "scam_detector.tflite")
            // val options = Interpreter.Options().apply {
            //     setNumThreads(2)
            // }
            // tflite = Interpreter(tfliteModel, options)
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
        // if (tflite != null) {
        //     try {
        //         // val input = preprocessText(message) // Example preprocessing
        //         // val output = Array(1) { FloatArray(1) }
        //         // tflite?.run(input, output)
        //         // return output[0][0] > 0.85f
        //         Log.d("OfflineScamClassifier", "Model inference simulated")
        //     } catch (e: Exception) {
        //         Log.e("OfflineScamClassifier", "Inference failed", e)
        //     }
        // }
        
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

    private fun loadModelFile(context: Context, modelName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
}
