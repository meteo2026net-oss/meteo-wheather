package com.example.data.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapForecastCoordinateValidationTest {
    @Test
    fun acceptsValidGridCellCentresThatDifferFromRequestedCityCoordinates() {
        // The API reports model grid-cell centres, not necessarily the exact
        // latitude/longitude supplied for the city.
        assertTrue(MapForecastCoordinateValidation.isValidGridCell(33.56, 36.31))
        assertTrue(MapForecastCoordinateValidation.isValidGridCell(35.0, 35.0))
        assertTrue(MapForecastCoordinateValidation.isValidGridCell(-90.0, -180.0))
        assertTrue(MapForecastCoordinateValidation.isValidGridCell(90.0, 180.0))
    }

    @Test
    fun rejectsNonFiniteAndOutOfRangeGridCoordinates() {
        assertFalse(MapForecastCoordinateValidation.isValidGridCell(Double.NaN, 36.0))
        assertFalse(MapForecastCoordinateValidation.isValidGridCell(33.0, Double.POSITIVE_INFINITY))
        assertFalse(MapForecastCoordinateValidation.isValidGridCell(90.01, 36.0))
        assertFalse(MapForecastCoordinateValidation.isValidGridCell(33.0, -180.01))
    }
}
