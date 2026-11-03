package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.WeatherConditionType
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnimatedWeatherIcon(
    condition: WeatherConditionType,
    isDay: Boolean = true,
    size: Dp = 100.dp,
    modifier: Modifier = Modifier,
    interactiveTapEnabled: Boolean = true
) {
    // Interactive tap bounce / spin state
    var tapCounter by remember { mutableIntStateOf(0) }
    val tapScale by animateFloatAsState(
        targetValue = if (tapCounter % 2 == 1) 1.18f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "tapScale"
    )

    // Infinite ambient animations
    val infiniteTransition = rememberInfiniteTransition(label = "weatherAnim")

    val sunRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sunRotation"
    )

    val sunPulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sunPulse"
    )

    val cloudDrift by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cloudDrift"
    )

    val rainOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rainOffset"
    )

    val snowOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "snowOffset"
    )

    val lightningAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3600
                0.0f at 0
                0.0f at 2800
                1.0f at 2860
                0.1f at 2920
                0.9f at 2980
                0.0f at 3060
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "lightningAlpha"
    )

    val moonGlow by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "moonGlow"
    )

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = tapScale
                scaleY = tapScale
            }
            .then(
                if (interactiveTapEnabled) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        tapCounter++
                    }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height

            when (condition) {
                WeatherConditionType.CLEAR -> {
                    if (isDay) {
                        drawSun(w, h, sunRotation, sunPulse)
                    } else {
                        drawMoonAndStars(w, h, moonGlow)
                    }
                }
                WeatherConditionType.PARTLY_CLOUDY -> {
                    if (isDay) {
                        drawSunBehindCloud(w, h, sunRotation * 0.5f, cloudDrift)
                    } else {
                        drawMoonBehindCloud(w, h, cloudDrift)
                    }
                }
                WeatherConditionType.CLOUDY -> {
                    drawLayeredClouds(w, h, cloudDrift)
                }
                WeatherConditionType.DRIZZLE,
                WeatherConditionType.RAIN -> {
                    drawRainWithCloud(w, h, cloudDrift, rainOffset, isHeavy = false)
                }
                WeatherConditionType.HEAVY_RAIN -> {
                    drawRainWithCloud(w, h, cloudDrift, rainOffset, isHeavy = true)
                }
                WeatherConditionType.THUNDERSTORM -> {
                    drawThunderstorm(w, h, cloudDrift, rainOffset, lightningAlpha)
                }
                WeatherConditionType.SNOW -> {
                    drawSnowWithCloud(w, h, cloudDrift, snowOffset)
                }
                WeatherConditionType.FOG -> {
                    drawFogLayers(w, h, cloudDrift)
                }
            }
        }
    }
}

// Canvas Drawing Helpers

private fun DrawScope.drawSun(w: Float, h: Float, rotationDegrees: Float, pulse: Float) {
    val center = Offset(w / 2f, h / 2f)
    val coreRadius = (w * 0.22f) * pulse

    // Radiant outer glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFD54F).copy(alpha = 0.55f), Color(0xFFFFB300).copy(alpha = 0.15f), Color.Transparent),
            center = center,
            radius = w * 0.46f
        ),
        radius = w * 0.46f,
        center = center
    )

    // Rotating Sun Rays (8 rays)
    val rayLength = w * 0.14f
    val rayStart = coreRadius + 4f
    val rad = Math.toRadians(rotationDegrees.toDouble())
    for (i in 0 until 8) {
        val angle = rad + (i * Math.PI / 4)
        val startX = (center.x + rayStart * cos(angle)).toFloat()
        val startY = (center.y + rayStart * sin(angle)).toFloat()
        val endX = (center.x + (rayStart + rayLength) * cos(angle)).toFloat()
        val endY = (center.y + (rayStart + rayLength) * sin(angle)).toFloat()

        drawLine(
            brush = Brush.linearGradient(
                listOf(Color(0xFFFFCA28), Color(0xFFFF9800))
            ),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = w * 0.045f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }

    // Glowing Sun Core
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFF9C4), Color(0xFFFFCA28), Color(0xFFF57C00)),
            center = Offset(center.x - coreRadius * 0.3f, center.y - coreRadius * 0.3f),
            radius = coreRadius * 1.3f
        ),
        radius = coreRadius,
        center = center
    )
}

