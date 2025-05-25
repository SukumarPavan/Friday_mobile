package com.example.friday

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.AlarmClock
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.RecognitionListener
import android.util.Log
import android.view.View
import android.view.InflateException
import android.widget.Toast
import android.widget.VideoView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.speech.tts.TextToSpeech
import android.os.Handler
import android.os.Looper

class FridayTasksActivity : AppCompatActivity() {
    private lateinit var commandLogRecyclerView: RecyclerView
    private lateinit var tasksRecyclerView: RecyclerView
    private val commandLog = mutableListOf<String>()
    private val tasks = listOf(
        Task("Make a Call", R.drawable.ic_call, "Call [contact name]"),
        Task("Send Message", R.drawable.ic_message, "Message [contact] [message]"),
        Task("Set Alarm", R.drawable.ic_alarm, "Set alarm for [time]"),
        Task("Open App", R.drawable.ic_apps, "Open [app name]"),
        Task("Set Timer", R.drawable.ic_timer, "Set timer for [duration]"),
        Task("Take Note", R.drawable.ic_note, "Note [content]")
    )
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var circleVideo: VideoView
    private lateinit var tts: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Activity started")
        // Temporarily comment out video setup to isolate inflate issue
        // videoView = findViewById(R.id.circleVideo)
        // videoView.setVideoURI(Uri.parse("android.resource://" + packageName + "/" + R.raw.circle))
        // videoView.setOnPreparedListener { mp -> mp.isLooping = true }

