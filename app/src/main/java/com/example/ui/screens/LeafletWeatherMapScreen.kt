package com.example.ui.screens

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
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
import androidx.compose.material.icons.automirrored.filled.*
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
import com.example.data.model.CityMapForecast
import org.json.JSONArray
import org.json.JSONObject
import com.example.data.model.MapWeatherOverlay
import com.example.ui.components.LeafletBaseMapConfigs
import com.example.ui.components.RainViewerFramePolicy

class LeafletJsBridge(
    private val onCityClicked: (cityName: String, temp: String, state: String, lat: Double, lng: Double) -> Unit
) {
    private val knownCities = mapOf(
        "دمشق" to (33.5138 to 36.2765),
        "حلب" to (36.2021 to 37.1343),
        "اللاذقية" to (35.5317 to 35.7915),
        "حمص" to (34.7324 to 36.7137),
        "طرطوس" to (34.8890 to 35.8866),
        "حماة" to (35.1318 to 36.7578),
        "دير الزور" to (35.3333 to 40.1500),
        "القامشلي" to (37.0494 to 41.2268),
        "السويداء" to (32.7130 to 36.5694),
        "درعا" to (32.6186 to 36.1011),
        "إدلب" to (35.9306 to 36.6339),
        "الرقة" to (35.9556 to 39.0078),
        "تدمر" to (34.5577 to 38.2753)
    )

    @JavascriptInterface
    fun onCitySelectedFromJs(cityName: String, temp: String, state: String, lat: Double, lng: Double) {
        val coordinates = knownCities[cityName] ?: return
        if (!lat.isFinite() || !lng.isFinite()) return
        if (lat !in (coordinates.first - 0.001)..(coordinates.first + 0.001)) return
        if (lng !in (coordinates.second - 0.001)..(coordinates.second + 0.001)) return

        // The map currently contains no verified city observations. Ignore values supplied by JS.
        onCityClicked(cityName, "—", "بيانات غير متاحة", coordinates.first, coordinates.second)
    }
}

internal fun isMapOverlayAvailable(
    overlay: MapWeatherOverlay,
    forecasts: List<CityMapForecast>
): Boolean = when (overlay) {
    MapWeatherOverlay.PRECIPITATION -> true // Radar availability is checked independently.
    MapWeatherOverlay.TEMPERATURE -> forecasts.any { it.temperatureCelsius != null }
    MapWeatherOverlay.WIND -> forecasts.any { it.windSpeedKmh != null }
    MapWeatherOverlay.CLOUDS -> forecasts.any { it.cloudCoverPercent != null }
}

