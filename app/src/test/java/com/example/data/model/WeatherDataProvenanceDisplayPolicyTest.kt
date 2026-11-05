package com.example.data.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherDataProvenanceDisplayPolicyTest {
    private fun value(
        number: Double? = 12.0,
        status: WeatherDataStatus
    ) = QualifiedWeatherValue(
        value = number,
        unit = "°C",
        provenance = WeatherDataProvenance(
            provider = "test-provider",
            kind = WeatherDataKind.OBSERVATION,
            status = status
        )
    )

    @Test
    fun onlyAvailableFiniteMeasurementsAreDisplayable() {
        assertTrue(value(status = WeatherDataStatus.AVAILABLE).isDisplayable)
        assertFalse(value(status = WeatherDataStatus.STALE).isDisplayable)
        assertFalse(value(status = WeatherDataStatus.UNAVAILABLE).isDisplayable)
        assertFalse(value(status = WeatherDataStatus.SIMULATED).isDisplayable)
    }

    @Test
    fun missingAndNonFiniteNumbersAreNeverDisplayable() {
        assertFalse(value(number = null, status = WeatherDataStatus.AVAILABLE).isDisplayable)
        assertFalse(value(number = Double.NaN, status = WeatherDataStatus.AVAILABLE).isDisplayable)
        assertFalse(value(number = Double.POSITIVE_INFINITY, status = WeatherDataStatus.AVAILABLE).isDisplayable)
    }

    @Test
    fun measuredZeroRemainsAValidAvailableValue() {
        assertTrue(value(number = 0.0, status = WeatherDataStatus.AVAILABLE).isDisplayable)
    }
}
