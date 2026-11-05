package com.example.data.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StationMeasurementDisplayPolicyTest {
    @Test
    fun temperatureAcceptsBroadPhysicalRangeAndRejectsInvalidValues() {
        assertTrue(StationMeasurementDisplayPolicy.temperatureCelsius(-90.0))
        assertTrue(StationMeasurementDisplayPolicy.temperatureCelsius(0.0))
        assertTrue(StationMeasurementDisplayPolicy.temperatureCelsius(55.0))
        assertFalse(StationMeasurementDisplayPolicy.temperatureCelsius(-101.0))
        assertFalse(StationMeasurementDisplayPolicy.temperatureCelsius(71.0))
        assertFalse(StationMeasurementDisplayPolicy.temperatureCelsius(Double.NaN))
        assertFalse(StationMeasurementDisplayPolicy.temperatureCelsius(Double.POSITIVE_INFINITY))
    }

    @Test
    fun humidityMustBeWithinZeroAndOneHundredPercent() {
        assertTrue(StationMeasurementDisplayPolicy.relativeHumidityPercent(0))
        assertTrue(StationMeasurementDisplayPolicy.relativeHumidityPercent(100))
        assertFalse(StationMeasurementDisplayPolicy.relativeHumidityPercent(-1))
        assertFalse(StationMeasurementDisplayPolicy.relativeHumidityPercent(101))
    }

    @Test
    fun pressureMustBeFiniteAndWithinBroadAtmosphericRange() {
        assertTrue(StationMeasurementDisplayPolicy.pressureHpa(800.0))
        assertTrue(StationMeasurementDisplayPolicy.pressureHpa(1013.25))
        assertTrue(StationMeasurementDisplayPolicy.pressureHpa(1100.0))
        assertFalse(StationMeasurementDisplayPolicy.pressureHpa(799.9))
        assertFalse(StationMeasurementDisplayPolicy.pressureHpa(1100.1))
        assertFalse(StationMeasurementDisplayPolicy.pressureHpa(Double.NaN))
    }

    @Test
    fun windSpeedMustBeFiniteAndNonNegative() {
        assertTrue(StationMeasurementDisplayPolicy.windSpeedKmh(0.0))
        assertTrue(StationMeasurementDisplayPolicy.windSpeedKmh(120.0))
        assertFalse(StationMeasurementDisplayPolicy.windSpeedKmh(-0.1))
        assertFalse(StationMeasurementDisplayPolicy.windSpeedKmh(300.1))
        assertFalse(StationMeasurementDisplayPolicy.windSpeedKmh(Double.POSITIVE_INFINITY))
    }
}
