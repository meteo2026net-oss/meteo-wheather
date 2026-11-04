package com.example.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LeafletBaseMapConfigsTest {
    @Test
    fun eachBaseMapUsesItsOwnProviderAttribution() {
        val dark = LeafletBaseMapConfigs.forStyle("dark")
        val streets = LeafletBaseMapConfigs.forStyle("osm")
        val satellite = LeafletBaseMapConfigs.forStyle("satellite")

        assertEquals("https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png", dark.urlTemplate)
        assertEquals("abcd", dark.subdomains)
        assertEquals(20, dark.maxZoom)
        assertTrue(dark.attributionHtml.contains("CARTO"))

        assertEquals("https://tile.openstreetmap.org/{z}/{x}/{y}.png", streets.urlTemplate)
        assertEquals(null, streets.subdomains)
        assertEquals(19, streets.maxZoom)
        assertTrue(streets.attributionHtml.contains("OpenStreetMap contributors"))

        assertEquals("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}", satellite.urlTemplate)
        assertEquals(null, satellite.subdomains)
        assertEquals(19, satellite.maxZoom)
        assertTrue(satellite.attributionHtml.contains("Tiles &copy; Esri"))
        assertNotEquals(dark.attributionHtml, satellite.attributionHtml)
    }

    @Test
    fun unknownStyleFallsBackToCreditedDarkCartoMap() {
        assertEquals(LeafletBaseMapConfigs.forStyle("dark"), LeafletBaseMapConfigs.forStyle("unknown"))
    }
}
