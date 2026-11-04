package com.example.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ForecastValueValidationTest {
    @Test
    fun finiteRejectsMissingAndNonFiniteValuesButKeepsNegativeTemperatures() {
        assertNull(ForecastValueValidation.finite(null))
        assertNull(ForecastValueValidation.finite(Double.NaN))
        assertNull(ForecastValueValidation.finite(Double.POSITIVE_INFINITY))
        assertEquals(-12.5, ForecastValueValidation.finite(-12.5)!!, 0.0)
        assertEquals(0.0, ForecastValueValidation.finite(0.0)!!, 0.0)
    }

    @Test
    fun nonNegativeFiniteRejectsNegativeAndNonFiniteMeasurements() {
        assertNull(ForecastValueValidation.nonNegativeFinite(-0.1))
        assertNull(ForecastValueValidation.nonNegativeFinite(Double.NaN))
        assertNull(ForecastValueValidation.nonNegativeFinite(null))
        assertEquals(0.0, ForecastValueValidation.nonNegativeFinite(0.0)!!, 0.0)
        assertEquals(18.2, ForecastValueValidation.nonNegativeFinite(18.2)!!, 0.0)
    }

    @Test
    fun percentageValidatorsEnforceInclusiveZeroToOneHundredRange() {
        assertEquals(0, ForecastValueValidation.probabilityPercent(0))
        assertEquals(100, ForecastValueValidation.probabilityPercent(100))
        assertNull(ForecastValueValidation.probabilityPercent(-1))
        assertNull(ForecastValueValidation.probabilityPercent(101))
        assertEquals(0, ForecastValueValidation.humidityPercent(0))
        assertEquals(100, ForecastValueValidation.humidityPercent(100))
        assertNull(ForecastValueValidation.humidityPercent(-1))
        assertNull(ForecastValueValidation.humidityPercent(101))
    }

    @Test
    fun temperatureRangeRequiresTwoFiniteValuesAndMaximumNotBelowMinimum() {
        assertEquals(12.0 to -3.0, ForecastValueValidation.temperatureRange(12.0, -3.0))
        assertEquals(0.0 to 0.0, ForecastValueValidation.temperatureRange(0.0, 0.0))
        assertNull(ForecastValueValidation.temperatureRange(null, 0.0))
        assertNull(ForecastValueValidation.temperatureRange(Double.NaN, 0.0))
        assertNull(ForecastValueValidation.temperatureRange(2.0, 3.0))
    }

    @Test
    fun nonNegativeIntegerRejectsNegativeValues() {
        assertEquals(0, ForecastValueValidation.nonNegativeInt(0))
        assertEquals(42, ForecastValueValidation.nonNegativeInt(42))
        assertNull(ForecastValueValidation.nonNegativeInt(-1))
        assertNull(ForecastValueValidation.nonNegativeInt(null))
    }
}
