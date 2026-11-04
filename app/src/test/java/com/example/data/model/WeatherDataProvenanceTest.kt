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
    fun unverifiedStationDefaultsToOfflineDemo() {
        val station = SyrianWeatherStation(
            id = "test",
            nameAr = "محطة اختبار",
            governorate = "اختبار",
            latitude = 33.5,
            longitude = 36.2,
            temperatureCelsius = 0.0,
            humidity = 0,
            pressureHpa = 0.0,
            windSpeedKmh = 0.0,
            conditionAr = "غير متاح",
            conditionType = WeatherConditionType.CLEAR
        )

        assertFalse(station.isOnline)
        assertTrue(station.isDemoData)
    }

    @Test
    fun defaultAirQualityAndPollenAreUnavailable() {
        val air = AirQualityInfo(
            aqiValue = -1,
            categoryAr = "غير متاح",
            adviceAr = "لا توجد بيانات",
            pm25 = 0.0,
            pm10 = 0.0
        )
        val pollen = PollenInfo(
            grassPollen = 0.0,
            birchPollen = 0.0,
            olivePollen = 0.0,
            ragweedPollen = 0.0,
            overallLevelAr = "غير متاح",
            adviceAr = "لا توجد بيانات"
        )

        assertFalse(air.isAvailable)
        assertFalse(air.isAqiAvailable)
        assertFalse(pollen.isAvailable)
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
