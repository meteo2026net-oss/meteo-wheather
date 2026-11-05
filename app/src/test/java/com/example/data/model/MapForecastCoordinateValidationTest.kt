package com.example.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    @Test
    fun matchesReorderedResponsesToTheirNearestCities() {
        val damascus = city("دمشق", 33.5138, 36.2765)
        val aleppo = city("حلب", 36.2021, 37.1343)
        val cities = listOf(damascus, aleppo)
        val aleppoResponse = response(36.24, 37.16)
        val damascusResponse = response(33.55, 36.30)

        val matched = MapForecastResponseMatching.match(
            cities, listOf(aleppoResponse, damascusResponse)
        )

        assertEquals(listOf(damascus, aleppo), matched?.map { it.first })
        assertEquals(damascusResponse, matched?.get(0)?.second)
        assertEquals(aleppoResponse, matched?.get(1)?.second)
    }

    @Test
    fun rejectsResponsesTooFarFromEveryRequestedCity() {
        val matched = MapForecastResponseMatching.match(
            listOf(city("دمشق", 33.5138, 36.2765)),
            listOf(response(0.0, 0.0))
        )

        assertNull(matched)
    }

    @Test
    fun rejectsDuplicateNearestCityAssignments() {
        val cities = listOf(
            city("دمشق", 33.5138, 36.2765),
            city("حلب", 36.2021, 37.1343)
        )

        val matched = MapForecastResponseMatching.match(
            cities,
            listOf(response(33.55, 36.30), response(33.56, 36.31))
        )

        assertNull(matched)
    }

    @Test
    fun rejectsAmbiguousAndInvalidResponseCoordinates() {
        val nearbyCities = listOf(
            city("A", 33.5138, 36.2765),
            city("B", 33.5138, 36.2865)
        )
        assertNull(
            MapForecastResponseMatching.match(
                nearbyCities, listOf(response(33.5138, 36.2815), response(33.5138, 36.2816))
            )
        )
        assertNull(
            MapForecastResponseMatching.match(
                listOf(city("دمشق", 33.5138, 36.2765)),
                listOf(response(Double.NaN, 36.2))
            )
        )
    }

    private fun city(name: String, latitude: Double, longitude: Double) = CityLocation(
        name = name,
        country = "سوريا",
        latitude = latitude,
        longitude = longitude
    )

    private fun response(latitude: Double, longitude: Double) = WeatherResponseDto(
        latitude = latitude,
        longitude = longitude
    )
}
