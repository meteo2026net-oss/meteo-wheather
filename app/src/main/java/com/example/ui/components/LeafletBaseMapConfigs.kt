package com.example.ui.components

/**
 * Base-map provider configuration. Attribution is tied to the selected tile provider.
 */
internal data class LeafletBaseMapConfig(
    val urlTemplate: String,
    val attributionHtml: String,
    val maxZoom: Int,
    val subdomains: String? = null
)

internal object LeafletBaseMapConfigs {
    fun forStyle(style: String): LeafletBaseMapConfig = when (style) {
        "satellite" -> LeafletBaseMapConfig(
            urlTemplate = "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}",
            attributionHtml = "Tiles &copy; Esri — Source: Esri, Maxar, Earthstar Geographics, and the GIS User Community",
            maxZoom = 19
        )
        "osm" -> LeafletBaseMapConfig(
            urlTemplate = "https://tile.openstreetmap.org/{z}/{x}/{y}.png",
            attributionHtml = "&copy; <a href=\"https://www.openstreetmap.org/copyright\">OpenStreetMap contributors</a>",
            maxZoom = 19
        )
        else -> LeafletBaseMapConfig(
            urlTemplate = "https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png",
            attributionHtml = "&copy; OpenStreetMap contributors &copy; CARTO",
            maxZoom = 20,
            subdomains = "abcd"
        )
    }
}
