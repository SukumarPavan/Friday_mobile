package com.example.friday

import ai.picovoice.porcupine.Porcupine
import ai.picovoice.porcupine.PorcupineManager
import ai.picovoice.porcupine.PorcupineManagerCallback
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class VoiceDetectionService : Service() {
    private var porcupineManager: PorcupineManager? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var serviceJob: Job? = null
    private val TAG = "VoiceDetectionService"
    private var wakeLock: PowerManager.WakeLock? = null
    private val isRunning = AtomicBoolean(false)
    private val WAKE_WORD_MODEL = "Friday_en_android_v3_0_0.ppn" // Exact filename from assets folder
    
    override fun onCreate() {
        super.onCreate()
        LogUtils.d(TAG, "VoiceDetectionService created")
        
        // Use PowerManager with partial wake lock (minimal CPU power)
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Friday:VoiceDetectionWakeLock"
        )
        
        // Start initialization in a lightweight manner
        initializeWakeWordDetection()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        LogUtils.d(TAG, "VoiceDetectionService onStartCommand")
        Toast.makeText(this, "Voice detection service started", Toast.LENGTH_SHORT).show()
        return START_STICKY
    }

    private fun initializeWakeWordDetection() {
        // Only start if not already running
        if (isRunning.getAndSet(true)) {
            LogUtils.d(TAG, "Wake word detection already running")
            return
        }
        
        // Use a separate coroutine for initialization
        serviceJob = scope.launch {
            try {
                // Verify what files we have in assets
                val availableAssets = assets.list("") ?: emptyArray()
                LogUtils.d(TAG, "Available assets: ${availableAssets.joinToString()}")
                
                // Find a suitable wake word model in assets
                val wakeWordModels = availableAssets.filter { 
                    it.endsWith(".ppn") 
                }
                
                if (wakeWordModels.isEmpty()) {
                    reportError("No wake word model (.ppn file) found in assets")
                    return@launch
                }
                
                // Use the first available model if our default isn't available
                val modelToUse = if (availableAssets.contains(WAKE_WORD_MODEL)) {
                    WAKE_WORD_MODEL
                } else {
                    wakeWordModels.first().also {
                        LogUtils.d(TAG, "Using alternative wake word model: $it")
                    }
                }
                
                LogUtils.d(TAG, "Using wake word model: $modelToUse")
                
                val modelPath = AssetUtils.prepareWakeWordModel(applicationContext, modelToUse)
                if (modelPath == null) {
                    reportError("Failed to prepare wake word model")
                    return@launch
                }
                
                LogUtils.d(TAG, "Wake word model prepared at: $modelPath")
                
                // Use the official PorcupineManager which is more efficient than manual audio processing
                try {
                    val callback = PorcupineManagerCallback { keywordIndex ->
                        LogUtils.d(TAG, "Wake word detected with index: $keywordIndex")
                        sendBroadcast(Intent("WAKE_WORD_DETECTED"))
                    }
                    
                    // Use higher sensitivity to improve detection reliability
                    porcupineManager = PorcupineManager.Builder()
                        .setAccessKey("GfwPR/YcCPs+C0/i7RShNBjqAqCkbCrj+phHkAYlOUr6vaXJYDovbA==")
                        .setKeywordPaths(arrayOf(modelPath))
                        .setSensitivities(floatArrayOf(0.8f)) // Higher sensitivity (0.0-1.0)
                        .build(applicationContext, callback)
                    
                    LogUtils.d(TAG, "PorcupineManager initialized successfully")
                    notifySuccess("Wake word detection active. Say 'Friday' to activate.")
                    
                    // Acquire a wake lock with a 30-minute timeout
                    wakeLock?.acquire(30 * 60 * 1000L)
                    LogUtils.d(TAG, "Wake lock acquired")
                    
                    // Start the manager (this internally handles audio recording efficiently)
                    porcupineManager?.start()
                    LogUtils.d(TAG, "PorcupineManager started")
                    
                } catch (e: Exception) {
                    reportError("Error initializing wake word detection: ${e.message}")
                    LogUtils.e(TAG, "Error initializing PorcupineManager", e)
                }
                
            } catch (e: Exception) {
                reportError("Initialization error: ${e.message}")
                LogUtils.e(TAG, "Error in service initialization", e)
                stopSelf()
            }
        }
    }
    
    private fun reportError(message: String) {
        LogUtils.e(TAG, message)
        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
        isRunning.set(false)
    }
    
    private fun notifySuccess(message: String) {
        LogUtils.d(TAG, message)
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        LogUtils.d(TAG, "VoiceDetectionService being destroyed")
        isRunning.set(false)
        serviceJob?.cancel()
        
        try {
            // Release wake lock if held
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                LogUtils.d(TAG, "Wake lock released")
            }
            
            // Stop the manager which handles cleanup automatically
            porcupineManager?.stop()
            LogUtils.d(TAG, "PorcupineManager stopped")
            
            porcupineManager?.delete()
            porcupineManager = null
            LogUtils.d(TAG, "PorcupineManager deleted")
            
        } catch (e: Exception) {
            LogUtils.e(TAG, "Error during service shutdown", e)
        }
    }
}