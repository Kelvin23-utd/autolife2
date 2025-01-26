package com.google.mediapipe.examples.llminference

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable

class ContextFusionAnalyzer(private val context: Context) : Closeable {
    companion object {
        private const val TAG = "ContextFusionAnalyzer"
    }

    @Volatile
    private var llmInference: LlmInference? = null
    private var motionStorage: MotionStorage? = null
    private var fileStorage: FileStorage? = null

    @Synchronized
    private fun initializeLlm() {
        if (llmInference == null) {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath("/data/local/tmp/llm/gemma2-2b-gpu.bin")
                .setMaxTokens(1024)
                .setTopK(20)
                .setTemperature(0.3f)
                .build()

            llmInference = LlmInference.createFromOptions(context, options)
        }
    }

    suspend fun performFusion(): String = withContext(Dispatchers.IO) {
        try {
            initializeLlm()
            motionStorage = MotionStorage(context)
            fileStorage = FileStorage(context)

            val motionHistory = motionStorage?.getMotionHistory() ?: "No motion data"
            val locationHistory = fileStorage?.getLastResponse() ?: "No location data"

            val truncatedMotion = motionHistory.takeLast(200)
            val truncatedLocation = locationHistory.takeLast(200)

            val prompt = """
                Given the following data, describe the most likely activity in exactly 20 words:
                Motion: $truncatedMotion
                Location: $truncatedLocation
            """.trimIndent()

            Log.d(TAG, "Sending fusion prompt: $prompt")

            val response = llmInference?.generateResponse(prompt) ?: "Fusion analysis failed"
            Log.d(TAG, "Received fusion response: $response")

            response
        } catch (e: Exception) {
            Log.e(TAG, "Error during fusion analysis", e)
            "Error during fusion: ${e.message}"
        }
    }

    @Synchronized
    override fun close() {
        try {
            llmInference?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing LLM inference", e)
        } finally {
            llmInference = null
            motionStorage = null
            fileStorage = null
        }
    }
}