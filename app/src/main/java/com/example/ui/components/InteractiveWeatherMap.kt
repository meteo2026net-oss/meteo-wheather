package com.example.ui.components

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.CityLocation
import com.example.data.model.MapWeatherOverlay

enum class LeafletTileStyle(val titleAr: String, val icon: String, val tileType: String) {
    DARK("خريطة ليفليت الداكنة", "🗺️", "dark"),
    SATELLITE("الأقمار الصناعية", "🛰️", "satellite"),
    STREETS("خريطة الشوارع", "🌍", "osm")
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InteractiveWeatherMap(
    currentCity: CityLocation,
    onCitySelected: (CityLocation) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTileStyle by remember { mutableStateOf(LeafletTileStyle.DARK) }
    var selectedOverlay by remember { mutableStateOf(MapWeatherOverlay.PRECIPITATION) }
    var isTimelinePlaying by remember { mutableStateOf(true) }
    var timelineStep by remember { mutableIntStateOf(0) }

    // Timeline automatic step increment
    LaunchedEffect(isTimelinePlaying) {
        while (isTimelinePlaying) {
            kotlinx.coroutines.delay(2400)
            timelineStep = (timelineStep + 1) % 5
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Color.White.copy(alpha = 0.16f))
            .border(1.5.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(26.dp))
            .padding(16.dp)
            .testTag("interactive_weather_map_container")
    ) {
        // Map Header & Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0284C7)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "خريطة سوريا التفاعلية (Leaflet)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "المركز الوطني للأرصاد الجوية والمناخ (MCNC)",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tile Style Selector Tabs (Leaflet Dark / Satellite / Street)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF0F172A))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            LeafletTileStyle.values().forEach { style ->
                val isSelected = selectedTileStyle == style
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) Color(0xFF0284C7) else Color.Transparent
                        )
                        .clickable { selectedTileStyle = style }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = style.icon, fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = style.titleAr,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Overlay Filter Chips (Precipitation, Temp, Wind, Clouds)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MapWeatherOverlay.values().forEach { overlay ->
                val isSelected = selectedOverlay == overlay
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) Color(0xFF0284C7).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.12f)
                        )
                        .border(
                            1.dp,
                            if (isSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.15f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { selectedOverlay = overlay }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = overlay.titleAr,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Real Leaflet Map Container via WebView
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF090D1A))
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(20.dp))
        ) {
            val htmlContent = remember(selectedTileStyle, selectedOverlay) {
                buildLeafletMapHtml(selectedTileStyle.tileType, selectedOverlay)
            }

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
                        setBackgroundColor(0xFF090D1A.toInt())
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

        Spacer(modifier = Modifier.height(12.dp))

        // Timeline Slider Bar (+0h, +1h, +2h, +3h, +6h)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { isTimelinePlaying = !isTimelinePlaying },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (isTimelinePlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "تشغيل الحركة",
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(20.dp)
                )
            }

            val timelineLabels = listOf("الآن", "+1 س", "+2 س", "+3 س", "+6 س")
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                timelineLabels.forEachIndexed { idx, label ->
                    val isSelected = timelineStep == idx
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.08f)
                            )
                            .clickable { timelineStep = idx }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color(0xFF0F172A) else Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Overlay Legend Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = when (selectedOverlay) {
                    MapWeatherOverlay.PRECIPITATION -> "مقياس الهطول:"
                    MapWeatherOverlay.TEMPERATURE -> "التدرج الحراري:"
                    MapWeatherOverlay.WIND -> "سرعة الرياح:"
                    MapWeatherOverlay.CLOUDS -> "كثافة السحب:"
                },
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.75f)
            )

            // Color Gradient bar for current overlay
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(
                        when (selectedOverlay) {
                            MapWeatherOverlay.PRECIPITATION -> Brush.horizontalGradient(
                                listOf(Color(0xFF22C55E), Color(0xFFEAB308), Color(0xFFEF4444), Color(0xFFA855F7))
                            )
                            MapWeatherOverlay.TEMPERATURE -> Brush.horizontalGradient(
                                listOf(Color(0xFF38BDF8), Color(0xFF4ADE80), Color(0xFFFBBF24), Color(0xFFEF4444))
                            )
                            MapWeatherOverlay.WIND -> Brush.horizontalGradient(
                                listOf(Color(0xFFE2E8F0), Color(0xFF38BDF8), Color(0xFF6366F1), Color(0xFFEC4899))
                            )
                            MapWeatherOverlay.CLOUDS -> Brush.horizontalGradient(
                                listOf(Color(0x33FFFFFF), Color(0x99FFFFFF), Color(0xFFFFFFFF))
                            )
                        }
                    )
            )
        }
    }
}

