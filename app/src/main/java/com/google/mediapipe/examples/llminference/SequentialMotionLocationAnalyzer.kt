package com.google.mediapipe.examples.llminference

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import java.io.Closeable

class SequentialMotionLocationAnalyzer(private val context: Context) : Closeable {
    companion object {
        private const val TAG = "SequentialAnalyzer"
        private const val MOTION_DURATION = 10000L // 10 seconds
    }

    private var isAnalyzing = false
    private var currentPhase = AnalysisPhase.NONE
    private var motionDetector: MotionDetector? = null
    private var locationAnalyzer: LocationAnalyzer? = null

    enum class AnalysisPhase {
        NONE,
        MOTION,
        LOCATION,
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

        // Create and start motion detector
        motionDetector = MotionDetector(context)
        motionDetector?.startDetection { motions ->
            callback("Detected motions: ${motions.joinToString(", ")}", AnalysisPhase.MOTION)
        }

        // Schedule end of motion phase
        CoroutineScope(Dispatchers.Main).launch {
            delay(MOTION_DURATION)
            finishMotionPhase(callback)
        }
    }

    private fun finishMotionPhase(callback: (String, AnalysisPhase) -> Unit) {
        // Clean up motion detection
        motionDetector?.stopDetection()
        motionDetector = null

        // Start location phase
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
                finishAnalysis(callback)
            } catch (e: Exception) {
                Log.e(TAG, "Error in location analysis", e)
                callback("Error in location analysis: ${e.message}", AnalysisPhase.LOCATION)
                cleanup()
            }
        }
    }

    private fun finishAnalysis(callback: (String, AnalysisPhase) -> Unit) {
        currentPhase = AnalysisPhase.COMPLETE

        // Get final results
        val results = getCombinedResults()
        callback(results, AnalysisPhase.COMPLETE)

        // Clean up
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
    }
}