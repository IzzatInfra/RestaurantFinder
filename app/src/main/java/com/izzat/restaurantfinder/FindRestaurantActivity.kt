package com.izzat.restaurantfinder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.PlaceLikelihood
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient

class FindRestaurantActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var btnOpenNavigation: Button
    private lateinit var btnSchedule: Button
    private lateinit var placesClient: PlacesClient
    private var currentLatLng: LatLng? = null
    private val LOCATION_PERMISSION_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_find_restaurant)

        // 🔐 Places Initialization
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, getString(R.string.google_maps_key))
        }
        placesClient = Places.createClient(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        btnOpenNavigation = findViewById(R.id.btnOpenNavigation)
        btnSchedule = findViewById(R.id.btnSchedule)

        val btnSearchNearby = findViewById<Button>(R.id.btnSearchNearby)
        btnSearchNearby.setOnClickListener {
            findNearbyRestaurants()
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        btnOpenNavigation.setOnClickListener {
            currentLatLng?.let {
                val gmmIntentUri = Uri.parse("google.navigation:q=${it.latitude},${it.longitude}")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                mapIntent.setPackage("com.google.android.apps.maps")
                if (mapIntent.resolveActivity(packageManager) != null) {
                    startActivity(mapIntent)
                } else {
                    Toast.makeText(this, "Google Maps not installed", Toast.LENGTH_SHORT).show()
                }
            } ?: Toast.makeText(this, "Location not ready yet", Toast.LENGTH_SHORT).show()
        }

        btnSchedule.setOnClickListener {
            currentLatLng?.let {
                val intent = Intent(this, ScheduleActivity::class.java)
                intent.putExtra("lat", it.latitude)
                intent.putExtra("lng", it.longitude)
                startActivity(intent)
            } ?: Toast.makeText(this, "Location not ready yet", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true
        getLastLocation()
    }

    private fun getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_CODE
            )
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                currentLatLng = LatLng(location.latitude, location.longitude)
                currentLatLng?.let {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(it, 15f))
                    mMap.addMarker(MarkerOptions().position(it).title("You are here"))
                    findNearbyRestaurants()
                }
            } else {
                Toast.makeText(this, "Unable to fetch location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun findNearbyRestaurants() {
        val placeFields = listOf(Place.Field.NAME, Place.Field.LAT_LNG)
        val request = FindCurrentPlaceRequest.newInstance(placeFields)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) return

        placesClient.findCurrentPlace(request).addOnSuccessListener { response ->
            for (placeLikelihood: PlaceLikelihood in response.placeLikelihoods) {
                val place = placeLikelihood.place
                val name = place.name ?: continue
                val location = place.latLng ?: continue

                if (name.contains("restaurant", true) || name.contains("cafe", true)) {
                    mMap.addMarker(MarkerOptions().position(location).title(name))
                }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to fetch nearby restaurants", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == LOCATION_PERMISSION_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getLastLocation()
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }
}
