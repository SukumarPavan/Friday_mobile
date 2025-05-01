package com.example.friday

import ai.picovoice.porcupine.PorcupineException
import ai.picovoice.porcupine.PorcupineManager
import ai.picovoice.porcupine.PorcupineManagerCallback
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.friday.ui.theme.FridayTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"
    private val detectionState = mutableStateOf("Waiting...")
    private var porcupineManager: PorcupineManager? = null
    private val lastDetectionTime = mutableStateOf<Long>(0)
    
    // Simple permission request
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
            detectionState.value = "Permission granted"
            initPorcupine()
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            detectionState.value = "Permission denied"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            // Safe initialization of logging
            try {
                Log.d(TAG, "App starting")
            } catch (e: Exception) {
                // Fail silently
            }
            
            // Set initial state
            detectionState.value = "Started app"
            
            setContent {
                FridayTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        WakeWordDetectionScreen(detectionState)
                    }
                }
            }
            
        } catch (e: Exception) {
            Toast.makeText(this, "Error starting app: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onResume() {
        super.onResume()
        
        // Check permissions safely
        try {
            val hasPermission = ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            
            if (hasPermission) {
                detectionState.value = "Has permission"
                initPorcupine()
            } else {
                detectionState.value = "Needs permission"
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        } catch (e: Exception) {
            detectionState.value = "Error: ${e.message}"
            Log.e(TAG, "Error checking permissions: ${e.message}", e)
        }
    }
    
    override fun onPause() {
        super.onPause()
        stopPorcupine()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopPorcupine()
    }
    
    private fun initPorcupine() {
        try {
            if (porcupineManager != null) {
                // Already initialized
                return
            }
            
            detectionState.value = "Initializing..."
            Log.d(TAG, "Starting to initialize Porcupine")
            
            // Check if wake word file exists
            val assetManager = applicationContext.assets
            val files = assetManager.list("")
            Log.d(TAG, "Available assets: ${files?.joinToString(", ")}")
            
            // Use the access key from our utility class
            val accessKey = AccessKeyUtil.PICOVOICE_ACCESS_KEY
            Log.d(TAG, "Using access key: ${accessKey.take(5)}...") // Only log first 5 chars for security
            
            // Initialize Porcupine
            porcupineManager = PorcupineManager.Builder()
                .setAccessKey(accessKey)
                .setKeywordPath("Friday_en_android_v3_0_0.ppn")
                .setSensitivity(0.7f) // Adjust sensitivity as needed
                .build(this, object : PorcupineManagerCallback {
                    override fun invoke(keywordIndex: Int) {
                        // This is called when the wake word is detected
                        Log.d(TAG, "Wake word detected! Index: $keywordIndex")
                        
                        // Update UI on main thread
                        runOnUiThread {
                            detectionState.value = "Detected!"
                            lastDetectionTime.value = System.currentTimeMillis()
                            
                            // Reset status after 3 seconds
                            CoroutineScope(Dispatchers.Main).launch {
                                delay(3000)
                                if ((System.currentTimeMillis() - lastDetectionTime.value) >= 3000) {
                                    detectionState.value = "Listening..."
                                }
                            }
                        }
                    }
                })
            
            // Start listening
            Log.d(TAG, "Porcupine initialized, starting audio processing")
            porcupineManager?.start()
            detectionState.value = "Listening..."
            Log.d(TAG, "Porcupine started successfully")
            
        } catch (e: PorcupineException) {
            detectionState.value = "Error: ${e.message}"
            Log.e(TAG, "Failed to initialize Porcupine: ${e.message}", e)
            Toast.makeText(this, "Wake word detection error: ${e.message}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            detectionState.value = "Error: ${e.message}"
            Log.e(TAG, "Unexpected error initializing Porcupine: ${e.message}", e)
            Toast.makeText(this, "Wake word detection error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun stopPorcupine() {
        try {
            porcupineManager?.stop()
            porcupineManager?.delete()
            porcupineManager = null
            Log.d(TAG, "Porcupine stopped and resources released")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping Porcupine: ${e.message}", e)
        }
    }
}

@Composable
fun WakeWordDetectionScreen(detectionState: MutableState<String>) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Friday Voice Assistant",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Status",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    StatusIndicator(detectionState.value)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Show additional text based on status
                    val instruction = when (detectionState.value) {
                        "Listening..." -> "Say \"Friday\" to activate"
                        "Detected!" -> "Wake word detected!"
                        "Permission denied" -> "Microphone permission required"
                        else -> detectionState.value
                    }
                    
                    Text(
                        text = instruction,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun StatusIndicator(status: String) {
    val backgroundColor = when (status) {
        "Detected!" -> Color(0xFF4CAF50) // Green
        "Listening..." -> Color(0xFF2196F3) // Blue
        "Initializing..." -> Color(0xFFFFC107) // Yellow
        "Permission denied" -> Color(0xFFF44336) // Red
        else -> Color(0xFF9E9E9E) // Gray
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .background(backgroundColor, shape = MaterialTheme.shapes.medium)
                .padding(16.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (status == "Listening...") {
                CircularProgressIndicator(
                    modifier = Modifier.padding(end = 8.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            }
            
            Text(
                text = status,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(start = if (status == "Listening...") 8.dp else 0.dp)
            )
        }
    }
} 