// Helper to build Leaflet HTML map string centered on Syria
private fun buildLeafletMapHtml(tileStyle: String, overlay: MapWeatherOverlay): String {
    val baseTileUrl = when (tileStyle) {
        "satellite" -> "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"
        "osm" -> "https://tile.openstreetmap.org/{z}/{x}/{y}.png"
        else -> "https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
    }

    val overlayColor = when (overlay) {
        MapWeatherOverlay.PRECIPITATION -> "#0284C7"
        MapWeatherOverlay.TEMPERATURE -> "#EF4444"
        MapWeatherOverlay.WIND -> "#38BDF8"
        MapWeatherOverlay.CLOUDS -> "#94A3B8"
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
                html, body, #map { width: 100%; height: 100%; margin: 0; padding: 0; background: #090D1A; }
                .leaflet-container { background: #090D1A; font-family: system-ui, sans-serif; }
                .city-popup { text-align: right; direction: rtl; font-size: 12px; line-height: 1.6; padding: 2px; }
                .city-title { font-size: 14px; font-weight: bold; color: #38BDF8; margin-bottom: 2px; }
                .leaflet-popup-content-wrapper { background: #0F172A; color: #FFF; border-radius: 12px; border: 1.5px solid #0284C7; box-shadow: 0 4px 14px rgba(0,0,0,0.5); }
                .leaflet-popup-tip { background: #0F172A; }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var map = L.map('map', {
                    center: [34.8021, 38.9968],
                    zoom: 7,
                    minZoom: 5,
                    maxZoom: 12,
                    zoomControl: false
                });

                L.tileLayer('$baseTileUrl', {
                    attribution: '&copy; OpenStreetMap & MCNC Syria',
                    subdomains: 'abcd',
                    maxZoom: 18
                }).addTo(map);

                // RainViewer Real-Time Radar Layer Overlay
                L.tileLayer('https://tile.rainviewer.com/v2/radar/nowcast_latest/256/{z}/{x}/{y}/2/1_1.png', {
                    opacity: 0.65,
                    maxZoom: 18
                }).addTo(map);

                // Syrian Cities Meteorological Pins
                var syrianCities = [
                    { name: 'دمشق (العاصمة)', lat: 33.5138, lng: 36.2765, temp: '18°C', state: 'مشمس وسماء صافية', wind: '14 كم/س' },
                    { name: 'حلب', lat: 36.2021, lng: 37.1343, temp: '22°C', state: 'غائم جزئياً', wind: '16 كم/س' },
                    { name: 'اللاذقية (الساحل)', lat: 35.5317, lng: 35.7915, temp: '20°C', state: 'ماطر وخفيف الرطوبة', wind: '22 كم/س' },
                    { name: 'حمص', lat: 34.7324, lng: 36.7137, temp: '19°C', state: 'معتدل ولطيف', wind: '18 كم/س' },
                    { name: 'طرطوس', lat: 34.8890, lng: 35.8866, temp: '21°C', state: 'زخات مطرية', wind: '24 كم/س' },
                    { name: 'حماة', lat: 35.1318, lng: 36.7578, temp: '21°C', state: 'مشمس', wind: '15 كم/س' },
                    { name: 'دير الزور (المنطقة الشرقية)', lat: 35.3333, lng: 40.1500, temp: '26°C', state: 'دافئ وجاف', wind: '20 كم/س' },
                    { name: 'القامشلي', lat: 37.0494, lng: 41.2268, temp: '24°C', state: 'صحو مع رياح خفيفة', wind: '22 كم/س' },
                    { name: 'السويداء (المنطقة الجنوبية)', lat: 32.7130, lng: 36.5694, temp: '17°C', state: 'بارد نسبياً', wind: '12 كم/س' },
                    { name: 'درعا', lat: 32.6186, lng: 36.1011, temp: '19°C', state: 'مشمس وصحو', wind: '16 كم/س' },
                    { name: 'إدلب', lat: 35.9306, lng: 36.6339, temp: '20°C', state: 'غيوم متفرقة', wind: '19 كم/س' },
                    { name: 'الرقة', lat: 35.9556, lng: 39.0078, temp: '25°C', state: 'صحو', wind: '21 كم/س' },
                    { name: 'تدمر (البادية السورية)', lat: 34.5577, lng: 38.2753, temp: '28°C', state: 'شديد الحرارة صحو', wind: '25 كم/س' }
                ];

                syrianCities.forEach(function(c) {
                    var circle = L.circleMarker([c.lat, c.lng], {
                        color: '#38BDF8',
                        fillColor: '$overlayColor',
                        fillOpacity: 0.9,
                        radius: 8
                    }).addTo(map);

                    circle.bindPopup(
                        '<div class="city-popup">' +
                        '<div class="city-title">' + c.name + '</div>' +
                        '<b>الحرارة:</b> ' + c.temp + '<br/>' +
                        '<b>حالة الطقس:</b> ' + c.state + '<br/>' +
                        '<b>سرعة الرياح:</b> ' + c.wind +
                        '</div>'
                    );
                });
            </script>
        </body>
        </html>
    """.trimIndent()
}
