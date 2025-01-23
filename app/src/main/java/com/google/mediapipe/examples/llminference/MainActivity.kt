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
    var currentAnalysis by remember { mutableStateOf<String?>(null) }
    var currentPhase by remember { mutableStateOf(SequentialMotionLocationAnalyzer.AnalysisPhase.NONE) }
    var isAnalyzing by remember { mutableStateOf(false) }

    val analyzer = remember { SequentialMotionLocationAnalyzer(context) }

    // Cleanup
    DisposableEffect(analyzer) {
        onDispose {
            analyzer.close()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Sequential Analysis", style = MaterialTheme.typography.titleMedium)

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (!isAnalyzing) {
                            isAnalyzing = true
                            analyzer.startAnalysis { result, phase ->
                                currentAnalysis = result
                                currentPhase = phase
                                if (phase == SequentialMotionLocationAnalyzer.AnalysisPhase.COMPLETE) {
                                    isAnalyzing = false
                                }
                            }
                        } else {
                            analyzer.stopAnalysis()
                            isAnalyzing = false
                        }
                    }
                ) {
                    Text(when(currentPhase) {
                        SequentialMotionLocationAnalyzer.AnalysisPhase.MOTION -> "Motion Detection in Progress..."
                        SequentialMotionLocationAnalyzer.AnalysisPhase.LOCATION -> "Location Analysis in Progress..."
                        SequentialMotionLocationAnalyzer.AnalysisPhase.COMPLETE -> "Analysis Complete"
                        SequentialMotionLocationAnalyzer.AnalysisPhase.NONE -> "Start Analysis"
                    })
                }

                // Progress indicator
                if (isAnalyzing) {
                    LinearProgressIndicator(
                        progress = when(currentPhase) {
                            SequentialMotionLocationAnalyzer.AnalysisPhase.MOTION -> 0.3f
                            SequentialMotionLocationAnalyzer.AnalysisPhase.LOCATION -> 0.7f
                            SequentialMotionLocationAnalyzer.AnalysisPhase.COMPLETE -> 1.0f
                            SequentialMotionLocationAnalyzer.AnalysisPhase.NONE -> 0f
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Results display
                currentAnalysis?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}