package com.google.mediapipe.examples.llminference

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Request location permission
        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)

        setContent {
            MaterialTheme {
                LocationScreen()
            }
        }
    }
}
@Composable
fun LocationScreen() {
    val context = LocalContext.current
    var analysis by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Create and remember the LocationAnalyzer
    val locationAnalyzer = remember { LocationAnalyzer(context) }

    // Cleanup when the composable is disposed
    DisposableEffect(locationAnalyzer) {
        onDispose {
            locationAnalyzer.close()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(
            onClick = {
                isLoading = true
                kotlinx.coroutines.MainScope().launch {
                    try {
                        withContext(Dispatchers.IO) {
                            val wifiScanner = WifiScanner(context)
                            val networks = wifiScanner.getWifiNetworks()
                            analysis = locationAnalyzer.analyzeLocation(networks)
                        }
                    } catch (e: Exception) {
                        Log.e("LocationScreen", "Error analyzing location", e)
                        analysis = "Error: ${e.message}"
                    } finally {
                        isLoading = false
                    }
                }
            }
        ) {
            Text("Scan Location")
        }

        Spacer(modifier = Modifier.height(16.dp))

        when {
            isLoading -> CircularProgressIndicator()
            analysis != null -> Text(analysis!!)
        }
    }
}


//import android.content.Context
//import android.graphics.Bitmap
//import android.os.Bundle
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.padding
//import androidx.compose.material3.CircularProgressIndicator
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Surface
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.DisposableEffect
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.unit.dp
//import com.google.mediapipe.tasks.genai.llminference.LlmInference
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.withContext
//
//
//class MainActivity : ComponentActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        val modelPath = "/data/local/tmp/llm/gemma2-2b-gpu.bin"
//
//        setContent {
//            MaterialTheme {
//                Surface(
//                    modifier = Modifier.fillMaxSize(),
//                    color = MaterialTheme.colorScheme.background
//                ) {
//                    // Replace yourBitmap with actual image
//                    val dummyBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
//                    VisionInferenceScreen(
//                        modelPath = modelPath,
//                        image = dummyBitmap,
//                        prompt = "Describe what you see in this image"
//                    )
//                }
//            }
//        }
//    }
//}
//
//class VisionInferenceHelper(private val context: Context) {
//    private var llmInference: LlmInference? = null
//
//    suspend fun initializeLLM(modelPath: String) {
//        withContext(Dispatchers.IO) {
//            try {
//                val options = LlmInference.LlmInferenceOptions.builder()
//                    .setModelPath(modelPath)
//                    .setMaxTokens(1024)
//                    .setTopK(40)
//                    .setTemperature(0.8f)
//                    .setRandomSeed(101)
//                    .build()
//
//                llmInference = LlmInference.createFromOptions(context, options)
//            } catch (e: Exception) {
//                throw Exception("Failed to initialize LLM: ${e.message}")
//            }
//        }
//    }
//
//    suspend fun generateVisionResponse(prompt: String, image: Bitmap): String {
//        return withContext(Dispatchers.IO) {
//            try {
//                llmInference?.generateResponse(prompt) ?:
//                throw Exception("LLM not initialized")
//            } catch (e: Exception) {
//                throw Exception("Failed to generate response: ${e.message}")
//            }
//        }
//    }
//
//    fun close() {
//        llmInference?.close()
//    }
//}
//
//@Composable
//fun VisionInferenceScreen(
//    modelPath: String,
//    image: Bitmap,
//    prompt: String
//) {
//    var response by remember { mutableStateOf<String?>(null) }
//    var error by remember { mutableStateOf<String?>(null) }
//    var isLoading by remember { mutableStateOf(true) }
//
//    val context = LocalContext.current
//    val helper = remember(context) { VisionInferenceHelper(context) }
//
//    LaunchedEffect(Unit) {
//        try {
//            helper.initializeLLM(modelPath)
//            response = helper.generateVisionResponse(prompt, image)
//            isLoading = false
//        } catch (e: Exception) {
//            error = e.message
//            isLoading = false
//        }
//    }
//
//    DisposableEffect(Unit) {
//        onDispose {
//            helper.close()
//        }
//    }
//
//    Box(
//        modifier = Modifier.fillMaxSize(),
//        contentAlignment = Alignment.Center
//    ) {
//        Column(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(16.dp),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            when {
//                isLoading -> {
//                    CircularProgressIndicator()
//                    Text(
//                        text = "Generating response...",
//                        modifier = Modifier.padding(top = 16.dp)
//                    )
//                }
//                error != null -> {
//                    Text(
//                        text = "Error: $error",
//                        color = MaterialTheme.colorScheme.error
//                    )
//                }
//                response != null -> {
//                    Text(
//                        text = response!!,
//                        style = MaterialTheme.typography.bodyLarge
//                    )
//                }
//            }
//        }
//    }
//}
