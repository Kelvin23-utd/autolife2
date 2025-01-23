package com.google.mediapipe.examples.llminference

import android.Manifest
import android.content.ComponentCallbacks2
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
    init {
        // Request lower memory pressure
        System.gc()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
                // Clear any caches or non-essential data
                System.gc()
            }
        }
    }
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACTIVITY_RECOGNITION
            )
        )

        setContent {
            MaterialTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    var locationAnalysis by remember { mutableStateOf<String?>(null) }
    var motionAnalysis by remember { mutableStateOf<String?>(null) }
    var motionHistory by remember { mutableStateOf<String?>(null) }
    var combinedAnalysis by remember { mutableStateOf<String?>(null) }
    var isLocationLoading by remember { mutableStateOf(false) }
    var isMotionTracking by remember { mutableStateOf(false) }
    var isCombinedAnalyzing by remember { mutableStateOf(false) }

    // Create analyzers
    val locationAnalyzer = remember(context) { LocationAnalyzer(context) }
    val motionDetector = remember(context) { MotionDetector(context) }
    val motionLocationAnalyzer = remember(context) { MotionLocationAnalyzer(context) }

    // Cleanup when the composable is disposed
    DisposableEffect(key1 = locationAnalyzer, key2 = motionDetector, key3 = motionLocationAnalyzer) {
        onDispose {
            locationAnalyzer.close()
            if (isMotionTracking) {
                motionDetector.stopDetection()
            }
            motionLocationAnalyzer.close()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Combined Analysis Section
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Combined Motion & Location Analysis",
                    style = MaterialTheme.typography.titleMedium)

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (!isCombinedAnalyzing) {
                            isCombinedAnalyzing = true
                            motionLocationAnalyzer.startAnalysis { result ->
                                combinedAnalysis = result
                            }
                        } else {
                            motionLocationAnalyzer.stopAnalysis()
                            isCombinedAnalyzing = false
                        }
                    }
                ) {
                    Text(if (isCombinedAnalyzing) "Stop Combined Analysis" else "Start Combined Analysis")
                }

                combinedAnalysis?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Divider for separation
        Divider()

        // Individual Analysis Sections
        Text("Individual Analysis Tools", style = MaterialTheme.typography.titleMedium)

        // Location scanning section
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Location Analysis", style = MaterialTheme.typography.titleSmall)
                Button(
                    onClick = {
                        isLocationLoading = true
                        kotlinx.coroutines.MainScope().launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    val wifiScanner = WifiScanner(context)
                                    val networks = wifiScanner.getWifiNetworks()
                                    locationAnalysis = locationAnalyzer.analyzeLocation(networks)
                                }
                            } catch (e: Exception) {
                                Log.e("MainScreen", "Error analyzing location", e)
                                locationAnalysis = "Error: ${e.message}"
                            } finally {
                                isLocationLoading = false
                            }
                        }
                    }
                ) {
                    Text("Scan Location")
                }

                when {
                    isLocationLoading -> CircularProgressIndicator()
                    locationAnalysis != null -> Text(locationAnalysis!!)
                }
            }
        }

        // Motion detection section
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Motion Analysis", style = MaterialTheme.typography.titleSmall)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (!isMotionTracking) {
                                motionDetector.startDetection { motions ->
                                    motionAnalysis = "Current motions: ${motions.joinToString(", ")}"
                                    motionHistory = motionDetector.getMotionHistory()
                                }
                            } else {
                                motionDetector.stopDetection()
                                motionAnalysis = null
                            }
                            isMotionTracking = !isMotionTracking
                        }
                    ) {
                        Text(if (isMotionTracking) "Stop Detection" else "Start Detection")
                    }

                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            motionDetector.clearMotionHistory()
                            motionHistory = null
                        }
                    ) {
                        Text("Clear History")
                    }
                }

                motionAnalysis?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (!motionHistory.isNullOrEmpty()) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = "Motion History (Last 10 Detections):",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = motionHistory!!,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}