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
    fun rejectsEmptyOrMismatchedCityAndResponseLists() {
        val damascus = city("دمشق", 33.5138, 36.2765)
        val aleppo = city("حلب", 36.2021, 37.1343)

        assertNull(MapForecastResponseMatching.match(emptyList(), emptyList()))
        assertNull(MapForecastResponseMatching.match(listOf(damascus), emptyList()))
        assertNull(
            MapForecastResponseMatching.match(
                listOf(damascus, aleppo), listOf(response(33.55, 36.30))
            )
        )
    }

    @Test
    fun rejectsInvalidRequestedCityCoordinates() {
        val invalidCity = city("مدينة غير صالحة", Double.NaN, 36.2765)
        assertNull(
            MapForecastResponseMatching.match(
                listOf(invalidCity), listOf(response(33.55, 36.30))
            )
        )
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

    @Test
    fun cityMapMapperKeepsValidMeasurementsAndRejectsInvalidProviderValues() {
        val damascus = city("دمشق", 33.5138, 36.2765)
        val mapped = CityMapForecastMapper.map(
            damascus,
            WeatherResponseDto(
                latitude = 33.55,
                longitude = 36.30,
                current = CurrentWeatherDto(
                    time = "2026-11-06T03:15",
                    temperature = 24.5,
                    relativeHumidity = 65,
                    precipitation = 0.2,
                    weatherCode = 61,
                    windSpeed = 18.0,
                    windDirection = 270.0,
                    cloudCover = 80
                )
            )
        )
        assertEquals(damascus, mapped.city)
        assertEquals("2026-11-06T03:15", mapped.time)
        assertEquals(24.5, mapped.temperatureCelsius!!, 0.0)
        assertEquals(65, mapped.humidityPercent)
        assertEquals(18.0, mapped.windSpeedKmh!!, 0.0)
        assertEquals(270.0, mapped.windDirectionDegrees!!, 0.0)
        assertEquals(80, mapped.cloudCoverPercent)
        assertEquals(0.2, mapped.precipitationMm!!, 0.0)
        assertEquals(61, mapped.weatherCode)
    }

    @Test
    fun cityMapMapperDoesNotExposeMalformedOrOutOfRangeMeasurements() {
        val aleppo = city("حلب", 36.2021, 37.1343)
        val mapped = CityMapForecastMapper.map(
            aleppo,
            WeatherResponseDto(
                latitude = 36.24,
                longitude = 37.16,
                current = CurrentWeatherDto(
                    time = "2026-02-30T25:90",
                    temperature = 999.0,
                    relativeHumidity = 120,
                    precipitation = Double.NaN,
                    weatherCode = 120,
                    windSpeed = 301.0,
                    windDirection = 361.0,
                    cloudCover = -1
                )
            )
        )
        assertEquals(aleppo, mapped.city)
        assertNull(mapped.time)
        assertNull(mapped.temperatureCelsius)
        assertNull(mapped.humidityPercent)
        assertNull(mapped.windSpeedKmh)
        assertNull(mapped.windDirectionDegrees)
        assertNull(mapped.cloudCoverPercent)
        assertNull(mapped.precipitationMm)
        assertNull(mapped.weatherCode)
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
