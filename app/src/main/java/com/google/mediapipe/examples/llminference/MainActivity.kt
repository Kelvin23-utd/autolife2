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
import kotlinx.coroutines.delay
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

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
    var isFusing by remember { mutableStateOf(false) }
    var fusionResult by remember { mutableStateOf<String?>(null) }
    var memoryInfo by remember { mutableStateOf(MemoryMonitor.getMemoryInfo()) }

    val analyzer = remember { SequentialMotionLocationAnalyzer(context) }
    val fusionAnalyzer = remember { ContextFusionAnalyzer(context) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Fixed content section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Memory Monitor Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Memory Monitor",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        memoryInfo.toString(),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Sequential Analysis Card
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
                                scope.launch {
                                    memoryInfo = MemoryMonitor.getMemoryInfo() // Initial reading
                                    analyzer.startAnalysis { result, phase ->
                                        currentAnalysis = result
                                        currentPhase = phase
                                        scope.launch {
                                            memoryInfo = MemoryMonitor.getMemoryInfo() // Update after phase change
                                        }
                                        if (phase == SequentialMotionLocationAnalyzer.AnalysisPhase.COMPLETE) {
                                            isAnalyzing = false
                                        }
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
                            else -> "Start Analysis"
                        })
                    }

                    if (isAnalyzing) {
                        LinearProgressIndicator(
                            progress = when(currentPhase) {
                                SequentialMotionLocationAnalyzer.AnalysisPhase.MOTION -> 0.3f
                                SequentialMotionLocationAnalyzer.AnalysisPhase.LOCATION -> 0.7f
                                SequentialMotionLocationAnalyzer.AnalysisPhase.COMPLETE -> 1.0f
                                else -> 0f
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

//                    currentAnalysis?.let {
//                        Text(
//                            text = it,
//                            style = MaterialTheme.typography.bodyMedium
//                        )
//                    }
                }
            }

            // Fusion Test Card
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Test Context Fusion", style = MaterialTheme.typography.titleMedium)

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isFusing,
                        onClick = {
                            scope.launch {
                                isFusing = true
                                memoryInfo = MemoryMonitor.getMemoryInfo() // Initial reading
                                fusionResult = fusionAnalyzer.performFusion()
                                memoryInfo = MemoryMonitor.getMemoryInfo() // Final reading
                                isFusing = false
                            }
                        }
                    ) {
                        Text(if (isFusing) "Fusing Contexts..." else "Test Fusion")
                    }

                    if (isFusing) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }

//                    fusionResult?.let {
//                        Text(
//                            text = "Fusion Result:\n$it",
//                            style = MaterialTheme.typography.bodyMedium
//                        )
//                    }
                }
            }
        }

        // Scrollable content section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Column {
                        currentAnalysis?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        fusionResult?.let {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Fusion Result:\n$it",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}