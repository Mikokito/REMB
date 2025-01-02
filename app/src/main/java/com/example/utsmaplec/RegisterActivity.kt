package com.example.utsmaplec

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {
    private lateinit var editTextName: EditText
    private lateinit var editTextEmail: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var editTextConfirmPassword: EditText
    private lateinit var editTextNumber: EditText
    private lateinit var buttonRegister: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize views
        editTextName = findViewById(R.id.editTextName)
        editTextEmail = findViewById(R.id.editTextEmail)
        editTextPassword = findViewById(R.id.editTextPassword)
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword)
        editTextNumber = findViewById(R.id.editTextNumber)
        buttonRegister = findViewById(R.id.buttonRegister)

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Set onClick listener for the register button
        buttonRegister.setOnClickListener { registerUser() }
    }

    private fun registerUser() {
        val name = editTextName.text.toString().trim()
        val email = editTextEmail.text.toString().trim()
        val password = editTextPassword.text.toString().trim()
        val confirmPassword = editTextConfirmPassword.text.toString().trim()
        val phoneNumber = editTextNumber.text.toString().trim()

        // Check if name, email, and passwords are not empty
        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || phoneNumber.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if password and confirm password match
        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        // Register user with Firebase Authentication
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Get the UID of the newly registered user
                    val uid = auth.currentUser!!.uid

                    // Create a map to store user data
                    val userData = hashMapOf(
                        "name" to name,
                        "email" to email,
                        "phoneNumber" to phoneNumber,
                        "created_at" to System.currentTimeMillis()
                    )

                    // Save user data to Firestore
                    db.collection("users").document(uid)
                        .set(userData)
                        .addOnSuccessListener {
                            // Registration and Firestore save successful
                            Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()

                            // Redirect to LoginActivity after successful registration
                            val intent = Intent(this, LoginActivity::class.java)
                            startActivity(intent)
                            finish() // Close the RegisterActivity to prevent going back to it
                        }
                        .addOnFailureListener { e ->
                            // Failed to save user data
                            Toast.makeText(this, "Failed to save user data: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    // Registration failed
                    Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
