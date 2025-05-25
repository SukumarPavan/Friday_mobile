package com.example.friday

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast

object DialogUtil {
    fun showContactInputDialog(context: Context, onContactSelected: (String) -> Unit) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_contact_input, null)
        val contactInput = dialogView.findViewById<EditText>(R.id.contactInput)

        AlertDialog.Builder(context)
            .setTitle("Enter Contact Name")
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                val contactName = contactInput.text.toString()
                if (contactName.isNotEmpty()) {
                    onContactSelected(contactName)
                } else {
                    Toast.makeText(context, "Please enter a contact name", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun showMessageInputDialog(context: Context, onMessageReady: (String, String) -> Unit) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_message_input, null)
        val contactInput = dialogView.findViewById<EditText>(R.id.contactInput)
        val messageInput = dialogView.findViewById<EditText>(R.id.messageInput)

        AlertDialog.Builder(context)
            .setTitle("Send Message")
            .setView(dialogView)
            .setPositiveButton("Send") { _, _ ->
                val contactName = contactInput.text.toString()
                val message = messageInput.text.toString()
                if (contactName.isNotEmpty() && message.isNotEmpty()) {
                    onMessageReady(contactName, message)
                } else {
                    Toast.makeText(context, "Please enter both contact and message", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun showTimeInputDialog(context: Context, onTimeSelected: (String) -> Unit) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_time_input, null)
        val timeInput = dialogView.findViewById<EditText>(R.id.timeInput)

        AlertDialog.Builder(context)
            .setTitle("Set Alarm Time")
            .setView(dialogView)
            .setPositiveButton("Set") { _, _ ->
                val time = timeInput.text.toString()
                if (time.isNotEmpty()) {
                    onTimeSelected(time)
                } else {
                    Toast.makeText(context, "Please enter a time", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun showAppInputDialog(context: Context, onAppSelected: (String) -> Unit) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_app_input, null)
        val appInput = dialogView.findViewById<EditText>(R.id.appInput)

        AlertDialog.Builder(context)
            .setTitle("Open App")
            .setView(dialogView)
            .setPositiveButton("Open") { _, _ ->
                val appName = appInput.text.toString()
                if (appName.isNotEmpty()) {
                    onAppSelected(appName)
                } else {
                    Toast.makeText(context, "Please enter an app name", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun showTimerInputDialog(context: Context, onDurationSelected: (String) -> Unit) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_timer_input, null)
        val durationInput = dialogView.findViewById<EditText>(R.id.durationInput)

        AlertDialog.Builder(context)
            .setTitle("Set Timer")
            .setView(dialogView)
            .setPositiveButton("Set") { _, _ ->
                val duration = durationInput.text.toString()
                if (duration.isNotEmpty()) {
                    onDurationSelected(duration)
                } else {
                    Toast.makeText(context, "Please enter a duration", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun showNoteInputDialog(context: Context, onNoteReady: (String) -> Unit) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_note_input, null)
        val noteInput = dialogView.findViewById<EditText>(R.id.noteInput)

        AlertDialog.Builder(context)
            .setTitle("Take Note")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val noteContent = noteInput.text.toString()
                if (noteContent.isNotEmpty()) {
                    onNoteReady(noteContent)
                } else {
                    Toast.makeText(context, "Please enter note content", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
} 