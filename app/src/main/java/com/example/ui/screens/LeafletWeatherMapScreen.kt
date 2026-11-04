package com.example.ui.screens

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.CityLocation
import com.example.data.model.MapWeatherOverlay

class LeafletJsBridge(
    private val onCityClicked: (cityName: String, temp: String, state: String, lat: Double, lng: Double) -> Unit
) {
    @JavascriptInterface
    fun onCitySelectedFromJs(cityName: String, temp: String, state: String, lat: Double, lng: Double) {
        onCityClicked(cityName, temp, state, lat, lng)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LeafletWeatherMapScreen(
    currentCity: CityLocation,
    onCitySelected: (CityLocation) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTileType by remember { mutableStateOf("dark") }
    var selectedOverlay by remember { mutableStateOf(MapWeatherOverlay.PRECIPITATION) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var inspectedCityInfo by remember { mutableStateOf<String?>(null) }
    var inspectedCityLocation by remember { mutableStateOf<CityLocation?>(null) }

    val syrianCitiesList = remember {
        listOf(
            CityLocation("دمشق", "Damascus", "سوريا", 33.5138, 36.2765, "محافظة دمشق"),
            CityLocation("حلب", "Aleppo", "سوريا", 36.2021, 37.1343, "محافظة حلب"),
            CityLocation("اللاذقية", "Latakia", "سوريا", 35.5317, 35.7915, "محافظة اللاذقية"),
            CityLocation("حمص", "Homs", "سوريا", 34.7324, 36.7137, "محافظة حمص"),
            CityLocation("طرطوس", "Tartus", "سوريا", 34.8890, 35.8866, "محافظة طرطوس"),
            CityLocation("حماة", "Hama", "سوريا", 35.1318, 36.7578, "محافظة حماة"),
            CityLocation("دير الزور", "Deir ez-Zor", "سوريا", 35.3333, 40.1500, "محافظة دير الزور"),
            CityLocation("القامشلي", "Qamishli", "سوريا", 37.0494, 41.2268, "محافظة الحسكة"),
            CityLocation("السويداء", "Sweida", "سوريا", 32.7130, 36.5694, "محافظة السويداء"),
            CityLocation("درعا", "Deraa", "سوريا", 32.6186, 36.1011, "محافظة درعا"),
            CityLocation("إدلب", "Idlib", "سوريا", 35.9306, 36.6339, "محافظة إدلب"),
            CityLocation("الرقة", "Raqqa", "سوريا", 35.9556, 39.0078, "محافظة الرقة"),
            CityLocation("تدمر", "Palmyra", "سوريا", 34.5577, 38.2753, "محافظة حمص")
        )
    }

    val htmlContent = remember(selectedTileType, selectedOverlay) {
        generateLeafletScreenHtml(selectedTileType, selectedOverlay)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "خريطة Leaflet الجوية الشاملة",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "المركز الوطني للأرصاد الجوية والمناخ (MCNC)",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "رجوع",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            webViewRef?.evaluateJavascript("flyToSyria()", null)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "التركيز على سوريا",
                            tint = Color(0xFF38BDF8)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A)
                )
            )
        },
        containerColor = Color(0xFF090D1A)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("leaflet_weather_map_screen")
        ) {
            // Map Style & Layer Control Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tile Style Switcher
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val tileOptions = listOf(
                        Triple("dark", "🗺️ داكنة", "الخريطة الليلية"),
                        Triple("satellite", "🛰️ أقمار", "الخريطة الفضائية"),
                        Triple("osm", "🌍 شوارع", "خريطة الشوارع")
                    )
                    tileOptions.forEach { (type, label, _) ->
                        val isSelected = selectedTileType == type
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFF0284C7) else Color.White.copy(alpha = 0.1f))
                                .clickable {
                                    selectedTileType = type
                                    webViewRef?.evaluateJavascript("switchTileLayer('$type')", null)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = Color.White
                            )
                        }
                    }
                }

                // Overlay Chips
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    MapWeatherOverlay.values().forEach { overlay ->
                        val isSelected = selectedOverlay == overlay
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFF0284C7) else Color.White.copy(alpha = 0.1f))
                                .clickable {
                                    selectedOverlay = overlay
                                    val overlayName = overlay.name.lowercase()
                                    webViewRef?.evaluateJavascript("switchWeatherOverlay('$overlayName')", null)
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
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
            }

            // Quick Syrian Governorates Selector Bar
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B))
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(syrianCitiesList) { city ->
                    val isSelected = currentCity.name == city.name
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.12f)
                            )
                            .border(
                                1.dp,
                                if (isSelected) Color(0xFF38BDF8) else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                onCitySelected(city)
                                webViewRef?.evaluateJavascript("flyToCoordinate(${city.latitude}, ${city.longitude}, 10)", null)
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = city.name,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color(0xFF0F172A) else Color.White
                        )
                    }
                }
            }

            // Interactive Leaflet WebView Frame
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
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

                            addJavascriptInterface(
                                LeafletJsBridge { cityName, temp, state, lat, lng ->
                                    inspectedCityInfo = "$cityName: $temp | $state"
                                    inspectedCityLocation = CityLocation(
                                        name = cityName,
                                        country = "سوريا",
                                        latitude = lat,
                                        longitude = lng
                                    )
                                },
                                "AndroidMapInterface"
                            )

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
                            webViewRef = this
                        }
                    },
                    update = { webView ->
                        webViewRef = webView
                    }
                )

                // Inspected City Floating Action Card
                inspectedCityLocation?.let { city ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                            .fillMaxWidth(0.92f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xF00F172A))
                            .border(1.5.dp, Color(0xFF0284C7), RoundedCornerShape(18.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = city.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = inspectedCityInfo ?: "",
                                    fontSize = 12.sp,
                                    color = Color(0xFFFFD54F)
                                )
                            }

                            Button(
                                onClick = {
                                    onCitySelected(city)
                                    onBackClick()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(text = "عرض طقس المدينة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun generateLeafletScreenHtml(tileType: String, overlay: MapWeatherOverlay): String {
    val baseTileUrl = when (tileType) {
        "satellite" -> "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"
        "osm" -> "https://tile.openstreetmap.org/{z}/{x}/{y}.png"
        else -> "https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
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
                .city-popup { text-align: right; direction: rtl; font-size: 13px; line-height: 1.6; }
                .city-title { font-size: 15px; font-weight: bold; color: #38BDF8; margin-bottom: 4px; }
                .leaflet-popup-content-wrapper { background: #0F172A; color: #FFF; border-radius: 12px; border: 1.5px solid #0284C7; box-shadow: 0 4px 14px rgba(0,0,0,0.6); }
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
                    maxZoom: 13,
                    zoomControl: false
                });

                var baseTileLayer = L.tileLayer('$baseTileUrl', {
                    attribution: '&copy; OpenStreetMap & MCNC Syria',
                    maxZoom: 18
                }).addTo(map);

                // RainViewer Real-Time Radar Layer Overlay
                var radarLayer = L.tileLayer('https://tile.rainviewer.com/v2/radar/nowcast_latest/256/{z}/{x}/{y}/2/1_1.png', {
                    opacity: 0.65,
                    maxZoom: 18
                }).addTo(map);

                // Syrian Cities Data
                var syrianCities = [
                    { name: 'دمشق', lat: 33.5138, lng: 36.2765, temp: '18°C', state: 'مشمس وصحو' },
                    { name: 'حلب', lat: 36.2021, lng: 37.1343, temp: '22°C', state: 'غائم جزئياً' },
                    { name: 'اللاذقية', lat: 35.5317, lng: 35.7915, temp: '20°C', state: 'ماطر وخفيف' },
                    { name: 'حمص', lat: 34.7324, lng: 36.7137, temp: '19°C', state: 'معتدل ولطيف' },
                    { name: 'طرطوس', lat: 34.8890, lng: 35.8866, temp: '21°C', state: 'زخات مطرية' },
                    { name: 'حماة', lat: 35.1318, lng: 36.7578, temp: '21°C', state: 'مشمس' },
                    { name: 'دير الزور', lat: 35.3333, lng: 40.1500, temp: '26°C', state: 'دافئ وجاف' },
                    { name: 'القامشلي', lat: 37.0494, lng: 41.2268, temp: '24°C', state: 'صحو' },
                    { name: 'السويداء', lat: 32.7130, lng: 36.5694, temp: '17°C', state: 'بارد نسبياً' },
                    { name: 'درعا', lat: 32.6186, lng: 36.1011, temp: '19°C', state: 'مشمس' },
                    { name: 'إدلب', lat: 35.9306, lng: 36.6339, temp: '20°C', state: 'غائم' },
                    { name: 'الرقة', lat: 35.9556, lng: 39.0078, temp: '25°C', state: 'صحو' },
                    { name: 'تدمر', lat: 34.5577, lng: 38.2753, temp: '28°C', state: 'شديد الحرارة' }
                ];

                syrianCities.forEach(function(c) {
                    var marker = L.circleMarker([c.lat, c.lng], {
                        color: '#38BDF8',
                        fillColor: '#0284C7',
                        fillOpacity: 0.9,
                        radius: 8
                    }).addTo(map);

                    marker.on('click', function() {
                        if (window.AndroidMapInterface && window.AndroidMapInterface.onCitySelectedFromJs) {
                            window.AndroidMapInterface.onCitySelectedFromJs(c.name, c.temp, c.state, c.lat, c.lng);
                        }
                    });

                    marker.bindPopup(
                        '<div class="city-popup">' +
                        '<div class="city-title">' + c.name + '</div>' +
                        '<b>درجة الحرارة:</b> ' + c.temp + '<br/>' +
                        '<b>حالة الطقس:</b> ' + c.state +
                        '</div>'
                    );
                });

                // Bidirectional Javascript Functions called from Android Kotlin
                function flyToCoordinate(lat, lng, zoomLevel) {
                    map.flyTo([lat, lng], zoomLevel, { duration: 1.2 });
                }

                function flyToSyria() {
                    map.flyTo([34.8021, 38.9968], 7, { duration: 1.2 });
                }

                function switchTileLayer(style) {
                    var newUrl = 'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png';
                    if (style === 'satellite') {
                        newUrl = 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}';
                    } else if (style === 'osm') {
                        newUrl = 'https://tile.openstreetmap.org/{z}/{x}/{y}.png';
                    }
                    map.removeLayer(baseTileLayer);
                    baseTileLayer = L.tileLayer(newUrl, { maxZoom: 18 }).addTo(map);
                }

                function switchWeatherOverlay(overlayType) {
                    map.removeLayer(radarLayer);
                    radarLayer = L.tileLayer('https://tile.rainviewer.com/v2/radar/nowcast_latest/256/{z}/{x}/{y}/2/1_1.png', {
                        opacity: 0.65,
                        maxZoom: 18
                    }).addTo(map);
                }
            </script>
        </body>
        </html>
    """.trimIndent()
}
