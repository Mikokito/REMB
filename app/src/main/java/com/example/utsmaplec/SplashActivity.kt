package com.example.utsmaplec

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.example.utsmaplec.StartActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Animasi saat masuk
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)

        Handler().postDelayed({
            val intent = Intent(
                this@SplashActivity,
                StartActivity::class.java
            )
            startActivity(intent)
            finish()

            // Animasi saat keluar
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }, 3000)
    }

}