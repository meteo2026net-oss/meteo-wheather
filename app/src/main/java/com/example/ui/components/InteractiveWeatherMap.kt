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
import com.example.data.model.CityMapForecast
import com.example.data.model.MapWeatherOverlay
import com.example.data.model.WeatherCodeMapping
import com.example.ui.screens.encodeJsonForHtmlScript
import org.json.JSONArray
import org.json.JSONObject

enum class LeafletTileStyle(val titleAr: String, val icon: String, val tileType: String) {
    DARK("خريطة ليفليت الداكنة", "🗺️", "dark"),
    SATELLITE("الأقمار الصناعية", "🛰️", "satellite"),
    STREETS("خريطة الشوارع", "🌍", "osm")
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InteractiveWeatherMap(
    currentCity: CityLocation,
    cityForecasts: List<CityMapForecast>,
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
            text = if (cityForecasts.isEmpty()) {
                "جارٍ تحميل توقعات المدن السورية من Open-Meteo..."
            } else {
                "توقعات نموذجية محدثة لـ ${cityForecasts.size} مدينة سورية؛ ليست قياسات محطات رصد أرضية."
            },
            fontSize = 10.sp,
            color = if (cityForecasts.isEmpty()) Color(0xFFFACC15) else Color(0xFF86EFAC)
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
            val htmlContent = remember(selectedTileStyle, selectedOverlay, cityForecasts) {
                buildLeafletMapHtml(selectedTileStyle.tileType, selectedOverlay, cityForecasts)
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
                        tag = htmlContent
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
                    if (shouldReloadEmbeddedMapHtml(webView.tag as? String, htmlContent)) {
                        webView.tag = htmlContent
                        webView.loadDataWithBaseURL(
                            "https://openstreetmap.org",
                            htmlContent,
                            "text/html",
                            "UTF-8",
                            null
                        )
                    }
                },
                onRelease = { webView ->
                    webView.stopLoading()
                    webView.webChromeClient = null
                    webView.webViewClient = WebViewClient()
                    webView.removeAllViews()
                    webView.destroy()
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
private fun buildLeafletMapHtml(
    tileStyle: String,
    overlay: MapWeatherOverlay,
    cityForecasts: List<CityMapForecast>
): String {
    val baseMap = LeafletBaseMapConfigs.forStyle(tileStyle)
    val cityForecastsJson = JSONArray().apply {
        cityForecasts.forEach { forecast ->
            val condition = forecast.weatherCode?.let(WeatherCodeMapping::description)?.arabic
                ?: "الحالة غير متاحة"
            put(
                JSONObject()
                    .put("name", forecast.city.name)
                    .put("latitude", forecast.city.latitude)
                    .put("longitude", forecast.city.longitude)
                    .put("temperature", forecast.temperatureCelsius ?: JSONObject.NULL)
                    .put("humidity", forecast.humidityPercent ?: JSONObject.NULL)
                    .put("wind", forecast.windSpeedKmh ?: JSONObject.NULL)
                    .put("windDirection", forecast.windDirectionDegrees ?: JSONObject.NULL)
                    .put("cloudCover", forecast.cloudCoverPercent ?: JSONObject.NULL)
                    .put("precipitation", forecast.precipitationMm ?: JSONObject.NULL)
                    .put("condition", condition)
                    .put("time", forecast.time ?: JSONObject.NULL)
            )
        }
    }.toString().let(::encodeJsonForHtmlScript)
    val subdomainsOption = baseMap.subdomains?.let { "subdomains: '$it'," }.orEmpty()

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
                    maxZoom: 12,
                    zoomControl: false
                });

                L.tileLayer('${baseMap.urlTemplate}', {
                    attribution: '${baseMap.attributionHtml}',
                    $subdomainsOption
                    maxZoom: ${baseMap.maxZoom}
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
                        var timelineEnabled = true;

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
                            opacity: 0.65,
                            attribution: 'Weather data by <a href="https://www.rainviewer.com/" target="_blank" rel="noopener">RainViewer</a>',
                            maxZoom: 7
                        });
                        radarLayer.addTo(map);
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



                // Live model forecasts for Syrian cities (not station observations).
                var syrianCities = ${cityForecastsJson};
                if (syrianCities.length === 0) {
                    var emptyForecastNotice = L.control({ position: 'topright' });
                    emptyForecastNotice.onAdd = function() {
                        var notice = L.DomUtil.create('div', 'radar-status');
                        notice.textContent = 'توقعات المدن غير متاحة حالياً؛ أعد المحاولة لاحقاً.';
                        return notice;
                    };
                    emptyForecastNotice.addTo(map);
                }

                function appendPopupLine(container, label, value, suffix) {
                    var line = document.createElement('div');
                    var labelNode = document.createElement('strong');
                    labelNode.textContent = label + ': ';
                    var valueNode = document.createElement('span');
                    valueNode.textContent = value === null || value === undefined
                        ? 'غير متاح'
                        : String(value) + (suffix || '');
                    line.appendChild(labelNode);
                    line.appendChild(valueNode);
                    container.appendChild(line);
                }

                syrianCities.forEach(function(city) {
                    if (!Number.isFinite(city.latitude) || !Number.isFinite(city.longitude)) return;
                    var marker = L.circleMarker([city.latitude, city.longitude], {
                        color: '#38BDF8',
                        fillColor: '#0284C7',
                        fillOpacity: 0.9,
                        radius: 8
                    }).addTo(map);

                    var popup = document.createElement('div');
                    popup.className = 'city-popup';
                    var title = document.createElement('div');
                    title.className = 'city-title';
                    title.textContent = city.name;
                    popup.appendChild(title);
                    appendPopupLine(popup, 'الحرارة', city.temperature, ' °م');
                    appendPopupLine(popup, 'الحالة', city.condition, '');
                    appendPopupLine(popup, 'الرطوبة', city.humidity, '%');
                    appendPopupLine(popup, 'سرعة الرياح', city.wind, ' كم/س');
                    appendPopupLine(popup, 'اتجاه الرياح', city.windDirection, '°');
                    appendPopupLine(popup, 'الغطاء السحابي', city.cloudCover, '%');
                    appendPopupLine(popup, 'الهطول', city.precipitation, ' مم');
                    appendPopupLine(popup, 'وقت البيانات', city.time, '');
                    marker.bindPopup(popup);
                });
            </script>
        </body>
        </html>
    """.trimIndent()
}
