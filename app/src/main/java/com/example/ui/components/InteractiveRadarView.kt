package com.example.ui.components

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InteractiveRadarView(
    cityName: String,
    modifier: Modifier = Modifier
) {
    var isSatelliteMode by remember { mutableStateOf(false) }

    // Coordinates lookup for Syrian cities or default center
    val (lat, lng) = remember(cityName) {
        when {
            cityName.contains("دمشق") -> Pair(33.5138, 36.2765)
            cityName.contains("حلب") -> Pair(36.2021, 37.1343)
            cityName.contains("اللاذقية") -> Pair(35.5317, 35.7915)
            cityName.contains("حمص") -> Pair(34.7324, 36.7137)
            cityName.contains("طرطوس") -> Pair(34.8890, 35.8866)
            cityName.contains("حماة") -> Pair(35.1318, 36.7578)
            cityName.contains("دير الزور") -> Pair(35.3333, 40.1500)
            cityName.contains("القامشلي") -> Pair(37.0494, 41.2268)
            cityName.contains("السويداء") -> Pair(32.7130, 36.5694)
            cityName.contains("درعا") -> Pair(32.6186, 36.1011)
            cityName.contains("إدلب") -> Pair(35.9306, 36.6339)
            cityName.contains("الرقة") -> Pair(35.9556, 39.0078)
            cityName.contains("تدمر") -> Pair(34.5577, 38.2753)
            else -> Pair(34.8021, 38.9968) // Syria center
        }
    }

    val htmlContent = remember(lat, lng, isSatelliteMode) {
        buildCityRadarLeafletHtml(cityName, lat, lng, isSatelliteMode)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.16f))
            .border(1.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(24.dp))
            .padding(16.dp)
            .testTag("interactive_radar_view")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Radar,
                    contentDescription = null,
                    tint = Color(0xFF67E8F9),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "رادار الأمطار والسحب الحقيقي (Leaflet)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "رادار حقيقي لمنطقة $cityName وسوريا",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
            }

            // Satellite Toggle
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSatelliteMode) Color(0xFF0284C7) else Color.White.copy(alpha = 0.18f))
                    .clickable { isSatelliteMode = !isSatelliteMode }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isSatelliteMode) "🛰️ قمر صناعي" else "🗺️ خريطة داكنة",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Leaflet Radar Container Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF0F172A))
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(18.dp))
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.builtInZoomControls = false
                        settings.setSupportZoom(true)
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        webViewClient = WebViewClient()
                        webChromeClient = WebChromeClient()
                        setBackgroundColor(0xFF0F172A.toInt())
                        loadDataWithBaseURL(
                            "https://openstreetmap.org",
                            htmlContent,
                            "text/html",
                            "UTF-8",
                            null
                        )
                    }
                },
                update = { webView ->
                    webView.loadDataWithBaseURL(
                        "https://openstreetmap.org",
                        htmlContent,
                        "text/html",
                        "UTF-8",
                        null
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Legend bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "مقياس شدة الهطول الراداري:",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.75f)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "خفيف", fontSize = 9.sp, color = Color(0xFF4ADE80))
                Box(
                    modifier = Modifier
                        .width(90.dp)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(
                            androidx.compose.ui.graphics.Brush.horizontalGradient(
                                listOf(Color(0xFF22C55E), Color(0xFFEAB308), Color(0xFFEF4444), Color(0xFFA855F7))
                            )
                        )
                )
                Text(text = "شديد", fontSize = 9.sp, color = Color(0xFFEC4899))
            }
        }
    }
}

private fun buildCityRadarLeafletHtml(cityName: String, lat: Double, lng: Double, isSatellite: Boolean): String {
    val baseTile = if (isSatellite) {
        "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"
    } else {
        "https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
    }

    return """
        <!DOCTYPE html>
        <html dir="rtl">
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
                html, body, #map { width: 100%; height: 100%; margin: 0; padding: 0; background: #0F172A; }
                .leaflet-container { background: #0F172A; font-family: system-ui, sans-serif; }
                .leaflet-popup-content-wrapper { background: #0F172A; color: #FFF; border-radius: 10px; border: 1px solid #38BDF8; }
                .leaflet-popup-tip { background: #0F172A; }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var map = L.map('map', {
                    center: [$lat, $lng],
                    zoom: 9,
                    minZoom: 6,
                    maxZoom: 13,
                    zoomControl: false
                });

                L.tileLayer('$baseTile', {
                    attribution: '&copy; OpenStreetMap & MCNC',
                    subdomains: 'abcd',
                    maxZoom: 18
                }).addTo(map);

                // RainViewer Real-Time Weather Radar
                L.tileLayer('https://tile.rainviewer.com/v2/radar/nowcast_latest/256/{z}/{x}/{y}/2/1_1.png', {
                    opacity: 0.70,
                    maxZoom: 18
                }).addTo(map);

                // City Center Marker
                var cityMarker = L.circleMarker([$lat, $lng], {
                    color: '#67E8F9',
                    fillColor: '#0284C7',
                    fillOpacity: 0.9,
                    radius: 9
                }).addTo(map);

                cityMarker.bindPopup('<b>رادار منطقة $cityName</b>').openPopup();
            </script>
        </body>
        </html>
    """.trimIndent()
}
