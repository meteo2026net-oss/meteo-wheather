package com.example.ui.components

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
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
                        text = "خريطة أساسية + طبقة رادار RainViewer",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
            }
        }

        Text(
            text = "تنبيه: طبقة RainViewer تعرض آخر إطار تاريخي متاح فقط؛ درجات الحرارة والرياح في علامات المدن غير متاحة.",
            fontSize = 10.sp,
            color = Color(0xFFFACC15)
        )
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
                val isAvailable = overlay == MapWeatherOverlay.PRECIPITATION
                val isSelected = selectedOverlay == overlay && isAvailable
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) Color(0xFF0284C7).copy(alpha = 0.85f)
                            else Color.White.copy(alpha = if (isAvailable) 0.12f else 0.05f)
                        )
                        .border(
                            1.dp,
                            if (isSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = if (isAvailable) 0.15f else 0.07f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable(enabled = isAvailable) {
                            selectedOverlay = overlay
                        }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isAvailable) overlay.titleAr else "${overlay.titleAr} (قريباً)",
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = Color.White.copy(alpha = if (isAvailable) 1f else 0.45f)
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
                        settings.javaScriptCanOpenWindowsAutomatically = false
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.builtInZoomControls = false
                        settings.setSupportZoom(true)
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        // Keep the generated map document in the WebView; block external main-frame navigation.
                        // HTTPS subresources (Leaflet, map tiles and RainViewer) remain available.
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                return request?.isForMainFrame == true
                            }
                        }
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

        // The provider currently exposes historical frames only.
        Text(
            text = "الزمن: أحدث إطار تاريخي متاح من RainViewer؛ لا توجد حركة تنبؤية مستقبلية في هذه النسخة.",
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            fontSize = 11.sp,
            color = Color(0xFFFACC15)
        )

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
                .radar-status {
                    background: rgba(15, 23, 42, 0.92);
                    color: #fff;
                    border: 1px solid #38BDF8;
                    border-radius: 8px;
                    padding: 6px 9px;
                    font-size: 11px;
                    direction: rtl;
                    max-width: 260px;
                }
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
                    attribution: '&copy; OpenStreetMap contributors / CARTO / Esri',
                    subdomains: 'abcd',
                    maxZoom: 18
                }).addTo(map);

                var radarLayer = null;
                var radarStatusControl = L.control({ position: 'bottomleft' });
                radarStatusControl.onAdd = function() {
                    var div = L.DomUtil.create('div', 'radar-status');
                    div.textContent = 'جارٍ تحميل بيانات الرادار التاريخية...';
                    return div;
                };
                radarStatusControl.addTo(map);

                // RainViewer's public API now provides historical frames only.
                // The provider discontinued public future radar frames in January 2026.
                fetch('https://api.rainviewer.com/public/weather-maps.json')
                    .then(function(response) {
                        if (!response.ok) throw new Error('RainViewer metadata request failed');
                        return response.json();
                    })
                    .then(function(data) {
                        var frames = data && data.radar && data.radar.past;
                        if (!data.host || !frames || frames.length === 0) {
                            throw new Error('No historical radar frames are available');
                        }
                        var frame = frames[frames.length - 1];
                        var tileUrl = data.host + frame.path + '/256/{z}/{x}/{y}/2/1_1.png';
                        radarLayer = L.tileLayer(tileUrl, {
                            opacity: 0.65,
                            attribution: 'Weather data by <a href="https://www.rainviewer.com/" target="_blank" rel="noopener">RainViewer</a>',
                            maxZoom: 7
                        });
                        radarLayer.addTo(map);
                        var frameTime = new Date(frame.time * 1000);
                        radarStatusControl.getContainer().textContent =
                            'آخر إطار تاريخي متاح: ' + frameTime.toLocaleString('ar');
                    })
                    .catch(function(error) {
                        radarStatusControl.getContainer().textContent =
                            'بيانات الرادار غير متاحة حالياً؛ لا يعني ذلك عدم وجود أمطار.';
                        console.warn('RainViewer radar unavailable', error);
                    });



                // Syrian Cities Meteorological Pins
                var syrianCities = [
                    { name: 'دمشق (العاصمة)', lat: 33.5138, lng: 36.2765, temp: '—', state: 'بيانات غير متاحة', wind: '—' },
                    { name: 'حلب', lat: 36.2021, lng: 37.1343, temp: '—', state: 'بيانات غير متاحة', wind: '—' },
                    { name: 'اللاذقية (الساحل)', lat: 35.5317, lng: 35.7915, temp: '—', state: 'بيانات غير متاحة', wind: '—' },
                    { name: 'حمص', lat: 34.7324, lng: 36.7137, temp: '—', state: 'بيانات غير متاحة', wind: '—' },
                    { name: 'طرطوس', lat: 34.8890, lng: 35.8866, temp: '—', state: 'بيانات غير متاحة', wind: '—' },
                    { name: 'حماة', lat: 35.1318, lng: 36.7578, temp: '—', state: 'بيانات غير متاحة', wind: '—' },
                    { name: 'دير الزور (المنطقة الشرقية)', lat: 35.3333, lng: 40.1500, temp: '—', state: 'بيانات غير متاحة', wind: '—' },
                    { name: 'القامشلي', lat: 37.0494, lng: 41.2268, temp: '—', state: 'بيانات غير متاحة', wind: '—' },
                    { name: 'السويداء (المنطقة الجنوبية)', lat: 32.7130, lng: 36.5694, temp: '—', state: 'بيانات غير متاحة', wind: '—' },
                    { name: 'درعا', lat: 32.6186, lng: 36.1011, temp: '—', state: 'بيانات غير متاحة', wind: '—' },
                    { name: 'إدلب', lat: 35.9306, lng: 36.6339, temp: '—', state: 'بيانات غير متاحة', wind: '—' },
                    { name: 'الرقة', lat: 35.9556, lng: 39.0078, temp: '—', state: 'بيانات غير متاحة', wind: '—' },
                    { name: 'تدمر (البادية السورية)', lat: 34.5577, lng: 38.2753, temp: '—', state: 'بيانات غير متاحة', wind: '—' }
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
