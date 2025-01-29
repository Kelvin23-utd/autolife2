package com.google.mediapipe.examples.llminference

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import java.io.Closeable
import java.lang.ref.WeakReference
import com.google.mediapipe.tasks.genai.llminference.LlmInference

class SequentialMotionLocationAnalyzer(context: Context) : Closeable {
    companion object {
        private const val TAG = "SequentialAnalyzer"
        private const val MOTION_DURATION = 10000L // 10 seconds
    }

    private val contextRef = WeakReference(context)
    private val analyzerScope = CoroutineScope(Dispatchers.Main + Job())
    private var currentJob: Job? = null

    private var isAnalyzing = false
    private var currentPhase = AnalysisPhase.NONE
    private var motionDetector: MotionDetector? = null
    private var locationAnalyzer: LocationAnalyzer? = null
    // Removed LLM inference reference since fusion is simplified

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

        val context = contextRef.get() ?: run {
            callback("Context no longer available", AnalysisPhase.NONE)
            return
        }

        // Start with motion phase
        startMotionPhase(callback)
    }

    private fun startMotionPhase(callback: (String, AnalysisPhase) -> Unit) {
        isAnalyzing = true
        currentPhase = AnalysisPhase.MOTION
        System.gc() // Request garbage collection before starting

        val context = contextRef.get() ?: run {
            cleanup()
            callback("Context no longer available", AnalysisPhase.NONE)
            return
        }

        motionDetector = MotionDetector(context)
        motionDetector?.startDetection { motions ->
            callback("Detected motions: ${motions.joinToString(", ")}", AnalysisPhase.MOTION)
        }

        currentJob = analyzerScope.launch {
            try {
                delay(MOTION_DURATION)
                finishMotionPhase(callback)
            } catch (e: CancellationException) {
                cleanup()
                throw e
            }
        }
    }

    private fun finishMotionPhase(callback: (String, AnalysisPhase) -> Unit) {
        val context = contextRef.get() ?: run {
            cleanup()
            callback("Context no longer available", AnalysisPhase.NONE)
            return
        }

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

        val context = contextRef.get() ?: run {
            cleanup()
            callback("Context no longer available", AnalysisPhase.NONE)
            return
        }

        currentJob = analyzerScope.launch {
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

        currentJob = analyzerScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val context = contextRef.get() ?: run {
                        cleanup()
                        callback("Context no longer available", AnalysisPhase.NONE)
                        return@withContext
                    }

                    val result = performContextFusion(context)
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

    private fun performContextFusion(context: Context): String {
        return "Fusion phase completed" // Simplified return
    }

    private fun finishAnalysis(callback: (String, AnalysisPhase) -> Unit) {
        currentPhase = AnalysisPhase.COMPLETE
        val results = getCombinedResults()
        callback(results, AnalysisPhase.COMPLETE)
        cleanup()
    }

    private fun getCombinedResults(): String {
        val context = contextRef.get() ?: return "Context no longer available"

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
        currentJob?.cancel()
        cleanup()
    }

    private fun cleanup() {
        currentJob?.cancel()
        currentJob = null
        motionDetector?.stopDetection()
        motionDetector = null
        locationAnalyzer?.close()
        locationAnalyzer = null
        isAnalyzing = false
        currentPhase = AnalysisPhase.NONE
        System.gc() // Request cleanup
    }

    override fun close() {
        stopAnalysis()
        analyzerScope.cancel()
        System.gc() // Final cleanup
    }
}