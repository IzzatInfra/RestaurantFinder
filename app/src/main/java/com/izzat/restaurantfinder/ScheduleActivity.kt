package com.izzat.restaurantfinder

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*

class ScheduleActivity : AppCompatActivity() {

    private lateinit var textSelectedDate: TextView
    private lateinit var btnPickDate: Button
    private lateinit var btnConfirmSchedule: Button
    private lateinit var spinnerRestaurants: Spinner
    private lateinit var layoutScheduleList: LinearLayout

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private var selectedDate: String = ""
    private var selectedRestaurant: String = ""
    private val MAX_SCHEDULE = 20

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule)

        textSelectedDate = findViewById(R.id.textSelectedDate)
        btnPickDate = findViewById(R.id.btnPickDate)
        btnConfirmSchedule = findViewById(R.id.btnConfirmSchedule)
        spinnerRestaurants = findViewById(R.id.spinnerRestaurants)
        layoutScheduleList = findViewById(R.id.layoutScheduleList)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        val uid = auth.currentUser?.uid ?: return

        // Extract restaurant data from intent
        val restaurantList = intent.getStringArrayListExtra("restaurantList") ?: arrayListOf()


        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, restaurantList)
        spinnerRestaurants.adapter = adapter

        spinnerRestaurants.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedRestaurant = parent.getItemAtPosition(position).toString()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        btnPickDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(this, { _, y, m, d ->
                selectedDate = "${d}/${m + 1}/${y}"
                textSelectedDate.text = selectedDate
            }, year, month, day)

            datePickerDialog.show()
        }

        btnConfirmSchedule.setOnClickListener {
            if (selectedDate.isBlank()) {
                Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedRestaurant.isBlank()) {
                Toast.makeText(this, "Please select a restaurant", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val scheduleRef = database.child("schedules").child(uid)

            scheduleRef.get().addOnSuccessListener { snapshot ->
                if (snapshot.childrenCount >= MAX_SCHEDULE) {
                    Toast.makeText(this, "Limit reached (Max $MAX_SCHEDULE)", Toast.LENGTH_SHORT).show()
                } else {
                    val newSchedule = mapOf(
                        "date" to selectedDate,
                        "restaurant" to selectedRestaurant
                    )
                    scheduleRef.push().setValue(newSchedule).addOnSuccessListener {
                        Toast.makeText(this, "Scheduled", Toast.LENGTH_SHORT).show()
                        selectedDate = ""
                        textSelectedDate.text = "No date selected"
                        loadSchedules()
                    }
                }
            }
        }

        loadSchedules()
    }

    private fun loadSchedules() {
        layoutScheduleList.removeAllViews()
        val uid = auth.currentUser?.uid ?: return
        val scheduleRef = database.child("schedules").child(uid)

        scheduleRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val scheduleList = mutableListOf<Triple<String, String, String>>() // date, restaurant, key

                for (child in snapshot.children) {
                    val date = child.child("date").value?.toString() ?: continue
                    val restaurant = child.child("restaurant").value?.toString() ?: "Unknown"
                    val key = child.key ?: continue
                    scheduleList.add(Triple(date, restaurant, key))
                }

                // Sort by date
                val sdf = java.text.SimpleDateFormat("d/M/yyyy", Locale.getDefault())
                val sortedList = scheduleList.sortedBy {
                    try { sdf.parse(it.first) } catch (e: Exception) { Date(Long.MAX_VALUE) }
                }

                for ((date, restaurant, key) in sortedList) {
                    val item = TextView(this@ScheduleActivity)
                    item.text = "$restaurant on $date"
                    item.textSize = 16f
                    item.setPadding(16, 16, 16, 16)

                    val deleteBtn = Button(this@ScheduleActivity)
                    deleteBtn.text = "Delete"
                    deleteBtn.setOnClickListener {
                        scheduleRef.child(key).removeValue().addOnSuccessListener {
                            Toast.makeText(applicationContext, "Deleted", Toast.LENGTH_SHORT).show()
                            loadSchedules()
                        }
                    }

                    val container = LinearLayout(this@ScheduleActivity)
                    container.orientation = LinearLayout.HORIZONTAL
                    container.addView(item, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                    container.addView(deleteBtn)

                    layoutScheduleList.addView(container)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(applicationContext, "Failed to load schedule", Toast.LENGTH_SHORT).show()
            }
        })
    }

}
