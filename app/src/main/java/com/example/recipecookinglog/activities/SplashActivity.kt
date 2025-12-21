package com.example.recipecookinglog.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.example.recipecookinglog.R
import com.example.recipecookinglog.databinding.ActivitySplashBinding
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val splashDelay: Long = 2500 // 2.5 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Animate logo
        animateLogo()

        // Navigate after delay
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToNextScreen()
        }, splashDelay)
    }

    private fun animateLogo() {
        // Scale animation for logo
        val scaleAnimation = AnimationUtils.loadAnimation(this, R.anim.splash_scale)
        binding.cardLogo.startAnimation(scaleAnimation)

        // Fade in animation for app name
        val fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.splash_fade_in)
        binding.tvAppName.startAnimation(fadeInAnimation)
    }

    private fun navigateToNextScreen() {
        // Check if user is already logged in
        val currentUser = FirebaseAuth.getInstance().currentUser

        val intent = if (currentUser != null) {
            // User is logged in, go to MainActivity
            Intent(this, MainActivity::class.java)
        } else {
            // User is not logged in, go to LoginActivity
            Intent(this, LoginActivity::class.java)
        }

        startActivity(intent)
        finish()

        // Add transition animation
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}