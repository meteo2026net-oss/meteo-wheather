package com.example.data.model

import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeatherDtoNullabilityTest {
    @Test
    fun hourlyAndDailyDtosAcceptNullMeasurementsWithoutRejectingWholeResponse() {
        val moshi = Moshi.Builder().build()
        val hourly = moshi.adapter(HourlyWeatherDto::class.java).fromJson(
            """{
                "time":["2026-11-05T00:00","2026-11-05T01:00"],
                "temperature_2m":[12.0,null],
                "relative_humidity_2m":[55,null],
                "precipitation_probability":[0,null],
                "weather_code":[0,null],
                "wind_speed_10m":[4.5,null],
                "visibility":[10000,null]
            }"""
        )!!

        assertEquals(12.0, hourly.temperature[0]!!, 0.0)
        assertNull(hourly.temperature[1])
        assertNull(hourly.relativeHumidity[1])
        assertNull(hourly.precipitationProbability?.get(1))
        assertNull(hourly.weatherCode[1])
        assertNull(hourly.windSpeed[1])
        assertNull(hourly.visibility?.get(1))

        val daily = moshi.adapter(DailyWeatherDto::class.java).fromJson(
            """{
                "time":["2026-11-05","2026-11-06"],
                "weather_code":[0,null],
                "temperature_2m_max":[20.0,null],
                "temperature_2m_min":[10.0,null],
                "sunrise":["2026-11-05T06:00",null],
                "sunset":["2026-11-05T17:00",null],
                "uv_index_max":[3.0,null],
                "precipitation_probability_max":[10,null]
            }"""
        )!!

        assertNull(daily.weatherCode[1])
        assertNull(daily.temperatureMax[1])
        assertNull(daily.temperatureMin[1])
        assertNull(daily.sunrise[1])
        assertNull(daily.sunset[1])
        assertNull(daily.uvIndexMax?.get(1))
        assertNull(daily.precipitationProbabilityMax?.get(1))
    }

    @Test
    fun forecastSelectionSkipsNullWeatherCodesAndIncompleteRecords() {
        assertEquals(
            listOf(0),
            HourlyForecastSelection.validIndices(
                times = listOf("2026-11-05T00:00", "2026-11-05T01:00"),
                weatherCodes = listOf(0, null),
                startIndex = 0
            )
        )
        assertEquals(
            listOf(0),
            DailyForecastSelection.validIndices(
                dates = listOf("2026-11-05", "2026-11-06"),
                weatherCodes = listOf(0, null),
                maxTemperatures = listOf(20.0, 21.0),
                minTemperatures = listOf(10.0, 11.0)
            )
        )
    }
}
