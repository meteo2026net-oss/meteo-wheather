package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.data.model.WeatherConditionType
import kotlin.random.Random

data class WeatherAtmosphere(
    val topColor: Color,
    val middleColor: Color,
    val bottomColor: Color,
    val cardBackground: Color,
    val isDarkBackground: Boolean
)

fun getAtmosphere(condition: WeatherConditionType, isDay: Boolean): WeatherAtmosphere {
    if (!isDay) {
        return when (condition) {
            WeatherConditionType.CLEAR -> WeatherAtmosphere(
                topColor = Color(0xFF090D1A),
                middleColor = Color(0xFF131D38),
                bottomColor = Color(0xFF1E1E38),
                cardBackground = Color(0x331E293B),
                isDarkBackground = true
            )
            WeatherConditionType.THUNDERSTORM -> WeatherAtmosphere(
                topColor = Color(0xFF090514),
                middleColor = Color(0xFF1F1135),
                bottomColor = Color(0xFF2A1B4E),
                cardBackground = Color(0x382D1B4E),
                isDarkBackground = true
            )
            WeatherConditionType.RAIN,
            WeatherConditionType.HEAVY_RAIN -> WeatherAtmosphere(
                topColor = Color(0xFF0B1320),
                middleColor = Color(0xFF14243B),
                bottomColor = Color(0xFF1F3552),
                cardBackground = Color(0x331E293B),
                isDarkBackground = true
            )
            else -> WeatherAtmosphere(
                topColor = Color(0xFF0F172A),
                middleColor = Color(0xFF1E293B),
                bottomColor = Color(0xFF334155),
                cardBackground = Color(0x33334155),
                isDarkBackground = true
            )
        }
    }

    return when (condition) {
        WeatherConditionType.CLEAR -> WeatherAtmosphere(
            topColor = Color(0xFF0284C7),      // Deep Vivid Azure
            middleColor = Color(0xFF38BDF8),   // Sky Blue
            bottomColor = Color(0xFF7DD3FC),   // Gentle Horizon Glow
            cardBackground = Color(0x38FFFFFF),
            isDarkBackground = false
        )
        WeatherConditionType.PARTLY_CLOUDY -> WeatherAtmosphere(
            topColor = Color(0xFF0284C7),
            middleColor = Color(0xFF60A5FA),
            bottomColor = Color(0xFF93C5FD),
            cardBackground = Color(0x38FFFFFF),
            isDarkBackground = false
        )
        WeatherConditionType.CLOUDY,
        WeatherConditionType.FOG -> WeatherAtmosphere(
            topColor = Color(0xFF475569),
            middleColor = Color(0xFF64748B),
            bottomColor = Color(0xFF94A3B8),
            cardBackground = Color(0x33FFFFFF),
            isDarkBackground = true
        )
        WeatherConditionType.DRIZZLE,
        WeatherConditionType.RAIN -> WeatherAtmosphere(
            topColor = Color(0xFF1E293B),
            middleColor = Color(0xFF334155),
            bottomColor = Color(0xFF475569),
            cardBackground = Color(0x33FFFFFF),
            isDarkBackground = true
        )
        WeatherConditionType.HEAVY_RAIN -> WeatherAtmosphere(
            topColor = Color(0xFF0F172A),
            middleColor = Color(0xFF1E293B),
            bottomColor = Color(0xFF2E3D4F),
            cardBackground = Color(0x33FFFFFF),
            isDarkBackground = true
        )
        WeatherConditionType.THUNDERSTORM -> WeatherAtmosphere(
            topColor = Color(0xFF181028),
            middleColor = Color(0xFF2E1C4E),
            bottomColor = Color(0xFF3B2763),
            cardBackground = Color(0x38FFFFFF),
            isDarkBackground = true
        )
        WeatherConditionType.SNOW -> WeatherAtmosphere(
            topColor = Color(0xFF334E68),
            middleColor = Color(0xFF627D98),
            bottomColor = Color(0xFFBCCCDC),
            cardBackground = Color(0x38FFFFFF),
            isDarkBackground = true
        )
    }
}

@Composable
fun DynamicWeatherBackground(
    condition: WeatherConditionType,
    isDay: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val atmosphere = getAtmosphere(condition, isDay)

    val topColor by animateColorAsState(targetValue = atmosphere.topColor, animationSpec = tween(900), label = "topColor")
    val midColor by animateColorAsState(targetValue = atmosphere.middleColor, animationSpec = tween(900), label = "midColor")
    val botColor by animateColorAsState(targetValue = atmosphere.bottomColor, animationSpec = tween(900), label = "botColor")

    val infiniteTransition = rememberInfiniteTransition(label = "ambientStars")
    val starTwinkle by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "starTwinkle"
    )

    // Pre-calculated star positions for night
    val stars = remember {
        val rand = Random(42)
        List(30) {
            Triple(rand.nextFloat(), rand.nextFloat() * 0.5f, rand.nextFloat() * 2f + 1.2f)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    listOf(topColor, midColor, botColor)
                )
            )
    ) {
        if (!isDay && (condition == WeatherConditionType.CLEAR || condition == WeatherConditionType.PARTLY_CLOUDY)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                for (star in stars) {
                    val x = star.first * w
                    val y = star.second * h
                    val r = star.third
                    drawCircle(
                        color = Color.White.copy(alpha = (starTwinkle * (0.4f + star.first * 0.5f)).coerceIn(0.1f, 0.9f)),
                        radius = r,
                        center = Offset(x, y)
                    )
                }
            }
        }

        content()
    }
}
