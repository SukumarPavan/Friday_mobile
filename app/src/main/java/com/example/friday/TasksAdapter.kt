package com.example.friday

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TasksAdapter(
    private val tasks: List<Task>,
    private val onTaskClick: (Task) -> Unit
) : RecyclerView.Adapter<TasksAdapter.TaskViewHolder>() {

    class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val taskIcon: ImageView = view.findViewById(R.id.taskIcon)
        val taskTitle: TextView = view.findViewById(R.id.taskTitle)
        val commandExample: TextView = view.findViewById(R.id.commandExample)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.taskIcon.setImageResource(task.iconResId)
        holder.taskTitle.text = task.title
        holder.commandExample.text = task.commandExample
        
        holder.itemView.setOnClickListener {
            onTaskClick(task)
        }
    }

    override fun getItemCount() = tasks.size
} 