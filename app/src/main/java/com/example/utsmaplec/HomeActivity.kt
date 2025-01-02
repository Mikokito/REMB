package com.example.utsmaplec

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.utsmaplec.model.Task
import android.widget.CalendarView
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log
import android.widget.SearchView

class HomeActivity : AppCompatActivity() {

    private val taskList = mutableListOf<Task>()
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Initialize FirebaseAuth and Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize components
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view_tasks)
        val searchView = findViewById<SearchView>(R.id.searchView)

        // Setup RecyclerView
        taskAdapter = TaskAdapter(taskList) { task -> onTaskChecked(task) }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = taskAdapter

        // Load tasks from Firestore
        loadTasksFromFirestore()

        // Setup Bottom Navigation
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Stay on Home
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
                    startActivity(Intent(this, HistoryActivity::class.java))
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
        bottomNavigationView.selectedItemId = R.id.nav_home

        // Setup search functionality
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterTasks(newText)
                return true
            }
        })
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

    private fun loadTasksFromFirestore() {
        val user = auth.currentUser
        user?.let {
            val userId = user.uid

            Log.d("HomeActivity", "Current user ID: $userId")

            // Reference to the user's tasks collection
            db.collection("users").document(userId).collection("tasks")
                .get()
                .addOnSuccessListener { documents ->
                    taskList.clear() // Clear the existing list
                    for (document in documents) {
                        val task = document.toObject(Task::class.java)
                        if (task != null && !task.completed) { // Only add tasks that are not completed
                            taskList.add(task) // Add task to the list
                        } else {
                            Log.e("HomeActivity", "Task is null or completed for document: ${document.id}")
                        }
                    }
                    taskAdapter.notifyDataSetChanged() // Notify the adapter to refresh the RecyclerView
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error loading tasks: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun onTaskChecked(task: Task) {
        // Remove the task from the current list
        taskList.remove(task)
        taskAdapter.notifyDataSetChanged() // Refresh the RecyclerView

        // Move the task to history
        moveTaskToHistory(task)
    }

    private fun moveTaskToHistory(task: Task) {
        val user = auth.currentUser
        user?.let {
            val userId = user.uid

            // Create a task document reference in the history collection
            val historyRef = db.collection("users").document(userId).collection("history").document()

            // Save the task with completed status
            val completedTask = task.copy(completed = true) // Mark the task as completed
            historyRef.set(completedTask)
                .addOnSuccessListener {
                    // Now delete the task from the tasks collection
                    deleteTaskFromFirestore(task)
                    Toast.makeText(this, "Task moved to history", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error moving task: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun deleteTaskFromFirestore(task: Task) {
        val user = auth.currentUser
        user?.let {
            val userId = user.uid

            // Reference to the task document
            val taskRef = db.collection("users").document(userId).collection("tasks").document(task.id) // Assuming task has an id field

            // Delete the task from Firestore
            taskRef.delete()
                .addOnSuccessListener {
                    Log.d("HomeActivity", "Task successfully deleted from Firestore")
                }
                .addOnFailureListener { e ->
                    Log.e("HomeActivity", "Error deleting task: ${e.message}")
                }
        }
    }

    private fun filterTasks(query: String?) {
        val filteredList = taskList.filter { task ->
            task.title.contains(query ?: "", ignoreCase = true) || 
            task.description.contains(query ?: "", ignoreCase = true)
        }
        taskAdapter.updateList(filteredList) // Update the adapter with the filtered list
    }
}