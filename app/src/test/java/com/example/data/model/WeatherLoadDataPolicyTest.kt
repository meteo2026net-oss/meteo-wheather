package com.example.data.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherLoadDataPolicyTest {
    private val damascus = CityLocation(
        name = "Damascus",
        country = "Syria",
        latitude = 33.5138,
        longitude = 36.2765
    )

    @Test
    fun retainsDataWhenRefreshingSameCoordinates() {
        assertTrue(WeatherLoadDataPolicy.shouldRetainPreviousData(damascus, damascus.copy(name = "دمشق")))
    }

    @Test
    fun clearsDataWhenLatitudeChanges() {
        assertFalse(WeatherLoadDataPolicy.shouldRetainPreviousData(damascus, damascus.copy(latitude = 34.0)))
    }

    @Test
    fun clearsDataWhenLongitudeChanges() {
        assertFalse(WeatherLoadDataPolicy.shouldRetainPreviousData(damascus, damascus.copy(longitude = 37.0)))
    }

    @Test
    fun clearsDataWhenSameCityNameHasDifferentCoordinates() {
        val otherLocation = damascus.copy(latitude = 34.0, longitude = 37.0)
        assertFalse(WeatherLoadDataPolicy.shouldRetainPreviousData(damascus, otherLocation))
    }
}
