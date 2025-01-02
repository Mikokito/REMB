package com.example.utsmaplec

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.widget.CalendarView
import android.widget.CalendarView.OnDateChangeListener
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.utsmaplec.model.Task
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import java.util.Calendar
import java.util.Locale
import java.text.SimpleDateFormat

class CalendarActivity : AppCompatActivity() {

    // Variables for text view and calendar view
    lateinit var calendarView: CalendarView
    private lateinit var taskAdapter: TaskAdapter
    private val taskList = mutableListOf<Task>()
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_calendar)

        // Initialize FirebaseAuth and Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize variables
        calendarView = findViewById(R.id.calendarView)
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view_todaytasks)

        // Setup RecyclerView
        taskAdapter = TaskAdapter(taskList) { task -> onTaskChecked(task) }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = taskAdapter

        // Set on date change listener for calendar view
        calendarView.setOnDateChangeListener(OnDateChangeListener { view, year, month, dayOfMonth ->
            // Format the selected date
            val selectedDate = String.format("%02d-%02d-%d", dayOfMonth, month + 1, year)
            loadTasksForDate(selectedDate) // Load tasks for the selected date
        })

        // Bottom Navigation View setup
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Move to HomeActivity
                    val intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_calendar -> {
                    // Already in CalendarActivity, no need to start again
                    true
                }
                R.id.nav_add -> {
                    // Show dialog to add Task
                    showAddTaskDialog()
                    true
                }
                R.id.nav_profile -> {
                    // Move to ProfileActivity
                    val intent = Intent(this, ProfileActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_history -> {
                    // Move to HistoryActivity
                    val intent = Intent(this, HistoryActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }

        // Set default selected item to calendar
        bottomNavigationView.selectedItemId = R.id.nav_calendar
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

    private fun loadTasksForDate(selectedDate: String) {
        val user = auth.currentUser
        user?.let {
            val userId = user.uid

            // Reference to the user's tasks collection
            db.collection("users").document(userId).collection("tasks")
                .get()
                .addOnSuccessListener { documents ->
                    taskList.clear() // Clear the existing list
                    Log.d("CalendarActivity", "Loading tasks for date: $selectedDate")
                    for (document in documents) {
                        val task = document.toObject(Task::class.java)
                        Log.d("CalendarActivity", "Task loaded: ${task?.title}, Date: ${task?.date}, Completed: ${task?.completed}")
                        if (task != null && task.date == selectedDate && !task.completed) { // Only add tasks that are not completed
                            taskList.add(task) // Add task to the list if the date matches and not completed
                        }
                    }
                    Log.d("CalendarActivity", "Total tasks for date $selectedDate: ${taskList.size}")
                    taskAdapter.notifyDataSetChanged() // Notify the adapter to refresh the RecyclerView
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error loading tasks: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun onTaskChecked(task: Task) {
        Log.d("CalendarActivity", "Task checked: ${task.title}")
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
                    Log.d("CalendarActivity", "Task successfully deleted from Firestore")
                }
                .addOnFailureListener { e ->
                    Log.e("CalendarActivity", "Error deleting task: ${e.message}")
                }
        }
    }
}