private fun DrawScope.drawMoonAndStars(w: Float, h: Float, glow: Float) {
    val center = Offset(w * 0.48f, h * 0.50f)
    val moonRadius = w * 0.26f

    // Soft celestial glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF81D4FA).copy(alpha = 0.35f * glow), Color.Transparent),
            center = center,
            radius = moonRadius * 1.8f
        ),
        radius = moonRadius * 1.8f,
        center = center
    )

    // Crescent Moon path
    val moonPath = Path().apply {
        // Outer arc
        addOval(androidx.compose.ui.geometry.Rect(center.x - moonRadius, center.y - moonRadius, center.x + moonRadius, center.y + moonRadius))
    }
    val subtractPath = Path().apply {
        val offsetCenter = Offset(center.x + moonRadius * 0.42f, center.y - moonRadius * 0.28f)
        val cutRadius = moonRadius * 0.88f
        addOval(androidx.compose.ui.geometry.Rect(offsetCenter.x - cutRadius, offsetCenter.y - cutRadius, offsetCenter.x + cutRadius, offsetCenter.y + cutRadius))
    }
    val finalMoon = Path.combine(androidx.compose.ui.graphics.PathOperation.Difference, moonPath, subtractPath)

    drawPath(
        path = finalMoon,
        brush = Brush.linearGradient(
            listOf(Color(0xFFFFF9C4), Color(0xFF81D4FA), Color(0xFF4FC3F7))
        )
    )

    // Little twinkling star sparkles
    drawSparkle(Offset(w * 0.78f, h * 0.28f), w * 0.08f * glow)
    drawSparkle(Offset(w * 0.84f, h * 0.65f), w * 0.06f * (2f - glow))
    drawSparkle(Offset(w * 0.22f, h * 0.24f), w * 0.05f * glow)
}

private fun DrawScope.drawSparkle(pos: Offset, r: Float) {
    val path = Path().apply {
        moveTo(pos.x, pos.y - r)
        quadraticTo(pos.x, pos.y, pos.x + r, pos.y)
        quadraticTo(pos.x, pos.y, pos.x, pos.y + r)
        quadraticTo(pos.x, pos.y, pos.x - r, pos.y)
        quadraticTo(pos.x, pos.y, pos.x, pos.y - r)
    }
    drawPath(path, Brush.radialGradient(listOf(Color.White, Color(0xFF81D4FA)), center = pos, radius = r))
}

private fun DrawScope.drawSunBehindCloud(w: Float, h: Float, rotation: Float, drift: Float) {
    // Sun in upper-right
    val sunCenter = Offset(w * 0.68f, h * 0.36f)
    val sunRadius = w * 0.17f

    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFFFFF176), Color(0xFFFFB74D), Color(0xFFF57C00)),
            center = sunCenter,
            radius = sunRadius
        ),
        radius = sunRadius,
        center = sunCenter
    )

    // Draw cloud in foreground with drift
    drawPuffyCloud(
        center = Offset(w * 0.46f + drift, h * 0.60f),
        width = w * 0.72f,
        height = h * 0.44f,
        colorTop = Color(0xFFFFFFFF),
        colorBottom = Color(0xFFB0BEC5)
    )
}

private fun DrawScope.drawMoonBehindCloud(w: Float, h: Float, drift: Float) {
    // Moon in upper right
    drawMoonAndStars(w * 0.85f, h * 0.85f, 1f)
    // Cloud in foreground
    drawPuffyCloud(
        center = Offset(w * 0.48f + drift, h * 0.62f),
        width = w * 0.74f,
        height = h * 0.44f,
        colorTop = Color(0xFFECEFF1),
        colorBottom = Color(0xFF90A4AE)
    )
}

