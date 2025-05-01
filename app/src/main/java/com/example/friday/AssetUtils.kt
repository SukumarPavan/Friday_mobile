package com.example.friday

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object AssetUtils {
    private const val TAG = "AssetUtils"
    
    /**
     * Checks if the wake word model exists in assets and copies it to internal storage if needed
     * @return File path to the model file or null if failed
     */
    fun prepareWakeWordModel(context: Context, assetName: String = "hey_friday_windows.ppn"): String? {
        try {
            // List all assets to verify the file exists
            val assets = context.assets.list("") ?: emptyArray()
            Log.d(TAG, "Assets found: ${assets.joinToString()}")
            
            if (!assets.contains(assetName)) {
                Log.e(TAG, "Wake word model not found in assets: $assetName")
                return null
            }
            
            // Copy to internal storage
            val destFile = File(context.filesDir, assetName)
            if (!destFile.exists()) {
                context.assets.open(assetName).use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                Log.d(TAG, "Asset copied to: ${destFile.absolutePath}")
            } else {
                Log.d(TAG, "Using existing file: ${destFile.absolutePath}")
            }
            
            return destFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing wake word model: ${e.message}")
            e.printStackTrace()
            return null
        }
    }
} 