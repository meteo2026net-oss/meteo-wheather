package com.example.ui.components

import com.example.ui.screens.encodeJsonForHtmlScript
import org.json.JSONObject

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
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
                        text = "طبقة رادار RainViewer",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "طبقة رادار خارجية لمنطقة $cityName",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                    Text(
                        text = "المصدر RainViewer؛ يعرض آخر إطار تاريخي متاح فقط، وقد لا تتوفر تغطية لسوريا.",
                        fontSize = 10.sp,
                        color = Color(0xFFFACC15)
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
                        setBackgroundColor(0xFF0F172A.toInt())
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
    val baseMap = LeafletBaseMapConfigs.forStyle(if (isSatellite) "satellite" else "dark")
    val subdomainsOption = baseMap.subdomains?.let { "subdomains: '$it'," }.orEmpty()
    val cityNameJson = JSONObject.quote(cityName).let(::encodeJsonForHtmlScript)

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
                    center: [$lat, $lng],
                    zoom: 9,
                    minZoom: 6,
                    maxZoom: 13,
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
                            opacity: 0.70,
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



                // City Center Marker
                var cityName = $cityNameJson;
                var cityMarker = L.circleMarker([$lat, $lng], {
                    color: '#67E8F9',
                    fillColor: '#0284C7',
                    fillOpacity: 0.9,
                    radius: 9
                }).addTo(map);

                var cityPopup = document.createElement('div');
                var cityPopupTitle = document.createElement('b');
                cityPopupTitle.textContent = 'رادار منطقة ' + cityName;
                cityPopup.appendChild(cityPopupTitle);
                cityMarker.bindPopup(cityPopup).openPopup();
            </script>
        </body>
        </html>
    """.trimIndent()
}


internal fun shouldReloadEmbeddedMapHtml(previousHtml: String?, currentHtml: String): Boolean =
    previousHtml != currentHtml