internal fun shouldReloadLeafletHtml(previousHtml: String?, currentHtml: String): Boolean =
    previousHtml != currentHtml

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LeafletWeatherMapScreen(
    currentCity: CityLocation,
    cityForecasts: List<CityMapForecast>,
    isLoadingCityForecasts: Boolean,
    cityForecastError: String?,
    onRefreshCityForecasts: () -> Unit,
    onCitySelected: (CityLocation) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTileType by remember { mutableStateOf("dark") }
    var selectedOverlay by remember { mutableStateOf(MapWeatherOverlay.PRECIPITATION) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var inspectedCityInfo by remember { mutableStateOf<String?>(null) }
    var inspectedCityLocation by remember { mutableStateOf<CityLocation?>(null) }
    val latestCityForecasts by rememberUpdatedState(cityForecasts)

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

    val htmlContent = remember(selectedTileType, selectedOverlay, cityForecasts) {
        generateLeafletScreenHtml(selectedTileType, selectedOverlay, cityForecasts)
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
                            text = "خرائط أساسية خارجية وطبقة رادار RainViewer",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
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
                        val isAvailable = isMapOverlayAvailable(overlay, cityForecasts)
                        val isSelected = selectedOverlay == overlay && isAvailable
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) Color(0xFF0284C7)
                                    else Color.White.copy(alpha = if (isAvailable) 0.1f else 0.045f)
                                )
                                .clickable(enabled = isAvailable) {
                                    selectedOverlay = overlay
                                    val overlayName = overlay.name.lowercase()
                                    webViewRef?.evaluateJavascript("switchWeatherOverlay('$overlayName')", null)
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
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

            Text(
                text = when {
                    cityForecasts.isNotEmpty() -> "توقعات Open-Meteo للمدن وليست رصداً من محطات. وقت التحديث: ${cityForecasts.firstOrNull()?.time ?: "غير متاح"}"
                    isLoadingCityForecasts -> "جارٍ تحميل توقعات المدن من Open-Meteo..."
                    cityForecastError != null -> "تعذر تحميل توقعات المدن: $cityForecastError"
                    else -> "لم تُحمّل توقعات المدن بعد. الرادار التاريخي من RainViewer."
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                fontSize = 10.sp,
                color = Color(0xFFFACC15)
            )

            if (cityForecasts.isEmpty()) {
                TextButton(onClick = onRefreshCityForecasts, enabled = !isLoadingCityForecasts) {
                    Text(if (isLoadingCityForecasts) "جارٍ التحديث..." else "تحديث توقعات المدن")
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
                        settings.javaScriptCanOpenWindowsAutomatically = false
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                            settings.domStorageEnabled = true
                                settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            settings.builtInZoomControls = false
                            settings.setSupportZoom(true)
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW

                            addJavascriptInterface(
                                LeafletJsBridge { cityName, _, _, lat, lng ->
                                    // Weather values are resolved from the Kotlin model, never from JavaScript.
                                    val forecast = latestCityForecasts.firstOrNull { it.city.name == cityName }
                                    val details = forecast?.let {
                                        listOfNotNull(
                                            it.temperatureCelsius?.let { value -> "الحرارة ${"%.1f".format(value)}°م" },
                                            it.humidityPercent?.let { value -> "الرطوبة $value%" },
                                            it.windSpeedKmh?.let { value -> "الرياح ${"%.1f".format(value)} كم/س" },
                                            it.cloudCoverPercent?.let { value -> "السحب $value%" },
                                            it.precipitationMm?.let { value -> "الهطول ${"%.1f".format(value)} مم" }
                                        ).joinToString(" | ")
                                    }.orEmpty()
                                    inspectedCityInfo = if (details.isBlank()) {
                                        "$cityName: توقعات المدينة غير متاحة"
                                    } else {
                                        "$cityName: $details"
                                    }
                                    inspectedCityLocation = forecast?.city ?: CityLocation(
                                        name = cityName,
                                        country = "سوريا",
                                        latitude = lat,
                                        longitude = lng
                                    )
                                },
                                "AndroidMapInterface"
                            )

                            // The map is a self-contained page. Block top-level navigation so
                            // remote tile, metadata, or attribution links cannot replace the app map.
                            // This does not block HTTPS subresources (Leaflet, tiles, RainViewer).
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
                            tag = htmlContent
                            webViewRef = this
                        }
                    },
                    update = { webView ->
                        webViewRef = webView
                        // AndroidView.update runs after recomposition. Reload only when the
                        // generated document changes (forecast, base map, or selected overlay).
                        if (shouldReloadLeafletHtml(webView.tag as? String, htmlContent)) {
                            webView.loadDataWithBaseURL(
                                "https://openstreetmap.org",
                                htmlContent,
                                "text/html",
                                "UTF-8",
                                null
                            )
                            webView.tag = htmlContent
                        }
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

private fun generateLeafletScreenHtml(tileType: String, overlay: MapWeatherOverlay, cityForecasts: List<CityMapForecast>): String {
    val baseMap = LeafletBaseMapConfigs.forStyle(tileType)
    val subdomainsOption = baseMap.subdomains?.let { "subdomains: '$it'," }.orEmpty()
    val cityForecastsJson = JSONArray().apply {
        cityForecasts.forEach { forecast ->
            put(JSONObject().apply {
                put("name", forecast.city.name)
                put("lat", forecast.city.latitude)
                put("lng", forecast.city.longitude)
                put("time", forecast.time ?: JSONObject.NULL)
                put("temp", forecast.temperatureCelsius ?: JSONObject.NULL)
                put("humidity", forecast.humidityPercent ?: JSONObject.NULL)
                put("wind", forecast.windSpeedKmh ?: JSONObject.NULL)
                put("windDirection", forecast.windDirectionDegrees ?: JSONObject.NULL)
                put("clouds", forecast.cloudCoverPercent ?: JSONObject.NULL)
                put("precip", forecast.precipitationMm ?: JSONObject.NULL)
                put("code", forecast.weatherCode ?: JSONObject.NULL)
            })
        }
    }.toString()

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
                .radar-timeline {
                    position: absolute;
                    z-index: 1000;
                    left: 50%;
                    bottom: 12px;
                    transform: translateX(-50%);
                    display: flex;
                    align-items: center;
                    gap: 6px;
                    width: min(94%, 520px);
                    box-sizing: border-box;
                    padding: 8px;
                    border: 1px solid rgba(56, 189, 248, 0.8);
                    border-radius: 12px;
                    background: rgba(15, 23, 42, 0.96);
                    color: #fff;
                    direction: rtl;
                    font: 11px system-ui, sans-serif;
                }
                .radar-timeline[hidden] { display: none; }
                .radar-timeline button {
                    flex: 0 0 auto;
                    min-width: 34px;
                    min-height: 30px;
                    border: 1px solid #475569;
                    border-radius: 8px;
                    background: #1e293b;
                    color: #fff;
                    font: inherit;
                    cursor: pointer;
                }
                .radar-timeline button:disabled { opacity: 0.45; cursor: default; }
                .radar-timeline input[type="range"] { flex: 1 1 auto; min-width: 45px; accent-color: #38bdf8; }
                .radar-timeline .radar-time-label { flex: 0 0 auto; max-width: 108px; text-align: center; font-size: 10px; }

            </style>
        </head>
        <body>
                        <div id="map"></div>
            <div id="radar-timeline" class="radar-timeline" hidden>
                <button id="radar-prev" type="button" aria-label="الإطار السابق">‹</button>
                <button id="radar-play" type="button" aria-label="تشغيل الحركة">تشغيل</button>
                <input id="radar-frame-slider" type="range" min="0" max="0" value="0" step="1" aria-label="اختيار إطار الرادار" />
                <button id="radar-next" type="button" aria-label="الإطار التالي">›</button>
                <span id="radar-time-label" class="radar-time-label">—</span>
            </div>
            <script>
                var map = L.map('map', {
                    center: [34.8021, 38.9968],
                    zoom: 7,
                    minZoom: 5,
                    maxZoom: 13,
                    zoomControl: false
                });

                var baseTileLayer = L.tileLayer('${baseMap.urlTemplate}', {
                    attribution: '${baseMap.attributionHtml}',
                    $subdomainsOption
                    maxZoom: ${baseMap.maxZoom}
                }).addTo(map);

                var radarLayer = null;
                var selectedWeatherOverlay = '${overlay.name.lowercase()}';
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
                        if (!data.host || !Array.isArray(frames) || frames.length === 0) {
                            throw new Error('No historical radar frames are available');
                        }

                        // Accept only HTTPS tile hosts owned by RainViewer and known radar paths.
                        var radarHost = new URL(data.host);
                        var trustedHostSuffix = '${RainViewerFramePolicy.HOST_SUFFIX}';
                        var trustedHost = radarHost.protocol === 'https:' &&
                            (radarHost.hostname === trustedHostSuffix ||
                             radarHost.hostname.endsWith('.' + trustedHostSuffix)) &&
                            radarHost.username === '' && radarHost.password === '' &&
                            (radarHost.port === '' || radarHost.port === '443') &&
                            (radarHost.pathname === '' || radarHost.pathname === '/') &&
                            radarHost.search === '' && radarHost.hash === '';
                        if (!trustedHost) throw new Error('Untrusted radar tile host');

                        var validFrames = frames.filter(function(candidate) {
                            return candidate &&
                                typeof candidate.path === 'string' &&
                                new RegExp('${RainViewerFramePolicy.PATH_PATTERN}').test(candidate.path) &&
                                Number.isFinite(candidate.time) &&
                                candidate.time > 0 &&
                                candidate.time * 1000 <= Date.now() + 5 * 60 * 1000;
                        });
                        if (validFrames.length === 0) throw new Error('No valid radar frames are available');

                        validFrames.sort(function(a, b) { return a.time - b.time; });
                        var timeline = document.getElementById('radar-timeline');
                        var frameSlider = document.getElementById('radar-frame-slider');
                        var timeLabel = document.getElementById('radar-time-label');
                        var playButton = document.getElementById('radar-play');
                        var previousButton = document.getElementById('radar-prev');
                        var nextButton = document.getElementById('radar-next');
                        var selectedFrameIndex = validFrames.length - 1;
                        var playbackTimer = null;
                        var timelineEnabled = selectedWeatherOverlay === 'precipitation';

                        function frameTileUrl(candidate) {
                            return radarHost.origin + candidate.path + '/256/{z}/{x}/{y}/2/1_1.png';
                        }
                        function stopPlayback() {
                            if (playbackTimer !== null) {
                                window.clearInterval(playbackTimer);
                                playbackTimer = null;
                            }
                            playButton.textContent = 'تشغيل';
                            playButton.setAttribute('aria-label', 'تشغيل الحركة');
                        }
                        function showRadarFrame(index) {
                            selectedFrameIndex = Math.max(0, Math.min(index, validFrames.length - 1));
                            var selectedFrame = validFrames[selectedFrameIndex];
                            radarLayer.setUrl(frameTileUrl(selectedFrame));
                            frameSlider.value = String(selectedFrameIndex);
                            var selectedTime = new Date(selectedFrame.time * 1000);
                            timeLabel.textContent = selectedTime.toLocaleTimeString('ar', {
                                hour: '2-digit', minute: '2-digit'
                            });
                            var ageMs = Date.now() - selectedFrame.time * 1000;
                            var staleSuffix = ageMs > 2 * 60 * 60 * 1000
                                ? ' — الإطار قديم، تحقق من وقت التحديث' : '';
                            radarStatusControl.getContainer().textContent =
                                'إطار راداري تاريخي: ' + selectedTime.toLocaleString('ar') + staleSuffix;
                            previousButton.disabled = selectedFrameIndex === 0;
                            nextButton.disabled = selectedFrameIndex === validFrames.length - 1;
                        }

                        frameSlider.max = String(validFrames.length - 1);
                        frameSlider.value = String(selectedFrameIndex);
                        timeline.hidden = !timelineEnabled || validFrames.length < 2;
                        radarLayer = L.tileLayer(frameTileUrl(validFrames[selectedFrameIndex]), {
                            opacity: 0.68,
                            attribution: 'Weather data by <a href="https://www.rainviewer.com/" target="_blank" rel="noopener">RainViewer</a>',
                            maxZoom: 7
                        });
                        if (selectedWeatherOverlay === 'precipitation') radarLayer.addTo(map);
                        showRadarFrame(selectedFrameIndex);

                        frameSlider.addEventListener('input', function() {
                            stopPlayback();
                            showRadarFrame(Number(frameSlider.value));
                        });
                        previousButton.addEventListener('click', function() {
                            stopPlayback();
                            showRadarFrame(selectedFrameIndex - 1);
                        });
                        nextButton.addEventListener('click', function() {
                            stopPlayback();
                            showRadarFrame(selectedFrameIndex + 1);
                        });
                        playButton.addEventListener('click', function() {
                            if (playbackTimer !== null) {
                                stopPlayback();
                                return;
                            }
                            if (selectedFrameIndex >= validFrames.length - 1) showRadarFrame(0);
                            playButton.textContent = 'إيقاف';
                            playButton.setAttribute('aria-label', 'إيقاف الحركة');
                            playbackTimer = window.setInterval(function() {
                                if (selectedFrameIndex >= validFrames.length - 1) {
                                    stopPlayback();
                                    return;
                                }
                                showRadarFrame(selectedFrameIndex + 1);
                            }, 900);
                        });
                    })
                    .catch(function(error) {
                        radarStatusControl.getContainer().textContent =
                            'بيانات الرادار غير متاحة حالياً؛ لا يعني ذلك عدم وجود أمطار.';
                        console.warn('RainViewer radar unavailable', error);
                    });



                // Forecasts are model output, not station observations.
                var syrianCities = ${cityForecastsJson};
                var cityMarkers = [];

                function forecastValueColor(value, overlayType) {
                    if (value === null || value === undefined || !Number.isFinite(Number(value))) return '#64748B';
                    value = Number(value);
                    if (overlayType === 'temperature') {
                        if (value < 0) return '#2563EB';
                        if (value < 10) return '#06B6D4';
                        if (value < 25) return '#22C55E';
                        if (value < 35) return '#F59E0B';
                        return '#EF4444';
                    }
                    if (overlayType === 'wind') {
                        if (value < 10) return '#22C55E';
                        if (value < 20) return '#84CC16';
                        if (value < 35) return '#F59E0B';
                        return '#EF4444';
                    }
                    if (overlayType === 'clouds') {
                        if (value < 25) return '#FACC15';
                        if (value < 50) return '#94A3B8';
                        if (value < 75) return '#64748B';
                        return '#334155';
                    }
                    if (overlayType === 'precipitation') {
                        if (value < 0.1) return '#94A3B8';
                        if (value < 1) return '#38BDF8';
                        if (value < 5) return '#2563EB';
                        return '#7C3AED';
                    }
                    return '#0284C7';
                }

                function cityOverlayValue(city, overlayType) {
                    if (overlayType === 'temperature') return city.temp;
                    if (overlayType === 'wind') return city.wind;
                    if (overlayType === 'clouds') return city.clouds;
                    if (overlayType === 'precipitation') return city.precip;
                    return null;
                }

                function styleCityMarkers(overlayType) {
                    cityMarkers.forEach(function(entry) {
                        var color = forecastValueColor(cityOverlayValue(entry.city, overlayType), overlayType);
                        entry.marker.setStyle({ color: color, fillColor: color, fillOpacity: 0.92 });
                    });
                    var legend = document.getElementById('forecast-overlay-legend');
                    if (legend) {
                        var titles = {
                            temperature: 'ألوان الحرارة المتوقعة (°م)',
                            wind: 'ألوان سرعة الرياح المتوقعة (كم/س)',
                            clouds: 'ألوان الغطاء السحابي المتوقع (%)',
                            precipitation: 'ألوان الهطول المتوقع (مم)'
                        };
                        legend.textContent = titles[overlayType] || 'ألوان توقعات المدن';
                    }
                }

                var forecastLegendControl = L.control({ position: 'bottomright' });
                forecastLegendControl.onAdd = function() {
                    var div = L.DomUtil.create('div', 'radar-status');
                    div.id = 'forecast-overlay-legend';
                    div.textContent = 'ألوان الهطول المتوقع (مم)';
                    return div;
                };
                forecastLegendControl.addTo(map);

                syrianCities.forEach(function(c) {
                    var markerColor = forecastValueColor(c.precip, 'precipitation');
                    var marker = L.circleMarker([c.lat, c.lng], {
                        color: markerColor,
                        fillColor: markerColor,
                        fillOpacity: 0.92,
                        radius: 8
                    }).addTo(map);
                    cityMarkers.push({ city: c, marker: marker });

                    marker.on('click', function() {
                        if (window.AndroidMapInterface && window.AndroidMapInterface.onCitySelectedFromJs) {
                            // Only city identity and fixed coordinates cross the bridge.
                            window.AndroidMapInterface.onCitySelectedFromJs(c.name, '', '', c.lat, c.lng);
                        }
                    });

                    var tempText = Number.isFinite(c.temp) ? c.temp.toFixed(1) + ' °م' : '—';
                    var humidityText = Number.isFinite(c.humidity) ? c.humidity + '%' : '—';
                    var windText = Number.isFinite(c.wind) ? c.wind.toFixed(1) + ' كم/س' : '—';
                    var cloudText = Number.isFinite(c.clouds) ? c.clouds + '%' : '—';
                    marker.bindPopup(
                        '<div class="city-popup">' +
                        '<div class="city-title">' + c.name + '</div>' +
                        '<b>الحرارة المتوقعة:</b> ' + tempText + '<br/>' +
                        '<b>الرطوبة:</b> ' + humidityText + '<br/>' +
                        '<b>الرياح:</b> ' + windText + '<br/>' +
                        '<b>الغطاء السحابي:</b> ' + cloudText + '<br/>' +
                        '<small>توقعات نموذج Open-Meteo وليست رصداً لمحطة</small>' +
                        '</div>'
                    );
                });

                styleCityMarkers(selectedWeatherOverlay);

                // Bidirectional Javascript Functions called from Android Kotlin
                function flyToCoordinate(lat, lng, zoomLevel) {
                    map.flyTo([lat, lng], zoomLevel, { duration: 1.2 });
                }

                function flyToSyria() {
                    map.flyTo([34.8021, 38.9968], 7, { duration: 1.2 });
                }

                function switchTileLayer(style) {
                    var source;
                    if (style === 'satellite') {
                        source = {
                            url: 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}',
                            attribution: 'Tiles &copy; Esri — Source: Esri, Maxar, Earthstar Geographics, and the GIS User Community',
                            maxZoom: 19
                        };
                    } else if (style === 'osm') {
                        source = {
                            url: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
                            attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap contributors</a>',
                            maxZoom: 19
                        };
                    } else {
                        source = {
                            url: 'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png',
                            attribution: '&copy; OpenStreetMap contributors &copy; CARTO',
                            subdomains: 'abcd',
                            maxZoom: 20
                        };
                    }
                    map.removeLayer(baseTileLayer);
                    var options = { attribution: source.attribution, maxZoom: source.maxZoom };
                    if (source.subdomains) options.subdomains = source.subdomains;
                    baseTileLayer = L.tileLayer(source.url, options).addTo(map);
                }

                function switchWeatherOverlay(overlayType) {
                    selectedWeatherOverlay = overlayType;
                    if (overlayType === 'precipitation') {
                        if (radarLayer && !map.hasLayer(radarLayer)) radarLayer.addTo(map);
                    } else if (radarLayer && map.hasLayer(radarLayer)) {
                        map.removeLayer(radarLayer);
                    }
                    // Marker colors are forecast-based, not raster weather layers.
                    styleCityMarkers(overlayType);
                }
            </script>
        </body>
        </html>
    """.trimIndent()
}
