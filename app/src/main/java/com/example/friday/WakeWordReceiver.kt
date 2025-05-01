package com.example.friday

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.widget.Toast
import android.os.Build

class WakeWordReceiver : BroadcastReceiver() {
    private val TAG = "WakeWordReceiver"
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "WAKE_WORD_DETECTED" -> {
                Log.d(TAG, "Wake word detected!")
                
                // Play a beep sound
                try {
                    val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
                    toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
                    toneGenerator.release()
                } catch (e: Exception) {
                    Log.e(TAG, "Error playing tone: ${e.message}")
                }
                
                // Vibrate
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                        val vibrator = vibratorManager.defaultVibrator
                        vibrator.vibrate(android.os.VibrationEffect.createOneShot(300, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(android.os.VibrationEffect.createOneShot(300, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator.vibrate(300)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error vibrating: ${e.message}")
                }
                
                // Show toast
                Toast.makeText(context, "Wake word detected! I'm listening...", Toast.LENGTH_SHORT).show()
            }
        }
    }
} 