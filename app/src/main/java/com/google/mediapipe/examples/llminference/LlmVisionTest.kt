package com.google.mediapipe.examples.llminference;

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference

class LlmVisionTest(private val context: Context) {
    companion object {
        private const val TAG = "LlmVisionTest"
    }

    private var llmInference: LlmInference? = null

    init {
        createLlmInference()
    }

    private fun createLlmInference() {
        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath("/data/local/tmp/llm/gemma2-2b-gpu.bin")
            .setMaxTokens(1024)
            .setTopK(40)
            .setTemperature(0.8f)
            .setRandomSeed(101)
            .build()

        llmInference = LlmInference.createFromOptions(context, options)
    }

    fun testImageAnalysis(bitmap: Bitmap): String {
        Log.d(TAG, "Starting image analysis...")

        // Pass in the prompt text to generateResponse
        val prompt = "summrize this image within 20 words."

        val response = llmInference?.generateResponse(prompt)
            ?: throw IllegalStateException("LLM not initialized")

        Log.d(TAG, "Generated response: $response")
        return response
    }

    fun close() {
        llmInference?.close()
        llmInference = null
    }
}