private fun DrawScope.drawLayeredClouds(w: Float, h: Float, drift: Float) {
    // Back cloud
    drawPuffyCloud(
        center = Offset(w * 0.58f - drift * 0.7f, h * 0.44f),
        width = w * 0.68f,
        height = h * 0.40f,
        colorTop = Color(0xFFCFD8DC),
        colorBottom = Color(0xFF78909C)
    )
    // Front cloud
    drawPuffyCloud(
        center = Offset(w * 0.44f + drift, h * 0.62f),
        width = w * 0.75f,
        height = h * 0.45f,
        colorTop = Color(0xFFFFFFFF),
        colorBottom = Color(0xFFB0BEC5)
    )
}

private fun DrawScope.drawRainWithCloud(
    w: Float,
    h: Float,
    drift: Float,
    rainOffset: Float,
    isHeavy: Boolean
) {
    // Cloud
    val cloudColorTop = if (isHeavy) Color(0xFF90A4AE) else Color(0xFFECEFF1)
    val cloudColorBottom = if (isHeavy) Color(0xFF455A64) else Color(0xFF78909C)

    drawPuffyCloud(
        center = Offset(w * 0.5f + drift, h * 0.42f),
        width = w * 0.74f,
        height = h * 0.44f,
        colorTop = cloudColorTop,
        colorBottom = cloudColorBottom
    )

    // Animated raindrops (slanting down-left for wind effect)
    val dropCount = if (isHeavy) 6 else 4
    val startY = h * 0.60f
    val travelDist = h * 0.32f
    val dropLength = if (isHeavy) h * 0.10f else h * 0.07f

    for (i in 0 until dropCount) {
        val frac = (rainOffset + (i.toFloat() / dropCount)) % 1f
        val x = w * (0.28f + (i * 0.12f)) - (frac * 6f)
        val y = startY + (frac * travelDist)

        drawLine(
            brush = Brush.linearGradient(
                listOf(Color(0xFF81D4FA).copy(alpha = 0.2f), Color(0xFF0288D1))
            ),
            start = Offset(x, y),
            end = Offset(x - 4f, y + dropLength),
            strokeWidth = if (isHeavy) 3.5f else 2.5f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}

private fun DrawScope.drawThunderstorm(
    w: Float,
    h: Float,
    drift: Float,
    rainOffset: Float,
    lightningAlpha: Float
) {
    // Dark ominous storm cloud
    drawPuffyCloud(
        center = Offset(w * 0.5f + drift, h * 0.40f),
        width = w * 0.76f,
        height = h * 0.46f,
        colorTop = Color(0xFF607D8B),
        colorBottom = Color(0xFF263238)
    )

    // Lightning bolt
    if (lightningAlpha > 0.05f) {
        val boltPath = Path().apply {
            moveTo(w * 0.52f, h * 0.55f)
            lineTo(w * 0.44f, h * 0.72f)
            lineTo(w * 0.52f, h * 0.72f)
            lineTo(w * 0.38f, h * 0.94f)
        }
        drawPath(
            path = boltPath,
            color = Color(0xFFFFEB3B).copy(alpha = lightningAlpha),
            style = Stroke(width = 4.5f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )
        // Halo around lightning
        drawPath(
            path = boltPath,
            color = Color(0xFFFF9800).copy(alpha = lightningAlpha * 0.6f),
            style = Stroke(width = 9f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )
    }

    // Heavy raindrops
    val startY = h * 0.62f
    val travelDist = h * 0.30f
    for (i in 0 until 5) {
        val frac = (rainOffset + (i.toFloat() / 5)) % 1f
        val x = w * (0.26f + (i * 0.13f))
        val y = startY + (frac * travelDist)
        drawLine(
            color = Color(0xFF4FC3F7),
            start = Offset(x, y),
            end = Offset(x - 5f, y + h * 0.08f),
            strokeWidth = 3f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}

private fun DrawScope.drawSnowWithCloud(w: Float, h: Float, drift: Float, snowOffset: Float) {
    // Cloud
    drawPuffyCloud(
        center = Offset(w * 0.5f + drift, h * 0.42f),
        width = w * 0.74f,
        height = h * 0.44f,
        colorTop = Color(0xFFECEFF1),
        colorBottom = Color(0xFFB0BEC5)
    )

    // Floating snowflakes
    val startY = h * 0.62f
    val travelDist = h * 0.30f
    for (i in 0 until 4) {
        val frac = (snowOffset + (i.toFloat() / 4)) % 1f
        val wave = sin((frac * Math.PI * 2) + i).toFloat() * 6f
        val x = w * (0.32f + (i * 0.13f)) + wave
        val y = startY + (frac * travelDist)

        drawSnowflake(Offset(x, y), w * 0.035f)
    }
}

private fun DrawScope.drawSnowflake(pos: Offset, r: Float) {
    // Draw simple crisp snowflake cross
    drawLine(Color.White, Offset(pos.x - r, pos.y), Offset(pos.x + r, pos.y), strokeWidth = 2.5f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    drawLine(Color.White, Offset(pos.x, pos.y - r), Offset(pos.x, pos.y + r), strokeWidth = 2.5f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    val diag = r * 0.707f
    drawLine(Color.White, Offset(pos.x - diag, pos.y - diag), Offset(pos.x + diag, pos.y + diag), strokeWidth = 2f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    drawLine(Color.White, Offset(pos.x - diag, pos.y + diag), Offset(pos.x + diag, pos.y - diag), strokeWidth = 2f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
}

private fun DrawScope.drawFogLayers(w: Float, h: Float, drift: Float) {
    val barHeight = h * 0.055f
    val yPositions = listOf(h * 0.35f, h * 0.50f, h * 0.65f, h * 0.80f)

    for ((index, y) in yPositions.withIndex()) {
        val offset = if (index % 2 == 0) drift * 1.5f else -drift * 1.5f
        val widthFrac = if (index % 2 == 0) 0.70f else 0.55f
        val startX = (w * (1f - widthFrac) / 2f) + offset
        val endX = startX + (w * widthFrac)

        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, Color(0xFFCFD8DC).copy(alpha = 0.85f), Color.Transparent),
                startX = startX,
                endX = endX
            ),
            start = Offset(startX, y),
            end = Offset(endX, y),
            strokeWidth = barHeight,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}

// Puffy Cloud Utility
private fun DrawScope.drawPuffyCloud(
    center: Offset,
    width: Float,
    height: Float,
    colorTop: Color,
    colorBottom: Color
) {
    val gradient = Brush.verticalGradient(
        colors = listOf(colorTop, colorBottom),
        startY = center.y - height / 2,
        endY = center.y + height / 2
    )

    // Base horizontal pill
    val baseRect = androidx.compose.ui.geometry.Rect(
        center.x - width * 0.42f,
        center.y + height * 0.05f,
        center.x + width * 0.42f,
        center.y + height * 0.45f
    )
    drawRoundRect(
        brush = gradient,
        topLeft = Offset(baseRect.left, baseRect.top),
        size = Size(baseRect.width, baseRect.height),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(height * 0.22f, height * 0.22f)
    )

    // Left puff
    drawCircle(
        brush = gradient,
        radius = height * 0.32f,
        center = Offset(center.x - width * 0.20f, center.y + height * 0.08f)
    )

    // Center tall puff
    drawCircle(
        brush = gradient,
        radius = height * 0.42f,
        center = Offset(center.x + width * 0.05f, center.y - height * 0.04f)
    )

    // Right puff
    drawCircle(
        brush = gradient,
        radius = height * 0.28f,
        center = Offset(center.x + width * 0.28f, center.y + height * 0.12f)
    )
}
