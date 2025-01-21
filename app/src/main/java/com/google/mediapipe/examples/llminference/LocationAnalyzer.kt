package com.google.mediapipe.examples.llminference

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import java.io.Closeable

class LocationAnalyzer(private val context: Context) : Closeable {
    companion object {
        private const val TAG = "LocationAnalyzer"
    }

    private var llmInference: LlmInference? = null

    init {
        createLlmInference()
    }

    private fun createLlmInference() {
        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath("/data/local/tmp/llm/gemma2-2b-gpu.bin")
            .setMaxTokens(1024)  // Longer responses
            .setTopK(20)         // More focused predictions
            .setTemperature(0.3f)  // More deterministic
            .setRandomSeed(101)    // Enable temperature/topK effects
            .build()

        llmInference = LlmInference.createFromOptions(context, options)
    }

    fun analyzeLocation(ssids: List<String>): String {
        val prompt = """
            Based on the following WiFi network names, analyze where this location might be(response in summary:
            ${ssids.joinToString("\n")}
            Provide a brief analysis of the likely location.
        """.trimIndent()

        Log.d(TAG, "Sending prompt to LLM:")
        Log.d(TAG, prompt)

        val response = llmInference?.generateResponse(prompt)
            ?: throw IllegalStateException("LLM not initialized")

        Log.d(TAG, "Received response from LLM:")
        Log.d(TAG, response)

        return response
    }

    override fun close() {
        llmInference?.close()
        llmInference = null
    }
}