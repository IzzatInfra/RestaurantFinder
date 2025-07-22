package com.izzat.restaurantfinder.api

import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.Call
import com.izzat.restaurantfinder.model.WeatherResponse

interface WeatherApiService {
    @GET("data/2.5/weather")
    fun getWeatherByCoords(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): Call<WeatherResponse>
}