package com.example.friday

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CommandLogAdapter(private val commands: List<String>) : 
    RecyclerView.Adapter<CommandLogAdapter.CommandViewHolder>() {

    class CommandViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val commandText: TextView = view.findViewById(R.id.commandText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommandViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_command_log, parent, false)
        return CommandViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommandViewHolder, position: Int) {
        holder.commandText.text = commands[position]
    }

    override fun getItemCount() = commands.size
} 