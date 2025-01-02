package com.example.utsmaplec

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.CheckBox
import androidx.recyclerview.widget.RecyclerView
import com.example.utsmaplec.model.Task

class TaskAdapter(private var taskList: List<Task>, private val onTaskChecked: (Task) -> Unit) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val taskTitle: TextView = itemView.findViewById(R.id.task_title)
        val taskDescription: TextView = itemView.findViewById(R.id.task_description)
        val taskDate: TextView = itemView.findViewById(R.id.task_date)
        val checkBoxDelete: CheckBox = itemView.findViewById(R.id.checkBoxDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = taskList[position]
        holder.taskTitle.text = task.title
        holder.taskDescription.text = task.description
        holder.taskDate.text = task.date
        holder.checkBoxDelete.isChecked = task.completed

        holder.checkBoxDelete.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                onTaskChecked(task)
            }
        }
    }

    override fun getItemCount(): Int = taskList.size

    fun updateList(newList: List<Task>) {
        taskList = newList
        notifyDataSetChanged() // Notify the adapter to refresh the RecyclerView
    }
}
