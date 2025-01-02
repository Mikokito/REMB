package com.example.utsmaplec

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.CalendarView
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.utsmaplec.model.Task
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HistoryActivity : AppCompatActivity() {

    private val taskList = mutableListOf<Task>()
    private val historyTaskList = mutableListOf<Task>()
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        // Initialize FirebaseAuth and Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize components
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Setup Bottom Navigation
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    true
                }
                R.id.nav_calendar -> {
                    startActivity(Intent(this, CalendarActivity::class.java))
                    true
                }
                R.id.nav_add -> {
                    // Show dialog to add Task
                    showAddTaskDialog()
                    true
                }
                R.id.nav_history -> {
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }

        // Set default menu item selected
        bottomNavigationView.selectedItemId = R.id.nav_history

        // Setup RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view_history_tasks)
        taskAdapter = TaskAdapter(historyTaskList) { task -> /* Handle task checked if needed */ }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = taskAdapter

        // Load completed tasks from Firestore
        loadCompletedTasks()
    }

    private fun showAddTaskDialog() {
        // Inflate layout dialog_add_task
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // Initialize components in the dialog
        val editTaskTitle = dialogView.findViewById<EditText>(R.id.edit_task_title)
        val editTaskDescription = dialogView.findViewById<EditText>(R.id.edit_task_description)
        val calendarView = dialogView.findViewById<CalendarView>(R.id.calendarView)
        val btnSubmitTask = dialogView.findViewById<Button>(R.id.btn_submit_task)

        // Set the default date to today
        val calendar = Calendar.getInstance()
        var selectedDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(calendar.time)

        // Set a listener for date changes
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedDate = String.format("%02d-%02d-%d", dayOfMonth, month + 1, year)
        }

        btnSubmitTask.setOnClickListener {
            val title = editTaskTitle.text.toString()
            val description = editTaskDescription.text.toString()

            if (title.isNotEmpty() && description.isNotEmpty()) { // Ensure both title and description are not empty
                // Create task object with the selected date
                val task = Task(title = title, description = description, date = selectedDate)

                // Save task to Firestore
                saveTaskToFirestore(task)

                // Add task to local list and update UI
                taskList.add(task)
                taskAdapter.notifyItemInserted(taskList.size - 1)
                dialog.dismiss() // Close dialog
            } else {
                if (title.isEmpty()) {
                    editTaskTitle.error = "Task title cannot be empty"
                }
                if (description.isEmpty()) {
                    editTaskDescription.error = "Task description cannot be empty"
                }
            }
        }

        // Show dialog
        dialog.show()
    }

    private fun saveTaskToFirestore(task: Task) {
        // Get current user's UID
        val user = auth.currentUser
        user?.let {
            val userId = user.uid

            // Create a task document reference
            val taskRef = db.collection("users").document(userId).collection("tasks").document()

            // Save task to Firestore with the generated ID
            taskRef.set(task.copy(id = taskRef.id)) // Set the ID to the document ID
                .addOnSuccessListener {
                    Toast.makeText(this, "Task added successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error adding task: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun loadCompletedTasks() {
        val user = auth.currentUser
        user?.let {
            val userId = user.uid

            // Reference to the user's history collection
            db.collection("users").document(userId).collection("history")
                .get()
                .addOnSuccessListener { documents ->
                    historyTaskList.clear() // Clear the existing list
                    for (document in documents) {
                        val task = document.toObject(Task::class.java)
                        historyTaskList.add(task) // Add task to the list
                    }
                    taskAdapter.notifyDataSetChanged() // Notify the adapter to refresh the RecyclerView
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error loading history: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
