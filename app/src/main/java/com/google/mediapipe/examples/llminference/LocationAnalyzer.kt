package com.google.mediapipe.examples.llminference

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference

class LocationAnalyzer(private val context: Context) {
    companion object {
        private const val TAG = "LocationAnalyzer"
    }

    private val llmInference: LlmInference by lazy {
        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath("/data/local/tmp/llm/gemma2-2b-gpu.bin")
            .setMaxTokens(1024)  // Longer responses
            .setTopK(100)         // More focused predictions
            .setTemperature(0.3f)  // More deterministic
            .setRandomSeed(101)    // Enable temperature/topK effects
            .build()

        LlmInference.createFromOptions(context, options)
    }

    fun analyzeLocation(ssids: List<String>): String {
        val prompt = """
            Based on the following WiFi network names, analyze where this location might be:
            ${ssids.joinToString("\n")}
            Provide a brief analysis of the likely location.
        """.trimIndent()

        Log.d(TAG, "Sending prompt to LLM:")
        Log.d(TAG, prompt)

        val response = llmInference.generateResponse(prompt)

        Log.d(TAG, "Received response from LLM:")
        Log.d(TAG, response)

        return response
    }
}