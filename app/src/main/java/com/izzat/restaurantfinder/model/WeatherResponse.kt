package com.izzat.restaurantfinder.model

data class WeatherResponse(
    val weather: List<Weather>,
    val main: Main,
    val name: String
)

data class Weather(val main: String, val description: String)
data class Main(val temp: Float, val humidity: Int)
