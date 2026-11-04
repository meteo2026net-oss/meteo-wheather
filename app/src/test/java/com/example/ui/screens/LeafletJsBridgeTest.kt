package com.example.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class LeafletJsBridgeTest {

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
}
