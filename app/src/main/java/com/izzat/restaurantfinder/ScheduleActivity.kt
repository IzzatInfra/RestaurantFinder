package com.izzat.restaurantfinder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.database.FirebaseDatabase
import java.util.*

class ScheduleActivity : AppCompatActivity() {

    private lateinit var btnPickDate: Button
    private lateinit var btnConfirmSchedule: Button
    private lateinit var textSelectedDate: TextView

    private var selectedDateInMillis: Long = 0L
    private var latitude = 0.0
    private var longitude = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule)

        btnPickDate = findViewById(R.id.btnPickDate)
        btnConfirmSchedule = findViewById(R.id.btnConfirmSchedule)
        textSelectedDate = findViewById(R.id.textSelectedDate)

        latitude = intent.getDoubleExtra("lat", 0.0)
        longitude = intent.getDoubleExtra("lng", 0.0)

        btnPickDate.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select date")
                .build()

            picker.show(supportFragmentManager, picker.toString())
            picker.addOnPositiveButtonClickListener { selection ->
                selectedDateInMillis = selection
                textSelectedDate.text = "Scheduled for: ${Date(selection)}"
            }
        }

        btnConfirmSchedule.setOnClickListener {
            if (selectedDateInMillis != 0L) {
                saveScheduleToFirebase()
                setReminderNotification()
            } else {
                Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveScheduleToFirebase() {
        val dbRef = FirebaseDatabase.getInstance().getReference("schedules")
        val id = dbRef.push().key ?: return
        val schedule = mapOf(
            "id" to id,
            "timestamp" to selectedDateInMillis,
            "lat" to latitude,
            "lng" to longitude
        )
        dbRef.child(id).setValue(schedule)
        Toast.makeText(this, "Schedule saved", Toast.LENGTH_SHORT).show()
    }

    private fun setReminderNotification() {
        val intent = Intent(this, ScheduleNotificationReceiver::class.java).apply {
            putExtra("message", "Time to visit your scheduled restaurant!")
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            selectedDateInMillis,
            pendingIntent
        )
        Toast.makeText(this, "Notification scheduled", Toast.LENGTH_SHORT).show()
    }
}
