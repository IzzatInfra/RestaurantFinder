package com.izzat.restaurantfinder

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private lateinit var editFirstName: EditText
    private lateinit var editLastName: EditText
    private lateinit var editAddress: EditText
    private lateinit var editMobile: EditText
    private lateinit var btnSave: Button
    private lateinit var btnLogout: Button
    private lateinit var switchFingerprint: SwitchCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("users")

        val user = auth.currentUser ?: return
        val uid = user.uid

        editFirstName = findViewById(R.id.editFirstName)
        editLastName = findViewById(R.id.editLastName)
        editAddress = findViewById(R.id.editAddress)
        editMobile = findViewById(R.id.editMobile)
        btnSave = findViewById(R.id.btnSave)
        btnLogout = findViewById(R.id.btnLogout)
        switchFingerprint = findViewById(R.id.switchFingerprint)

        // Load user data from Firebase
        database.child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                editFirstName.setText(snapshot.child("firstName").getValue(String::class.java) ?: "")
                editLastName.setText(snapshot.child("lastName").getValue(String::class.java) ?: "")
                editAddress.setText(snapshot.child("address").getValue(String::class.java) ?: "")
                editMobile.setText(snapshot.child("mobile").getValue(String::class.java) ?: "")
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ProfileActivity, "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
        })

        // Load fingerprint preference
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        switchFingerprint.isChecked = prefs.getBoolean("fingerprint_enabled", false)

        switchFingerprint.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("fingerprint_enabled", isChecked).apply()
        }

        btnSave.setOnClickListener {
            val user = auth.currentUser
            val uid = user?.uid ?: return@setOnClickListener

            val updates = mapOf(
                "firstName" to editFirstName.text.toString().trim(),
                "lastName" to editLastName.text.toString().trim(),
                "address" to editAddress.text.toString().trim(),
                "mobile" to editMobile.text.toString().trim()
            )

            database.child(uid).setValue(updates).addOnSuccessListener {
                Toast.makeText(this, "Profile created/updated", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to update: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }

        btnLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        val btnAbout = findViewById<Button>(R.id.btnAbout)
        btnAbout.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }
    }
}
