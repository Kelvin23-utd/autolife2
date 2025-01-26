package com.google.mediapipe.examples.llminference

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import java.io.Closeable
import com.google.mediapipe.tasks.genai.llminference.LlmInference

class SequentialMotionLocationAnalyzer(private val context: Context) : Closeable {
    companion object {
        private const val TAG = "SequentialAnalyzer"
        private const val MOTION_DURATION = 10000L // 10 seconds
    }

    private var isAnalyzing = false
    private var currentPhase = AnalysisPhase.NONE
    private var motionDetector: MotionDetector? = null
    private var locationAnalyzer: LocationAnalyzer? = null
    private var llmInference: LlmInference? = null

    enum class AnalysisPhase {
        NONE,
        MOTION,
        LOCATION,
        FUSION,
        COMPLETE
    }

    fun startAnalysis(callback: (String, AnalysisPhase) -> Unit) {
        if (isAnalyzing) {
            callback("Analysis already in progress", currentPhase)
            return
        }

        // Start with motion phase
        startMotionPhase(callback)
    }

    private fun startMotionPhase(callback: (String, AnalysisPhase) -> Unit) {
        isAnalyzing = true
        currentPhase = AnalysisPhase.MOTION
        System.gc() // Request garbage collection before starting

        motionDetector = MotionDetector(context)
        motionDetector?.startDetection { motions ->
            callback("Detected motions: ${motions.joinToString(", ")}", AnalysisPhase.MOTION)
        }

        CoroutineScope(Dispatchers.Main).launch {
            delay(MOTION_DURATION)
            finishMotionPhase(callback)
        }
    }

    private fun finishMotionPhase(callback: (String, AnalysisPhase) -> Unit) {
        val motionStorage = MotionStorage(context)
        val motionHistory = motionStorage.getMotionHistory()

        // Clean up motion resources immediately
        motionDetector?.stopDetection()
        motionDetector = null
        System.gc() // Request cleanup

        startLocationPhase(callback)
    }

    private fun startLocationPhase(callback: (String, AnalysisPhase) -> Unit) {
        currentPhase = AnalysisPhase.LOCATION
        callback("Starting location analysis...", AnalysisPhase.LOCATION)

        CoroutineScope(Dispatchers.Main).launch {
            try {
                withContext(Dispatchers.IO) {
                    locationAnalyzer = LocationAnalyzer(context)
                    val wifiScanner = WifiScanner(context)
                    val networks = wifiScanner.getWifiNetworks()
                    locationAnalyzer?.analyzeLocation(networks)
                }
                startFusionPhase(callback)
            } catch (e: Exception) {
                Log.e(TAG, "Error in location analysis", e)
                callback("Error in location analysis: ${e.message}", AnalysisPhase.LOCATION)
                cleanup()
            }
        }
    }

    private fun startFusionPhase(callback: (String, AnalysisPhase) -> Unit) {
        currentPhase = AnalysisPhase.FUSION
        callback("Starting context fusion...", AnalysisPhase.FUSION)

        // Clean up location resources before LLM
        locationAnalyzer?.close()
        locationAnalyzer = null
        System.gc() // Request cleanup

        CoroutineScope(Dispatchers.Main).launch {
            try {
                withContext(Dispatchers.IO) {
                    val result = performContextFusion()
                    callback(result, AnalysisPhase.FUSION)
                }
                finishAnalysis(callback)
            } catch (e: Exception) {
                Log.e(TAG, "Error in fusion phase", e)
                callback("Error in fusion analysis: ${e.message}", AnalysisPhase.FUSION)
                cleanup()
            }
        }
    }

    private fun performContextFusion(): String {
//        val options = LlmInference.LlmInferenceOptions.builder()
//            .setModelPath("/data/local/tmp/llm/gemma2-2b-gpu.bin")
//            .setMaxTokens(512)  // Minimal tokens for short response
//            .setTopK(20)
//            .setTemperature(0.3f)
//            .build()
//
//        llmInference = LlmInference.createFromOptions(context, options)
//
//        val motionStorage = MotionStorage(context)
//        val fileStorage = FileStorage(context)
//        val motionHistory = motionStorage.getMotionHistory() ?: "No motion data"
//        val locationHistory = fileStorage.getLastResponse() ?: "No location data"
//
//        val prompt = """
//            Given the following data, describe the most likely activity in exactly 20 words:
//            Motion: $motionHistory
//            Location: $locationHistory
//        """.trimIndent()
//
//        val response = llmInference?.generateResponse(prompt) ?: "Fusion analysis failed"
//
//        // Clean up LLM immediately after use
//        llmInference?.close()
//        llmInference = null

        return "next:vvvv flagggg"
    }

    private fun finishAnalysis(callback: (String, AnalysisPhase) -> Unit) {
        currentPhase = AnalysisPhase.COMPLETE
        val results = getCombinedResults()
        callback(results, AnalysisPhase.COMPLETE)
        cleanup()
    }

    private fun getCombinedResults(): String {
        val resultBuilder = StringBuilder()

        // Motion results
        resultBuilder.append("=== Motion Analysis Results ===\n")
        val motionStorage = MotionStorage(context)
        val motionHistory = motionStorage.getMotionHistory()
        resultBuilder.append(motionHistory ?: "No motion data available")
        resultBuilder.append("\n\n")

        // Location results
        resultBuilder.append("=== Location Analysis Results ===\n")
        val fileStorage = FileStorage(context)
        val locationHistory = fileStorage.getLastResponse()
        resultBuilder.append(locationHistory ?: "No location data available")

        return resultBuilder.toString()
    }

    fun getCurrentPhase(): AnalysisPhase = currentPhase

    fun stopAnalysis() {
        cleanup()
    }

    private fun cleanup() {
        motionDetector?.stopDetection()
        motionDetector = null
        locationAnalyzer?.close()
        locationAnalyzer = null
        isAnalyzing = false
        currentPhase = AnalysisPhase.NONE
    }

    override fun close() {
        cleanup()
        llmInference?.close()
        llmInference = null
        System.gc() // Final cleanup
    }
}