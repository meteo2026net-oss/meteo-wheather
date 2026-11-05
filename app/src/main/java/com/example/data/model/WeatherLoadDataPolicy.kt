package com.example.data.model

object WeatherLoadDataPolicy {
    fun shouldRetainPreviousData(previous: CityLocation, requested: CityLocation): Boolean =
        previous.latitude == requested.latitude && previous.longitude == requested.longitude
}
