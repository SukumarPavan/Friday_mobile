package com.example.friday

import android.content.Context
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility class for enhanced logging
 */
object LogUtils {
    private const val TAG = "FridayLog"
    private var logFile: File? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    
    /**
     * Initialize the log file
     */
    fun initialize(context: Context) {
        try {
            logFile = File(context.filesDir, "friday_logs.txt")
            if (!logFile!!.exists()) {
                logFile!!.createNewFile()
            }
            
            // Truncate log file if it's too large (1MB)
            if (logFile!!.length() > 1024 * 1024) {
                logFile!!.delete()
                logFile!!.createNewFile()
            }
            
            appendLog("===== Log initialized at ${dateFormat.format(Date())} =====")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize log file: ${e.message}")
        }
    }
    
    /**
     * Log a debug message
     */
    fun d(tag: String, message: String) {
        Log.d(tag, message)
        appendLog("D/$tag: $message")
    }
    
    /**
     * Log an error message
     */
    fun e(tag: String, message: String, e: Exception? = null) {
        Log.e(tag, message, e)
        appendLog("E/$tag: $message")
        e?.let {
            appendLog("Exception: ${e.message}")
            appendLog("Stack trace: ${e.stackTraceToString()}")
        }
    }
    
    /**
     * Log and show an error to the user
     */
    fun showError(context: Context, tag: String, message: String, e: Exception? = null) {
        e(tag, message, e)
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
    
    /**
     * Append a message to the log file
     */
    private fun appendLog(message: String) {
        logFile?.let { file ->
            try {
                val timestamp = dateFormat.format(Date())
                val logMessage = "$timestamp - $message\n"
                
                FileOutputStream(file, true).use { output ->
                    output.write(logMessage.toByteArray())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write to log file: ${e.message}")
            }
        }
    }
    
    /**
     * Get the full log contents
     */
    fun getLogContents(context: Context): String {
        return try {
            logFile?.readText() ?: "No logs available"
        } catch (e: Exception) {
            "Failed to read logs: ${e.message}"
        }
    }
} 