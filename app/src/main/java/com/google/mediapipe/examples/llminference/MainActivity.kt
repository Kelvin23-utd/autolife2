package com.google.mediapipe.examples.llminference

import android.Manifest
import android.graphics.BitmapFactory
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
        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)

        setContent {
            MaterialTheme {
                LocationAndVisionScreen()
            }
        }
    }
}

@Composable
fun LocationAndVisionScreen() {
    val context = LocalContext.current
    var locationAnalysis by remember { mutableStateOf<String?>(null) }
    var visionAnalysis by remember { mutableStateOf<String?>(null) }
    var isLocationLoading by remember { mutableStateOf(false) }
    var isVisionLoading by remember { mutableStateOf(false) }

    // Lazy state holders for analyzers
    var locationAnalyzer by remember { mutableStateOf<LocationAnalyzer?>(null) }
    var llmVisionTest by remember { mutableStateOf<LlmVisionTest?>(null) }

    // Clean up function
    fun cleanupResources() {
        try {
            locationAnalyzer?.let {
                it.close()
                locationAnalyzer = null
            }
            llmVisionTest?.let {
                it.close()
                llmVisionTest = null
            }
            System.gc()
        } catch (e: Exception) {
            Log.e("Cleanup", "Error during cleanup", e)
        }
    }

    // Cleanup when the composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            cleanupResources()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Location scanning section
        Button(
            onClick = {
                isLocationLoading = true
                kotlinx.coroutines.MainScope().launch {
                    try {
                        withContext(Dispatchers.IO) {
                            // Lazy initialization of LocationAnalyzer
                            if (locationAnalyzer == null) {
                                locationAnalyzer = LocationAnalyzer(context)
                            }

                            val wifiScanner = WifiScanner(context)
                            val networks = wifiScanner.getWifiNetworks()
                            locationAnalysis = locationAnalyzer?.analyzeLocation(networks)
                                ?: "Error: Analyzer not initialized"
                        }
                    } catch (e: Exception) {
                        Log.e("LocationScreen", "Error analyzing location", e)
                        locationAnalysis = "Error: ${e.message}"
                        // Cleanup on error
                        locationAnalyzer?.close()
                        locationAnalyzer = null
                    } finally {
                        isLocationLoading = false
                    }
                }
            },
            enabled = !isLocationLoading
        ) {
            Text("Scan Location")
        }

        Spacer(modifier = Modifier.height(8.dp))

        when {
            isLocationLoading -> CircularProgressIndicator()
            locationAnalysis != null -> Text("Location: ${locationAnalysis!!}")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Divider()
        Spacer(modifier = Modifier.height(16.dp))

        // Vision test section
        Button(
            onClick = {
                isVisionLoading = true
                kotlinx.coroutines.MainScope().launch {
                    try {
                        withContext(Dispatchers.IO) {
                            // Lazy initialization of LlmVisionTest
                            if (llmVisionTest == null) {
                                llmVisionTest = LlmVisionTest(context)
                            }

                            val bitmap = BitmapFactory.decodeResource(
                                context.resources,
                                R.drawable.s1
                            )
                            visionAnalysis = llmVisionTest?.testImageAnalysis(bitmap)
                                ?: "Error: Vision test not initialized"
                        }
                    } catch (e: Exception) {
                        Log.e("VisionTest", "Error running vision test", e)
                        visionAnalysis = "Error: ${e.message}"
                        // Cleanup on error
                        llmVisionTest?.close()
                        llmVisionTest = null
                    } finally {
                        isVisionLoading = false
                    }
                }
            },
            enabled = !isVisionLoading
        ) {
            Text("Run Vision Test")
        }

        Spacer(modifier = Modifier.height(8.dp))

        when {
            isVisionLoading -> CircularProgressIndicator()
            visionAnalysis != null -> Text("Vision: ${visionAnalysis!!}")
        }
    }
}