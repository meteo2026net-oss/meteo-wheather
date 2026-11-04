package com.example.data.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherDataProvenanceTest {

    private fun provenance(
        status: WeatherDataStatus = WeatherDataStatus.AVAILABLE
    ) = WeatherDataProvenance(
        provider = "test-provider",
        kind = WeatherDataKind.OBSERVATION,
        status = status,
        observedAtEpochMillis = 1_700_000_000_000
    )

    @Test
    fun measuredZeroIsDisplayable() {
        val value = QualifiedWeatherValue(
            value = 0.0,
            unit = "mm",
            provenance = provenance()
        )

        assertTrue(value.isDisplayable)
    }

    @Test
    fun missingValueIsNotDisplayable() {
        val value = QualifiedWeatherValue(
            value = null,
            unit = "hPa",
            provenance = provenance()
        )

        assertFalse(value.isDisplayable)
    }

    @Test
    fun unavailableSourceIsNotDisplayable() {
        val value = QualifiedWeatherValue(
            value = 12.0,
            unit = "km/h",
            provenance = provenance(WeatherDataStatus.UNAVAILABLE)
        )

        assertFalse(value.isDisplayable)
    }

    @Test
    fun simulatedValueCanBeDisplayedButMustRemainIdentifiedAsSimulation() {
        val value = QualifiedWeatherValue(
            value = 4.0,
            unit = "mm",
            provenance = provenance(WeatherDataStatus.SIMULATED)
        )

        assertTrue(value.isDisplayable)
        assertTrue(value.provenance.status == WeatherDataStatus.SIMULATED)
    }
}
