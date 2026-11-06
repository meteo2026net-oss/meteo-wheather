package com.example.ui.screens

import com.example.ui.components.shouldReloadEmbeddedMapHtml
import org.junit.Assert.assertEquals
import org.junit.Test

class LeafletJsBridgeTest {

    @Test
    fun embeddedMapHtmlReloadsOnlyWhenDocumentChanges() {
        val initialHtml = "<html>radar-a</html>"
        val sameHtml = "<html>radar-a</html>"
        val updatedHtml = "<html>radar-b</html>"

        assertEquals(true, shouldReloadEmbeddedMapHtml(null, initialHtml))
        assertEquals(false, shouldReloadEmbeddedMapHtml(initialHtml, sameHtml))
        assertEquals(true, shouldReloadEmbeddedMapHtml(initialHtml, updatedHtml))
    }

    @Test
    fun knownCityWithMatchingCoordinatesIsAcceptedAndJsWeatherIsIgnored() {
        var callbackCount = 0
        var receivedTemp: String? = null
        var receivedState: String? = null
        var receivedLatitude: Double? = null
        var receivedLongitude: Double? = null

        val bridge = LeafletJsBridge { _, temp, state, lat, lng ->
            callbackCount++
            receivedTemp = temp
            receivedState = state
            receivedLatitude = lat
            receivedLongitude = lng
        }

        bridge.onCitySelectedFromJs("دمشق", "99", "مشمس", 33.5138, 36.2765)

        assertEquals(1, callbackCount)
        assertEquals("—", receivedTemp)
        assertEquals("بيانات غير متاحة", receivedState)
        assertEquals(33.5138, receivedLatitude!!, 0.000001)
        assertEquals(36.2765, receivedLongitude!!, 0.000001)
    }

    @Test
    fun unknownCityIsRejected() {
        var callbackCount = 0
        val bridge = LeafletJsBridge { _, _, _, _, _ -> callbackCount++ }

        bridge.onCitySelectedFromJs("مدينة غير معروفة", "20", "مشمس", 33.5138, 36.2765)

        assertEquals(0, callbackCount)
    }

    @Test
    fun mismatchedCoordinatesAreRejected() {
        var callbackCount = 0
        val bridge = LeafletJsBridge { _, _, _, _, _ -> callbackCount++ }

        bridge.onCitySelectedFromJs("دمشق", "20", "مشمس", 36.2021, 37.1343)

        assertEquals(0, callbackCount)
    }

    @Test
    fun nonFiniteCoordinatesAreRejected() {
        var callbackCount = 0
        val bridge = LeafletJsBridge { _, _, _, _, _ -> callbackCount++ }

        bridge.onCitySelectedFromJs("دمشق", "20", "مشمس", Double.NaN, 36.2765)
        bridge.onCitySelectedFromJs("دمشق", "20", "مشمس", 33.5138, Double.POSITIVE_INFINITY)

        assertEquals(0, callbackCount)
    }

    @Test
    fun coordinatesOutsideToleranceAreRejected() {
        var callbackCount = 0
        val bridge = LeafletJsBridge { _, _, _, _, _ -> callbackCount++ }

        bridge.onCitySelectedFromJs("دمشق", "20", "مشمس", 33.5150, 36.2765)

        assertEquals(0, callbackCount)
    }
    @Test
    fun overlayAvailabilityRequiresActualVariableValues() {
        val city = com.example.data.model.CityLocation(
            name = "دمشق", nameEn = "Damascus", country = "سوريا",
            latitude = 33.5138, longitude = 36.2765
        )
        val empty = com.example.data.model.CityMapForecast(
            city = city, time = null, temperatureCelsius = null,
            humidityPercent = null, windSpeedKmh = null, windDirectionDegrees = null,
            cloudCoverPercent = null, precipitationMm = null, weatherCode = null
        )
        val temperatureOnly = empty.copy(temperatureCelsius = 18.5)

        assertEquals(true, isMapOverlayAvailable(com.example.data.model.MapWeatherOverlay.PRECIPITATION, emptyList()))
        assertEquals(false, isMapOverlayAvailable(com.example.data.model.MapWeatherOverlay.TEMPERATURE, listOf(empty)))
        assertEquals(true, isMapOverlayAvailable(com.example.data.model.MapWeatherOverlay.TEMPERATURE, listOf(temperatureOnly)))
        assertEquals(false, isMapOverlayAvailable(com.example.data.model.MapWeatherOverlay.WIND, listOf(temperatureOnly)))
        assertEquals(false, isMapOverlayAvailable(com.example.data.model.MapWeatherOverlay.CLOUDS, listOf(temperatureOnly)))
    }

    @Test
    fun leafletHtmlReloadsOnlyWhenDocumentContentChanges() {
        val first = "<html>forecast-a</html>"
        val same = "<html>forecast-a</html>"
        val changedForecast = "<html>forecast-b</html>"

        assertEquals(true, shouldReloadLeafletHtml(null, first))
        assertEquals(false, shouldReloadLeafletHtml(first, same))
        assertEquals(true, shouldReloadLeafletHtml(first, changedForecast))
    }

}
