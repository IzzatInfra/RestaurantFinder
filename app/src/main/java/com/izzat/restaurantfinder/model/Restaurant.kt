package com.izzat.restaurantfinder.model

import com.google.android.gms.maps.model.LatLng

data class Restaurant(
    val name: String,
    val latLng: LatLng
)
