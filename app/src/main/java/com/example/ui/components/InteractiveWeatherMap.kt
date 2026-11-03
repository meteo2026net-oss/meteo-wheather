package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CityLocation
import com.example.data.model.MapLocationPin
import com.example.data.model.MapWeatherOverlay
import com.example.data.model.WeatherConditionType
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun InteractiveWeatherMap(
    currentCity: CityLocation,
    onCitySelected: (CityLocation) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedOverlay by remember { mutableStateOf(MapWeatherOverlay.PRECIPITATION) }
    var zoom by remember { mutableFloatStateOf(1.0f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var isTimelinePlaying by remember { mutableStateOf(true) }
    var timelineStep by remember { mutableIntStateOf(0) } // 0: Now, 1: +1h, 2: +2h, 3: +3h, 4: +6h
    var inspectedPin by remember { mutableStateOf<MapLocationPin?>(null) }

    // Map Locations Pins (Geographical coordinates normalized roughly)
    val mapPins = remember {
        listOf(
            MapLocationPin("الرياض", "السعودية", 0.58, 0.54, 29.0, WeatherConditionType.CLEAR, 0.0, 18.0),
            MapLocationPin("القاهرة", "مصر", 0.38, 0.44, 24.0, WeatherConditionType.PARTLY_CLOUDY, 0.0, 14.0),
            MapLocationPin("دبي", "الإمارات", 0.72, 0.52, 31.0, WeatherConditionType.CLEAR, 0.0, 16.0),
            MapLocationPin("الدار البيضاء", "المغرب", 0.14, 0.40, 21.0, WeatherConditionType.RAIN, 3.2, 22.0),
            MapLocationPin("مكة المكرمة", "السعودية", 0.52, 0.62, 34.0, WeatherConditionType.CLEAR, 0.0, 12.0),
            MapLocationPin("عَمّان", "الأردن", 0.42, 0.40, 20.0, WeatherConditionType.PARTLY_CLOUDY, 0.0, 15.0),
            MapLocationPin("الكويت", "الكويت", 0.62, 0.46, 27.0, WeatherConditionType.CLEAR, 0.0, 20.0),
            MapLocationPin("الدوحة", "قطر", 0.67, 0.52, 30.0, WeatherConditionType.CLEAR, 0.0, 14.0),
            MapLocationPin("بغداد", "العراق", 0.54, 0.38, 23.0, WeatherConditionType.CLOUDY, 0.2, 18.0),
            MapLocationPin("إسطنبول", "تركيا", 0.40, 0.22, 16.0, WeatherConditionType.RAIN, 2.5, 25.0),
            MapLocationPin("باريس", "فرنسا", 0.22, 0.14, 13.0, WeatherConditionType.RAIN, 4.1, 28.0),
            MapLocationPin("لندن", "المملكة المتحدة", 0.20, 0.08, 11.0, WeatherConditionType.DRIZZLE, 1.8, 30.0)
        )
    }

    // Animation Transitions
    val infiniteTransition = rememberInfiniteTransition(label = "mapFlow")
    val flowPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "flowPhase"
    )

    val radarSweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarSweep"
    )

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
                        imageVector = Icons.Default.Map,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "خرائط الطقس التفاعلية",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "استكشف الأمطار، درجات الحرارة، وحركة الرياح",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
            }

            // Reset Map View Button
            IconButton(
                onClick = {
                    zoom = 1.0f
                    offset = Offset.Zero
                    inspectedPin = null
                }
            ) {
                Icon(
                    imageVector = Icons.Default.CenterFocusStrong,
                    contentDescription = "إعادة ضبط المنظر",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Overlay Mode Selector Chips
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
                            if (isSelected) Color(0xFF0284C7) else Color.White.copy(alpha = 0.12f)
                        )
                        .border(
                            1.dp,
                            if (isSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.15f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { selectedOverlay = overlay }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = overlay.titleAr,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Interactive Map Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF090D1A))
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(20.dp))
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, gestureZoom, _ ->
                        zoom = (zoom * gestureZoom).coerceIn(0.85f, 2.5f)
                        offset = Offset(
                            x = (offset.x + pan.x).coerceIn(-150f * zoom, 150f * zoom),
                            y = (offset.y + pan.y).coerceIn(-120f * zoom, 120f * zoom)
                        )
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val center = Offset(w / 2f + offset.x, h / 2f + offset.y)

                // Background Geographical Grid & Continents base
                drawGeographicBase(w, h, center, zoom)

                // Draw chosen Weather Layer Overlay
                when (selectedOverlay) {
                    MapWeatherOverlay.PRECIPITATION -> {
                        drawPrecipitationRadarOverlay(w, h, center, zoom, radarSweepAngle, timelineStep)
                    }
                    MapWeatherOverlay.TEMPERATURE -> {
                        drawTemperatureHeatmapOverlay(w, h, center, zoom, timelineStep)
                    }
                    MapWeatherOverlay.WIND -> {
                        drawWindStreamlinesOverlay(w, h, center, zoom, flowPhase)
                    }
                    MapWeatherOverlay.CLOUDS -> {
                        drawCloudCoverOverlay(w, h, center, zoom, flowPhase)
                    }
                }
            }

            // City Pins rendered as interactive clickable overlays
            mapPins.forEach { pin ->
                val pinX = (pin.latitude.toFloat() * 260f * zoom) + offset.x
                val pinY = (pin.longitude.toFloat() * 220f * zoom) + offset.y
                Box(
                    modifier = Modifier
                        .offset(
                            x = pinX.dp.coerceIn(10.dp, 300.dp),
                            y = pinY.dp.coerceIn(10.dp, 210.dp)
                        )
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (inspectedPin?.name == pin.name) Color(0xFFFFD54F) else Color(0xDD0F172A)
                        )
                        .border(
                            1.dp,
                            if (inspectedPin?.name == pin.name) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.35f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { inspectedPin = pin }
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(
                                    when (pin.conditionType) {
                                        WeatherConditionType.CLEAR -> Color(0xFFFFCA28)
                                        WeatherConditionType.RAIN -> Color(0xFF38BDF8)
                                        else -> Color(0xFF94A3B8)
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${pin.name} ${pin.tempCelsius.toInt()}°",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (inspectedPin?.name == pin.name) Color(0xFF0F172A) else Color.White
                        )
                    }
                }
            }

            // Floating Inspector Card when a city is tapped
            inspectedPin?.let { pin ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(10.dp)
                        .fillMaxWidth(0.92f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xF00F172A))
                        .border(1.dp, Color(0xFF0284C7), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = pin.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = pin.country,
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "الحرارة: ${pin.tempCelsius.toInt()}° | الرياح: ${pin.windKmh.toInt()} كم/س",
                                    fontSize = 11.sp,
                                    color = Color(0xFFFFD54F)
                                )
                                if (pin.rainMm > 0) {
                                    Text(
                                        text = " | أمطار: ${pin.rainMm} ملم",
                                        fontSize = 11.sp,
                                        color = Color(0xFF67E8F9)
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                onCitySelected(
                                    CityLocation(
                                        name = pin.name,
                                        country = pin.country,
                                        latitude = 24.7136, // approx
                                        longitude = 46.6753
                                    )
                                )
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                        ) {
                            Text(text = "عرض الطقس", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Map Controls (Zoom in / out)
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable { zoom = (zoom + 0.25f).coerceAtMost(2.5f) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "تكبير", tint = Color.White, modifier = Modifier.size(16.dp))
                }
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable { zoom = (zoom - 0.25f).coerceAtLeast(0.85f) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = "تصغير", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
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

// Canvas Drawing Subroutines for Map Overlays

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGeographicBase(
    w: Float,
    h: Float,
    center: Offset,
    zoom: Float
) {
    // Subtle Latitude/Longitude lines
    for (i in 1..4) {
        drawLine(
            color = Color(0xFF1E293B),
            start = Offset(0f, (h / 5f) * i),
            end = Offset(w, (h / 5f) * i),
            strokeWidth = 1.dp.toPx()
        )
    }
    for (i in 1..5) {
        drawLine(
            color = Color(0xFF1E293B),
            start = Offset((w / 6f) * i, 0f),
            end = Offset((w / 6f) * i, h),
            strokeWidth = 1.dp.toPx()
        )
    }

    // Stylized Coastlines / Landmass contours (Arabian Peninsula & Mediterranean contours)
    val landPath = Path().apply {
        moveTo(w * 0.10f * zoom, h * 0.35f * zoom)
        quadraticTo(w * 0.35f * zoom, h * 0.28f * zoom, w * 0.50f * zoom, h * 0.38f * zoom)
        quadraticTo(w * 0.65f * zoom, h * 0.42f * zoom, w * 0.75f * zoom, h * 0.58f * zoom)
        quadraticTo(w * 0.60f * zoom, h * 0.72f * zoom, w * 0.48f * zoom, h * 0.65f * zoom)
        quadraticTo(w * 0.35f * zoom, h * 0.55f * zoom, w * 0.10f * zoom, h * 0.35f * zoom)
    }
    drawPath(
        path = landPath,
        color = Color(0xFF131D31),
        style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPrecipitationRadarOverlay(
    w: Float,
    h: Float,
    center: Offset,
    zoom: Float,
    sweepAngle: Float,
    step: Int
) {
    val stepOffset = step * 12.dp.toPx()

    // Rain Cells (scattered across regions)
    val cells = listOf(
        Triple(Offset(w * 0.30f + stepOffset, h * 0.35f), 55.dp.toPx() * zoom, Color(0xFF22C55E)),
        Triple(Offset(w * 0.42f + stepOffset, h * 0.32f), 38.dp.toPx() * zoom, Color(0xFFEAB308)),
        Triple(Offset(w * 0.45f + stepOffset, h * 0.30f), 18.dp.toPx() * zoom, Color(0xFFEF4444)),
        Triple(Offset(w * 0.65f + stepOffset, h * 0.50f), 45.dp.toPx() * zoom, Color(0xFF22C55E)),
        Triple(Offset(w * 0.18f + stepOffset, h * 0.22f), 50.dp.toPx() * zoom, Color(0xFF0284C7))
    )

    for ((pos, radius, color) in cells) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = 0.65f), color.copy(alpha = 0.20f), Color.Transparent),
                center = pos,
                radius = radius
            ),
            radius = radius,
            center = pos
        )
    }

    // Radar Beam Sweep
    val rad = Math.toRadians(sweepAngle.toDouble())
    val sweepLength = w * 0.45f * zoom
    val endPt = Offset(
        (center.x + sweepLength * cos(rad)).toFloat(),
        (center.y + sweepLength * sin(rad)).toFloat()
    )
    drawLine(
        brush = Brush.linearGradient(listOf(Color(0xFF06B6D4), Color(0xFF38BDF8))),
        start = center,
        end = endPt,
        strokeWidth = 2.dp.toPx()
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTemperatureHeatmapOverlay(
    w: Float,
    h: Float,
    center: Offset,
    zoom: Float,
    step: Int
) {
    // Thermal zones: Hot desert in south, temperate in middle, cold in north
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFEF4444).copy(alpha = 0.45f), Color(0xFFF97316).copy(alpha = 0.25f), Color.Transparent),
            center = Offset(w * 0.58f, h * 0.55f),
            radius = w * 0.40f * zoom
        ),
        radius = w * 0.40f * zoom,
        center = Offset(w * 0.58f, h * 0.55f)
    )

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFBBF24).copy(alpha = 0.40f), Color(0xFF4ADE80).copy(alpha = 0.20f), Color.Transparent),
            center = Offset(w * 0.38f, h * 0.40f),
            radius = w * 0.35f * zoom
        ),
        radius = w * 0.35f * zoom,
        center = Offset(w * 0.38f, h * 0.40f)
    )

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF38BDF8).copy(alpha = 0.50f), Color(0xFF0284C7).copy(alpha = 0.25f), Color.Transparent),
            center = Offset(w * 0.25f, h * 0.15f),
            radius = w * 0.32f * zoom
        ),
        radius = w * 0.32f * zoom,
        center = Offset(w * 0.25f, h * 0.15f)
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWindStreamlinesOverlay(
    w: Float,
    h: Float,
    center: Offset,
    zoom: Float,
    phase: Float
) {
    // Vector flow curves representing jet streams and wind flow
    val yBands = listOf(0.25f, 0.40f, 0.55f, 0.70f)
    for ((idx, yFrac) in yBands.withIndex()) {
        val y = h * yFrac
        val path = Path().apply {
            moveTo(0f, y)
            cubicTo(
                w * 0.30f, y - 25.dp.toPx() * (if (idx % 2 == 0) 1 else -1),
                w * 0.70f, y + 25.dp.toPx() * (if (idx % 2 == 0) 1 else -1),
                w, y
            )
        }
        drawPath(
            path = path,
            color = Color(0xFF38BDF8).copy(alpha = 0.35f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        // Animated Particle on Streamline
        val particleX = ((phase + idx * 0.25f) % 1f) * w
        val particleY = y + sin((phase + idx) * Math.PI * 2).toFloat() * 12.dp.toPx()
        drawCircle(
            color = Color(0xFF67E8F9),
            radius = 3.5.dp.toPx(),
            center = Offset(particleX, particleY)
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCloudCoverOverlay(
    w: Float,
    h: Float,
    center: Offset,
    zoom: Float,
    phase: Float
) {
    val drift = (phase * 30f) - 15f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.30f), Color.White.copy(alpha = 0.10f), Color.Transparent),
            center = Offset(w * 0.35f + drift, h * 0.30f),
            radius = 80.dp.toPx() * zoom
        ),
        radius = 80.dp.toPx() * zoom,
        center = Offset(w * 0.35f + drift, h * 0.30f)
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.08f), Color.Transparent),
            center = Offset(w * 0.65f - drift, h * 0.45f),
            radius = 70.dp.toPx() * zoom
        ),
        radius = 70.dp.toPx() * zoom,
        center = Offset(w * 0.65f - drift, h * 0.45f)
    )
}
