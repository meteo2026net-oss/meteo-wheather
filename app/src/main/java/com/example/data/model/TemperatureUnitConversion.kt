package com.example.data.model

/**
 * Converts Celsius readings for display without changing the stored/API value.
 */
object TemperatureUnitConversion {
    fun toDisplayValue(celsius: Double, useFahrenheit: Boolean): Int {
        val value = if (useFahrenheit) celsius * 9.0 / 5.0 + 32.0 else celsius
        return value.toInt()
    }
}