        try {
            setContentView(R.layout.activity_friday_tasks)
            Log.d(TAG, "onCreate: setContentView done")

            // Set fallback background colors if needed
            findViewById<MaterialCardView>(R.id.featuresCard).setCardBackgroundColor(ContextCompat.getColor(this, R.color.design_default_color_primary))
            findViewById<MaterialCardView>(R.id.commandLogCard).setCardBackgroundColor(ContextCompat.getColor(this, R.color.design_default_color_primary))

            // Find views after setContentView
            val featuresHeader: TextView = findViewById(R.id.featuresHeader)
            val featuresContent: LinearLayout = findViewById(R.id.featuresContent)
            val commandLogHeader: TextView = findViewById(R.id.commandLogHeader)
            val commandLogContent: LinearLayout = findViewById(R.id.commandLogContent)

            // Fallback: Set background color programmatically in case resource is missing
            try {
                featuresHeader.setBackgroundColor(resources.getColor(android.R.color.holo_blue_dark))
                commandLogHeader.setBackgroundColor(resources.getColor(android.R.color.holo_blue_dark))
            } catch (e: Exception) {
                Log.e("FridayTasksActivity", "Error setting fallback color: ${e.message}", e)
            }

            // Expand/collapse logic
            featuresHeader.setOnClickListener {
                featuresContent.visibility = if (featuresContent.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            }
            commandLogHeader.setOnClickListener {
                commandLogContent.visibility = if (commandLogContent.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            }

            // TEMP: Comment out VideoView setup to prevent crash if resource is missing
            // try {
            //     val resId = resources.getIdentifier("circle", "raw", packageName)
            //     if (resId != 0) {
            //         circleVideo.setVideoPath("android.resource://$packageName/$resId")
            //     } else {
            //         Log.w("FridayTasksActivity", "circle.mp4 not found in res/raw. VideoView will not play.")
            //     }
            // } catch (e: Exception) {
            //     Log.e("FridayTasksActivity", "Error setting up VideoView: ${e.message}", e)
            // }

            circleVideo = findViewById(R.id.circleVideo)
            val resId = resources.getIdentifier("circle", "raw", packageName)
            if (resId != 0) {
                circleVideo.setVideoPath("android.resource://$packageName/$resId")
            } else {
                Log.w(TAG, "circle.mp4 not found in res/raw. VideoView will not play.")
            }

            // Double the size of the VideoView
            val params = circleVideo.layoutParams
            params.width = (params.width * 2)
            params.height = (params.height * 2)
            circleVideo.layoutParams = params

            setupRecyclerViews()
            startHeadlessVoiceRecognition()
            tts = TextToSpeech(this) { status ->
                if (status != TextToSpeech.ERROR) {
                    tts.language = Locale.getDefault()
                }
            }
        } catch (e: InflateException) {
            Log.e(TAG, "Inflate error: ", e)
            Toast.makeText(this, "Layout inflation error: ${e.message}", Toast.LENGTH_LONG).show()
            finish() // Close the activity if inflation fails
        } catch (e: Exception) {
             Log.e(TAG, "Other error during onCreate: ", e)
            Toast.makeText(this, "Other error during onCreate: ${(e as java.lang.Exception).message}", Toast.LENGTH_LONG).show()
            finish() // Close the activity on other errors
        }
    }

    private fun setupRecyclerViews() {
        commandLogRecyclerView = findViewById(R.id.commandLogRecyclerView)
        tasksRecyclerView = findViewById(R.id.tasksRecyclerView)

        commandLogRecyclerView.layoutManager = LinearLayoutManager(this)
        tasksRecyclerView.layoutManager = LinearLayoutManager(this)

        commandLogRecyclerView.adapter = CommandLogAdapter(commandLog)
        tasksRecyclerView.adapter = TasksAdapter(tasks) { task ->
            // Handle task click
            executeTask(task)
        }
    }

    private fun startHeadlessVoiceRecognition() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech recognition not available", Toast.LENGTH_SHORT).show()
            return
        }
        if (::speechRecognizer.isInitialized) {
            speechRecognizer.destroy()
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                circleVideo.visibility = View.VISIBLE
                circleVideo.start()
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                circleVideo.visibility = View.GONE
                circleVideo.pause()
                // Suppress toast for error 7 (ERROR_NO_MATCH)
                if (error != SpeechRecognizer.ERROR_NO_MATCH) {
                    Toast.makeText(this@FridayTasksActivity, "Speech recognition error: $error", Toast.LENGTH_SHORT).show()
                }
                Handler(Looper.getMainLooper()).postDelayed({
                    startHeadlessVoiceRecognition()
                }, 1000)
            }
            override fun onResults(results: Bundle?) {
                circleVideo.visibility = View.GONE
                circleVideo.pause()
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val command = matches[0].toLowerCase(Locale.getDefault())
                    commandLog.add(command)
                    commandLogRecyclerView.adapter?.notifyDataSetChanged()
                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            val response = WebhookUtil.sendSpeechInput(command)
                            Log.d("FridayTasksActivity", "Raw n8n response: $response")
                            if (response != null && response.isNotBlank()) {
                                try {
                                    val arr = org.json.JSONArray(response)
                                    val jsonResponse = if (arr.length() > 0) arr.getJSONObject(0) else JSONObject()
                                    val reply = jsonResponse.optString("reply", "")
                                    Log.d("FridayTasksActivity", "TTS reply: $reply")
                                    if (reply.isNotEmpty()) {
                                        tts.speak(reply, TextToSpeech.QUEUE_FLUSH, null, null)
                                    }
                                    val action = jsonResponse.optString("action", "")
                                    val parameters = jsonResponse.optJSONObject("parameters")
                                    when (action) {
                                        "make_call" -> {
                                            val contact = parameters?.optString("contact")
                                            if (contact != null) makePhoneCall(contact)
                                        }
                                        "send_message" -> {
                                            val contact = parameters?.optString("contact")
                                            val message = parameters?.optString("message")
                                            if (contact != null && message != null) sendMessage(contact, message)
                                        }
                                        "set_alarm" -> {
                                            val time = parameters?.optString("time")
                                            if (time != null) setAlarm(time)
                                        }
                                        "open_app" -> {
                                            val appName = parameters?.optString("app_name")
                                            if (appName != null) openApp(appName)
                                        }
                                        "set_timer" -> {
                                            val duration = parameters?.optString("duration")
                                            if (duration != null) setTimer(duration)
                                        }
                                        "take_note" -> {
                                            val content = parameters?.optString("content")
                                            if (content != null) takeNote(content)
                                        }
                                        "get_weather" -> {
                                            val location = parameters?.optString("location") ?: "your location"
                                            val msg = "Getting weather for $location"
                                            speakOrToast(msg)
                                        }
                                        "play_music" -> {
                                            val song = parameters?.optString("song") ?: ""
                                            val artist = parameters?.optString("artist") ?: ""
                                            speakOrToast("Playing $song $artist")
                                            try {
                                                val intent = Intent(android.provider.MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
                                                    putExtra(android.app.SearchManager.QUERY, "$song $artist")
                                                }
                                                startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(this@FridayTasksActivity, "No music app found", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        "navigate" -> {
                                            val destination = parameters?.optString("destination") ?: "your destination"
                                            val msg = "Navigating to $destination"
                                            speakOrToast(msg)
                                        }
                                        "search_web" -> {
                                            val query = parameters?.optString("query") ?: ""
                                            val msg = if (query.isNotEmpty()) "Searching the web for $query" else "Searching the web"
                                            speakOrToast(msg)
                                        }
                                        "turn_on_flashlight" -> {
                                            speakOrToast("Turning on flashlight")
                                            // TODO: Implement flashlight control
                                        }
                                        "turn_off_flashlight" -> {
                                            speakOrToast("Turning off flashlight")
                                            // TODO: Implement flashlight control
                                        }
                                        "get_time" -> {
                                            val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                                            speakOrToast("The time is $time")
                                        }
                                        "get_date" -> {
                                            val date = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date())
                                            speakOrToast("Today's date is $date")
                                        }
                                        "read_notifications" -> {
                                            speakOrToast("Reading notifications")
                                            // TODO: Implement notification reading
                                        }
                                        "control_wifi" -> {
                                            val state = parameters?.optString("on_off") ?: "on"
                                            speakOrToast("Turning WiFi $state")
                                            // TODO: Implement WiFi control
                                        }
                                        "control_bluetooth" -> {
                                            val state = parameters?.optString("on_off") ?: "on"
                                            speakOrToast("Turning Bluetooth $state")
                                            // TODO: Implement Bluetooth control
                                        }
                                        "open_url" -> {
                                            val url = parameters?.optString("url")
                                            if (url != null) {
                                                speakOrToast("Opening $url")
                                                try {
                                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                    startActivity(browserIntent)
                                                } catch (e: Exception) {
                                                    Toast.makeText(this@FridayTasksActivity, "Could not open URL", Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                speakOrToast("No URL provided")
                                            }
                                        }
                                        "set_reminder" -> {
                                            val time = parameters?.optString("time") ?: "sometime"
                                            val content = parameters?.optString("content") ?: ""
                                            val msg = if (content.isNotEmpty()) "Setting reminder at $time: $content" else "Setting reminder at $time"
                                            speakOrToast(msg)
                                            // TODO: Implement reminder
                                        }
                                        "home" -> {
                                            speakOrToast("Going home")
                                            if (FridayAccessibilityService.instance != null) {
                                                FridayAccessibilityService.instance?.performGlobalActionByName("home")
                                            } else {
                                                promptEnableAccessibility()
                                            }
                                        }
                                        "back" -> {
                                            speakOrToast("Going back")
                                            if (FridayAccessibilityService.instance != null) {
                                                FridayAccessibilityService.instance?.performGlobalActionByName("back")
                                            } else {
                                                promptEnableAccessibility()
                                            }
                                        }
                                        "recent" -> {
                                            speakOrToast("Opening recents")
                                            if (FridayAccessibilityService.instance != null) {
                                                FridayAccessibilityService.instance?.performGlobalActionByName("recent")
                                            } else {
                                                promptEnableAccessibility()
                                            }
                                        }
                                        "swipe_right" -> {
                                            speakOrToast("Swiping right")
                                            if (FridayAccessibilityService.instance != null) {
                                                FridayAccessibilityService.instance?.performSwipe("right")
                                            } else {
                                                promptEnableAccessibility()
                                            }
                                        }
                                        "swipe_left" -> {
                                            speakOrToast("Swiping left")
                                            if (FridayAccessibilityService.instance != null) {
                                                FridayAccessibilityService.instance?.performSwipe("left")
                                            } else {
                                                promptEnableAccessibility()
                                            }
                                        }
                                        "scroll_up" -> {
                                            speakOrToast("Scrolling up")
                                            if (FridayAccessibilityService.instance != null) {
                                                FridayAccessibilityService.instance?.performSwipe("up")
                                            } else {
                                                promptEnableAccessibility()
                                            }
                                        }
                                        "scroll_down" -> {
                                            speakOrToast("Scrolling down")
                                            if (FridayAccessibilityService.instance != null) {
                                                FridayAccessibilityService.instance?.performSwipe("down")
                                            } else {
                                                promptEnableAccessibility()
                                            }
                                        }
                                        "close_app" -> {
                                            speakOrToast("Closing app")
                                            if (FridayAccessibilityService.instance != null) {
                                                FridayAccessibilityService.instance?.performGlobalActionByName("close_app")
                                            } else {
                                                promptEnableAccessibility()
                                            }
                                        }
                                        "clear_recent_app" -> {
                                            val appName = parameters?.optString("app_name") ?: ""
                                            speakOrToast("Clearing $appName from recents")
                                            // TODO: Implement clear recent app with accessibility
                                            promptEnableAccessibility()
                                        }
                                        "send_message_button" -> {
                                            speakOrToast("Sending message")
                                            if (FridayAccessibilityService.instance != null) {
                                                FridayAccessibilityService.instance?.clickSendButton()
                                            } else {
                                                promptEnableAccessibility()
                                            }
                                        }
                                        else -> {
                                            speakOrToast("Not supported")
                                            // Do NOT restart listening for unsupported actions
                                            return@launch
                                        }
                                    }
                                    // Only restart listening for supported actions
                                    if (!action.isNullOrEmpty()) {
                                        startHeadlessVoiceRecognition()
                                    }
                                } catch (e: Exception) {
                                    Log.e("FridayTasksActivity", "Parsing error: $response, error: ${e.message}", e)
                                    Toast.makeText(this@FridayTasksActivity,
                                        "Error processing response: ${e.message}",
                                        Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(this@FridayTasksActivity,
                                    "No response from server",
                                    Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Log.e("FridayTasksActivity", "Error sending speech input: ${e.message}", e)
                            Toast.makeText(this@FridayTasksActivity,
                                "Error sending speech input",
                                Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        speechRecognizer.startListening(intent)
    }

    private fun executeTask(task: Task) {
        when (task.title) {
            "Make a Call" -> {
                DialogUtil.showContactInputDialog(this) { contactName ->
                    makePhoneCall(contactName)
                }
            }
            "Send Message" -> {
                DialogUtil.showMessageInputDialog(this) { contactName, message ->
                    sendMessage(contactName, message)
                }
            }
            "Set Alarm" -> {
                DialogUtil.showTimeInputDialog(this) { time ->
                    setAlarm(time)
                }
            }
            "Open App" -> {
                DialogUtil.showAppInputDialog(this) { appName ->
                    openApp(appName)
                }
            }
            "Set Timer" -> {
                DialogUtil.showTimerInputDialog(this) { duration ->
                    setTimer(duration)
                }
            }
            "Take Note" -> {
                DialogUtil.showNoteInputDialog(this) { content ->
                    takeNote(content)
                }
            }
        }
    }

    private fun makePhoneCall() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), CALL_PERMISSION_REQUEST)
        } else {
            DialogUtil.showContactInputDialog(this) { contactName ->
                makePhoneCall(contactName)
            }
        }
    }

    private fun sendMessage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), SMS_PERMISSION_REQUEST)
        } else {
            DialogUtil.showMessageInputDialog(this) { contactName, message ->
                sendMessage(contactName, message)
            }
        }
    }

    private fun setAlarm() {
        DialogUtil.showTimeInputDialog(this) { time ->
            setAlarm(time)
        }
    }

    private fun openApp() {
        DialogUtil.showAppInputDialog(this) { appName ->
            openApp(appName)
        }
    }

    private fun setTimer() {
        DialogUtil.showTimerInputDialog(this) { duration ->
            setTimer(duration)
        }
    }

    private fun takeNote() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSION_REQUEST)
        } else {
            DialogUtil.showNoteInputDialog(this) { content ->
                takeNote(content)
            }
        }
    }

    private fun makePhoneCall(contactName: String) {
        if (contactName.isNotEmpty()) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), CALL_PERMISSION_REQUEST)
            } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CONTACTS), READ_CONTACTS_PERMISSION_REQUEST)
                Toast.makeText(this, "Please grant contacts permission and try again.", Toast.LENGTH_LONG).show()
                return
            } else {
                val phoneNumber = getPhoneNumberForContact(contactName)
                if (phoneNumber != null) {
                    try {
                        val intent = Intent(Intent.ACTION_CALL)
                        intent.data = Uri.parse("tel:" + phoneNumber)
                        startActivity(intent)
                        Toast.makeText(this, "Calling $contactName", Toast.LENGTH_SHORT).show()
                    } catch (e: SecurityException) {
                        Toast.makeText(this, "Call permission denied or error", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this, "Could not initiate call: "+e.message, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Contact '$contactName' not found", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "Please specify a contact to call", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendMessage(contactName: String, message: String) {
        if (contactName.isNotEmpty() && message.isNotEmpty()) {
            val phoneNumber = getPhoneNumberForContact(contactName)
            if (phoneNumber != null) {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("smsto:" + phoneNumber)
                    putExtra("sms_body", message)
                }
                try {
                    startActivity(intent)
                    Toast.makeText(this, "Message sent to $contactName", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Could not open messaging app: "+e.message, Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Contact '$contactName' not found", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Please specify contact and message", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getPhoneNumberForContact(contactName: String): String? {
        var phoneNumber: String? = null
        val resolver = contentResolver
        val cursor = resolver.query(
            android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER, android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME),
            "${android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
            arrayOf("%$contactName%"),
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                phoneNumber = it.getString(it.getColumnIndexOrThrow(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER))
                val foundName = it.getString(it.getColumnIndexOrThrow(android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                Log.d("FridayTasksActivity", "Matched contact: $foundName, number: $phoneNumber")
            }
        }
        return phoneNumber?.replace("[^0-9]+".toRegex(), "") // Clean up the number
    }

    private fun setAlarm(time: String) {
        if (time.isNotEmpty()) {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_MESSAGE, "Friday Alarm") // Default message
                try {
                    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                    val date = timeFormat.parse(time)
                    if (date != null) {
                        val calendar = Calendar.getInstance()
                        calendar.time = date
                        putExtra(AlarmClock.EXTRA_HOUR, calendar.get(Calendar.HOUR_OF_DAY))
                        putExtra(AlarmClock.EXTRA_MINUTES, calendar.get(Calendar.MINUTE))
                        putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                    } else {
                        val timeParts = time.split(":")
                        if (timeParts.size == 2) {
                            putExtra(AlarmClock.EXTRA_HOUR, timeParts[0].toInt())
                            putExtra(AlarmClock.EXTRA_MINUTES, timeParts[1].toInt())
                            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                        } else {
                            Toast.makeText(this@FridayTasksActivity, "Invalid time format", Toast.LENGTH_SHORT).show()
                            return
                        }
                    }
                } catch (e: NumberFormatException) {
                    Toast.makeText(this@FridayTasksActivity, "Invalid time format for alarm", Toast.LENGTH_SHORT).show()
                    return
                } catch (e: Exception) {
                    Toast.makeText(this@FridayTasksActivity, "Could not set alarm: "+e.message, Toast.LENGTH_SHORT).show()
                    return
                }
            }
            try {
                startActivity(intent)
                Toast.makeText(this, "Alarm set for $time", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@FridayTasksActivity, "Could not open alarm app: "+e.message, Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Please specify a time for the alarm", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openApp(appName: String) {
        val packageManager = packageManager
        val intent = packageManager.getLaunchIntentForPackage(appName)
        if (intent != null) {
            try {
                startActivity(intent)
                Toast.makeText(this, "Opened $appName", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Could not open app: "+e.message, Toast.LENGTH_SHORT).show()
            }
        } else {
            val launchIntent: Intent? = packageManager.getInstalledApplications(0)
                .asSequence()
                .filter { it.enabled }
                .firstOrNull { appInfo ->
                    val label = packageManager.getApplicationLabel(appInfo).toString()
                    label.equals(appName, ignoreCase = true)
                }?.let { appInfo ->
                    packageManager.getLaunchIntentForPackage(appInfo.packageName)
                }

            if (launchIntent != null) {
                try {
                    startActivity(launchIntent)
                    Toast.makeText(this, "Opened $appName", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Could not open app: "+e.message, Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "App '$appName' not found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setTimer(duration: String) {
        val durationInSeconds = parseDuration(duration)
        if (durationInSeconds > 0) {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, durationInSeconds)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            }
            try {
                startActivity(intent)
                Toast.makeText(this, "Timer set for $duration", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@FridayTasksActivity, "Could not open timer app: "+e.message, Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Could not understand the timer duration", Toast.LENGTH_SHORT).show()
        }
    }

    private fun parseDuration(duration: String): Int {
        var totalSeconds = 0
        val parts = duration.split(" ")
        if (parts.size >= 2) {
            try {
                val value = parts[0].toInt()
                val unit = parts[1].toLowerCase(Locale.getDefault())
                when (unit) {
                    "seconds", "second" -> totalSeconds = value
                    "minutes", "minute" -> totalSeconds = value * 60
                    "hours", "hour" -> totalSeconds = value * 3600
                }
            } catch (e: NumberFormatException) {
                // Ignore, return 0
            }
        }
        return totalSeconds
    }

    private fun takeNote(noteContent: String) {
        if (noteContent.isNotEmpty()) {
            try {
                val fridayNotesDir = File(getExternalFilesDir(null), "FridayNotes")
                if (!fridayNotesDir.exists()) {
                    fridayNotesDir.mkdirs()
                }
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val noteFile = File(fridayNotesDir, "note_$timestamp.txt")
                noteFile.writeText(noteContent)
                Toast.makeText(this, "Note saved", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Could not save note: "+e.message, Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Please provide content for the note", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == READ_CONTACTS_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Contacts permission granted. Please try your command again.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Contacts permission denied. Cannot access contacts.", Toast.LENGTH_SHORT).show()
            }
        }
        // ... handle other permissions as needed ...
    }

    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }

    private fun speakOrToast(text: String) {
        Log.d(TAG, "speakOrToast: $text")
        val result = tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        if (result == TextToSpeech.ERROR) {
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
        }
    }

    // Add this helper function to prompt enabling accessibility
    private fun promptEnableAccessibility() {
        Toast.makeText(this, "Please enable Friday Accessibility Service", Toast.LENGTH_LONG).show()
        val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }

    companion object {
        private const val VOICE_RECOGNITION_REQUEST_CODE = 1
        private const val CALL_PERMISSION_REQUEST = 2
        private const val SMS_PERMISSION_REQUEST = 3
        private const val STORAGE_PERMISSION_REQUEST = 4
        private const val READ_CONTACTS_PERMISSION_REQUEST = 5
        private const val TAG = "FridayTasksActivity"
    }
}

data class Task(
    val title: String,
    val iconResId: Int,
    val commandExample: String
) 