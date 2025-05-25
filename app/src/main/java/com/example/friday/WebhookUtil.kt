package com.example.friday

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

object WebhookUtil {
    private const val TAG = "WebhookUtil"
    
    // Replace this with your n8n webhook URL
    private const val N8N_WEBHOOK_URL = "https://mako-generous-frequently.ngrok-free.app/webhook/f9dff7fd-ce94-4e72-b57b-628729a13715"
    
    suspend fun sendWebhook(data: Map<String, Any> = mapOf("event" to "wake_word_detected")) {
        try {
            withContext(Dispatchers.IO) {
                val url = URL(N8N_WEBHOOK_URL)
                val connection = url.openConnection() as HttpURLConnection
                
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                connection.doOutput = true
                connection.doInput = true
                connection.connectTimeout = 10000 // 10 seconds
                connection.readTimeout = 10000 // 10 seconds
                
                // Create JSON payload
                val jsonObject = JSONObject()
                data.forEach { (key, value) ->
                    jsonObject.put(key, value)
                }
                
                // Add timestamp
                jsonObject.put("timestamp", System.currentTimeMillis())
                
                val jsonString = jsonObject.toString()
                Log.d(TAG, "Sending webhook request: $jsonString")
                
                // Write the JSON payload
                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(jsonString)
                    writer.flush()
                }
                
                // Get response code
                val responseCode = connection.responseCode
                Log.d(TAG, "Response code: $responseCode")
                
                // Read response
                val response = if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                } else {
                    // Read error stream if available
                    val errorStream = connection.errorStream
                    if (errorStream != null) {
                        BufferedReader(InputStreamReader(errorStream)).use { it.readText() }
                    } else {
                        "No error message available"
                    }
                }
                
                Log.d(TAG, "Response body: $response")
                
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    Log.e(TAG, "Error response: $response")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending webhook: ${e.message}", e)
        }
    }

    suspend fun sendSpeechInput(userInput: String): String? {
        return try {
            withContext(Dispatchers.IO) {
                val url = URL(N8N_WEBHOOK_URL)
                val connection = url.openConnection() as HttpURLConnection
                
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                connection.doOutput = true
                connection.doInput = true
                connection.connectTimeout = 10000 // 10 seconds
                connection.readTimeout = 10000 // 10 seconds
                
                // Create JSON payload
                val jsonObject = JSONObject().apply {
                    put("event", "speech_input")
                    put("user_input", userInput)
                    put("timestamp", System.currentTimeMillis())
                }
                
                val jsonString = jsonObject.toString()
                Log.d(TAG, "Sending webhook request: $jsonString")
                
                // Write the JSON payload
                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(jsonString)
                    writer.flush()
                }
                
                // Get response code
                val responseCode = connection.responseCode
                Log.d(TAG, "Response code: $responseCode")
                
                // Read response
                val response = if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                } else {
                    // Read error stream if available
                    val errorStream = connection.errorStream
                    if (errorStream != null) {
                        BufferedReader(InputStreamReader(errorStream)).use { it.readText() }
                    } else {
                        "No error message available"
                    }
                }
                
                Log.d(TAG, "Response body: $response")
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    response
                } else {
                    Log.e(TAG, "Error response: $response")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending speech input: ${e.message}", e)
            null
        }
    }
} 