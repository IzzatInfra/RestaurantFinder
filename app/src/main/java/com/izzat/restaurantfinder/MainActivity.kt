package com.izzat.restaurantfinder

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.Executor

class MainActivity : AppCompatActivity() {

    private lateinit var btnFindRestaurant: Button
    private lateinit var btnSchedule: Button
    private lateinit var btnProfile: Button
    private lateinit var btnLogout: Button
    private lateinit var textUserEmail: TextView

    private var backPressedTime: Long = 0
    private lateinit var backToast: Toast

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 🔐 Check fingerprint setting
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        if (prefs.getBoolean("fingerprint_enabled", false)) {
            authenticateWithBiometrics()
        }

        btnFindRestaurant = findViewById(R.id.btnFindRestaurant)
        btnSchedule = findViewById(R.id.btnSchedule)
        btnProfile = findViewById(R.id.btnProfile)
        btnLogout = findViewById(R.id.btnLogout)
        textUserEmail = findViewById(R.id.textUserEmail)

        val currentUser = FirebaseAuth.getInstance().currentUser
        textUserEmail.text = currentUser?.email ?: "Welcome!"

        btnFindRestaurant.setOnClickListener {
            startActivity(Intent(this, FindRestaurantActivity::class.java))
        }

        btnSchedule.setOnClickListener {
            startActivity(Intent(this, ScheduleActivity::class.java))
        }

        btnProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // 🔁 Handle double back to exit
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (System.currentTimeMillis() - backPressedTime < 2000) {
                    backToast.cancel()
                    finish()
                } else {
                    backToast = Toast.makeText(this@MainActivity, "Press BACK again to exit", Toast.LENGTH_SHORT)
                    backToast.show()
                    backPressedTime = System.currentTimeMillis()
                }
            }
        })
    }

    private fun authenticateWithBiometrics() {
        val biometricManager = BiometricManager.from(this)
        if (biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            ) == BiometricManager.BIOMETRIC_SUCCESS
        ) {
            val executor: Executor = ContextCompat.getMainExecutor(this)
            val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(applicationContext, "Authentication successful", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            })

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Fingerprint Authentication")
                .setSubtitle("Log in using your fingerprint")
                .setNegativeButtonText("Cancel")
                .build()

            biometricPrompt.authenticate(promptInfo)
        } else {
            Toast.makeText(this, "Biometric not supported or not enrolled", Toast.LENGTH_SHORT).show()
        }
    }
}
