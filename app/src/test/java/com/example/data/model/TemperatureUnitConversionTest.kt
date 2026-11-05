package com.example.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TemperatureUnitConversionTest {
    @Test
    fun keepsCelsiusValuesWhenCelsiusIsSelected() {
        assertEquals(0, TemperatureUnitConversion.toDisplayValue(0.0, false))
        assertEquals(21, TemperatureUnitConversion.toDisplayValue(21.9, false))
        assertEquals(-5, TemperatureUnitConversion.toDisplayValue(-5.8, false))
    }

    @Test
    fun convertsCelsiusToFahrenheitWhenFahrenheitIsSelected() {
        assertEquals(32, TemperatureUnitConversion.toDisplayValue(0.0, true))
        assertEquals(212, TemperatureUnitConversion.toDisplayValue(100.0, true))
        assertEquals(-40, TemperatureUnitConversion.toDisplayValue(-40.0, true))
        assertEquals(68, TemperatureUnitConversion.toDisplayValue(20.0, true))
    }
}
