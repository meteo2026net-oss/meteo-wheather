package com.example.data.remote

import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SupabaseStationReadingsDataSourceTest {
    private val dataSource = SupabaseStationReadingsDataSource()

    @Test
    fun keepsNewestVerifiedRowPerStationWhenRowsAreNewestFirst() {
        val rows = JSONArray(
            """
            [
              {"station_id":"damascus","observed_at":"2026-11-06T02:00:00Z","temperature_c":18.5,"relative_humidity":42,"pressure_hpa":1012.4,"wind_speed_kmh":7.0},
              {"station_id":"damascus","observed_at":"2026-11-06T01:00:00Z","temperature_c":17.0,"relative_humidity":45,"pressure_hpa":1013.0,"wind_speed_kmh":5.0}
            ]
            """.trimIndent()
        )

        val result = dataSource.parseLatestReadings(rows)

        assertEquals(1, result.size)
        assertEquals("2026-11-06T02:00:00Z", result.getValue("damascus").observedAt)
        assertEquals(18.5, result.getValue("damascus").temperatureCelsius!!, 0.0)
    }

    @Test
    fun ignoresRowsWithoutAnyUsableMeasurement() {
        val rows = JSONArray(
            """
            [
              {"station_id":"empty","observed_at":"2026-11-06T02:00:00Z"},
              {"station_id":"invalid","observed_at":"2026-11-06T02:00:00Z","temperature_c":999,"relative_humidity":150,"pressure_hpa":1200,"wind_speed_kmh":-1}
            ]
            """.trimIndent()
        )

        assertTrue(dataSource.parseLatestReadings(rows).isEmpty())
    }

    @Test
    fun retainsValidFieldsAndDropsOutOfRangeFields() {
        val rows = JSONArray(
            """
            [
              {"station_id":"aleppo","observed_at":"2026-11-06T02:00:00Z","temperature_c":21.0,"relative_humidity":140,"pressure_hpa":1010.0,"wind_speed_kmh":400.0,"wind_direction_deg":361.0,"precipitation_mm":-0.5}
            ]
            """.trimIndent()
        )

        val reading = dataSource.parseLatestReadings(rows).getValue("aleppo")

        assertEquals(21.0, reading.temperatureCelsius!!, 0.0)
        assertNull(reading.relativeHumidityPercent)
        assertEquals(1010.0, reading.pressureHpa!!, 0.0)
        assertNull(reading.windSpeedKmh)
        assertNull(reading.windDirectionDegrees)
        assertNull(reading.precipitationMm)
    }

    @Test
    fun skipsRowsWithoutStationIdOrObservationTime() {
        val rows = JSONArray(
            """
            [
              {"observed_at":"2026-11-06T02:00:00Z","temperature_c":20},
              {"station_id":"latakia","temperature_c":20}
            ]
            """.trimIndent()
        )

        val result = dataSource.parseLatestReadings(rows)

        assertFalse(result.containsKey("latakia"))
        assertTrue(result.isEmpty())
    }
}
