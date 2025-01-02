package com.example.utsmaplec

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Check if user is logged in
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // User is signed in, redirect to HomeActivity
            Handler().postDelayed({
                val intent = Intent(this, HomeActivity::class.java)
                startActivity(intent)
                finish()
            }, 3000) // Delay for splash screen
        } else {
            // User is not signed in, redirect to StartActivity
            Handler().postDelayed({
                val intent = Intent(this, StartActivity::class.java)
                startActivity(intent)
                finish()
            }, 3000) // Delay for splash screen
        }
    }
}