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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.PlaceLikelihood
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.izzat.restaurantfinder.adapter.RestaurantAdapter
import com.izzat.restaurantfinder.api.PlacesApiService
import com.izzat.restaurantfinder.model.Restaurant
import kotlinx.coroutines.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.math.*

import com.izzat.restaurantfinder.api.WeatherApiService
import com.izzat.restaurantfinder.model.WeatherResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FindRestaurantActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var btnOpenNavigation: Button
    private lateinit var btnSchedule: Button
    private lateinit var btnSearchNearby: Button
    private lateinit var recyclerNearby: RecyclerView
    private lateinit var restaurantAdapter: RestaurantAdapter
    private lateinit var placesClient: PlacesClient
    private lateinit var retrofit: Retrofit
    private lateinit var placesApi: PlacesApiService
    private lateinit var btnOpenInGoogle: Button
    private lateinit var btnOpenInWaze: Button
    private lateinit var weatherApi: WeatherApiService

    private var currentLatLng: LatLng? = null
    private val LOCATION_PERMISSION_CODE = 1001

    private val restaurantResults = mutableListOf<Restaurant>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_find_restaurant)

        // Retrofit & Places Init
        retrofit = Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        placesApi = retrofit.create(PlacesApiService::class.java)

        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, getString(R.string.google_maps_key))
        }
        placesClient = Places.createClient(this)

        // Views
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        btnOpenNavigation = findViewById(R.id.btnOpenNavigation)
        btnSchedule = findViewById(R.id.btnSchedule)
        btnSearchNearby = findViewById(R.id.btnSearchNearby)
        recyclerNearby = findViewById(R.id.recyclerNearbyRestaurants)
        btnOpenInGoogle = findViewById(R.id.btnOpenInGoogle)
        btnOpenInWaze = findViewById(R.id.btnOpenInWaze)

        // Recycler Setup
        restaurantAdapter = RestaurantAdapter { selectedRestaurant ->
            val intent = Intent(this, ScheduleActivity::class.java)
            intent.putExtra("name", selectedRestaurant.name)
            intent.putExtra("lat", selectedRestaurant.latLng.latitude)
            intent.putExtra("lng", selectedRestaurant.latLng.longitude)
            startActivity(intent)
        }

        recyclerNearby.apply {
            layoutManager = LinearLayoutManager(this@FindRestaurantActivity)
            adapter = restaurantAdapter
        }

        btnOpenInGoogle.setOnClickListener {
            if (restaurantResults.isNotEmpty()) {
                val query = restaurantResults[0].name
                val url = "https://www.google.com/search?q=" + Uri.encode(query)
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            } else {
                Toast.makeText(this, "No restaurant selected", Toast.LENGTH_SHORT).show()
            }
        }

        btnOpenInWaze.setOnClickListener {
            currentLatLng?.let {
                val lat = it.latitude
                val lng = it.longitude
                val wazeUri = "https://waze.com/ul?ll=$lat,$lng&navigate=yes"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(wazeUri))
                intent.setPackage("com.waze")

                // Check if Waze is installed
                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                } else {
                    // If not installed, open Play Store or browser
                    val playStoreIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=com.waze")
                    )
                    startActivity(playStoreIntent)
                }
            } ?: Toast.makeText(this, "Location not ready yet", Toast.LENGTH_SHORT).show()
        }

//        val weatherRetrofit = Retrofit.Builder()
//            .baseUrl("https://api.openweathermap.org/")
//            .addConverterFactory(GsonConverterFactory.create())
//            .build()
//
//        weatherApi = weatherRetrofit.create(WeatherApiService::class.java)

        weatherApi = Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApiService::class.java)

        // Map
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Button actions
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
            if (restaurantResults.isNotEmpty()) {
                val names = restaurantResults.map { it.name }
                val intent = Intent(this, ScheduleActivity::class.java)
                intent.putStringArrayListExtra("restaurantList", ArrayList(names))
                startActivity(intent)
            } else {
                Toast.makeText(this, "No restaurants to schedule", Toast.LENGTH_SHORT).show()
            }
        }

        btnSearchNearby.setOnClickListener {
            findNearbyRestaurants()
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
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_CODE
            )
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                currentLatLng = LatLng(location.latitude, location.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng!!, 15f))
                mMap.addMarker(MarkerOptions().position(currentLatLng!!).title("You are here"))

                // Fetch weather here
                getWeather(location.latitude, location.longitude)
            } else {
                Toast.makeText(this, "Unable to fetch location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun findNearbyRestaurants() {
        currentLatLng?.let { userLocation ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = placesApi.getNearbyRestaurants(
                        location = "${userLocation.latitude},${userLocation.longitude}",
                        radius = 10000,
                        type = "restaurant",
                        apiKey = getString(R.string.google_maps_key)
                    )

                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful && response.body()?.results != null) {
                            val places = response.body()!!.results
                            mMap.clear()
                            mMap.addMarker(MarkerOptions().position(userLocation).title("You are here"))
                            restaurantResults.clear()

                            for (place in places) {
                                val name = place.name ?: continue
                                val lat = place.geometry?.location?.lat ?: continue
                                val lng = place.geometry.location.lng
                                val latLng = LatLng(lat, lng)

                                val distance = distanceInKm(userLocation, latLng)
                                if (distance <= 10.0) {
                                    val displayName = "$name (${String.format("%.1f", distance)} km)"
                                    val restaurant = Restaurant(displayName, latLng)
                                    restaurantResults.add(restaurant)
                                    mMap.addMarker(MarkerOptions().position(latLng).title(displayName))
                                }
                            }

                            restaurantAdapter.submitList(restaurantResults.toList())  // Update list
                            Toast.makeText(applicationContext, "${restaurantResults.size} found", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(applicationContext, "No data found", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(applicationContext, "API Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun distanceInKm(start: LatLng, end: LatLng): Double {
        val radius = 6371.0
        val dLat = Math.toRadians(end.latitude - start.latitude)
        val dLng = Math.toRadians(end.longitude - start.longitude)
        val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(start.latitude)) *
                cos(Math.toRadians(end.latitude)) * sin(dLng / 2).pow(2.0)
        return 2 * radius * asin(sqrt(a))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == LOCATION_PERMISSION_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getLastLocation()
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getWeather(lat: Double, lon: Double) {
        val call = weatherApi.getWeatherByCoords(lat, lon, getString(R.string.openweather_api_key))

        call.enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                if (response.isSuccessful) {
                    val weather = response.body()
                    val info = "${weather?.name}: ${weather?.main?.temp}°C, ${weather?.weather?.get(0)?.description}"
                    Toast.makeText(this@FindRestaurantActivity, info, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@FindRestaurantActivity, "Failed to get weather", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                Toast.makeText(this@FindRestaurantActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

}
