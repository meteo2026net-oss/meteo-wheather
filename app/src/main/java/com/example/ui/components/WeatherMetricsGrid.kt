package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AirQualityInfo
import com.example.data.model.CurrentWeather
import com.example.data.model.DailyForecast
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun WeatherMetricsGrid(
    current: CurrentWeather,
    todayForecast: DailyForecast?,
    airQuality: AirQualityInfo,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Row 1: Wind & Humidity
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Wind Card
            MetricCard(
                title = "سرعة الرياح",
                icon = Icons.Default.Air,
                iconTint = Color(0xFF67E8F9),
                modifier = Modifier.weight(1f)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${current.windSpeedKmh.toInt()}",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "كم/س",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Compass Indicator
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CompassNeedle(
                            degrees = current.windDirectionDegrees.toFloat(),
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = getWindDirectionArabic(current.windDirectionDegrees),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Text(
                                text = "اتجاه الرياح",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // Humidity Card
            MetricCard(
                title = "الرطوبة النسبية",
                icon = Icons.Default.WaterDrop,
                iconTint = Color(0xFF38BDF8),
                modifier = Modifier.weight(1f)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${current.humidity}",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "%",
                            fontSize = 16.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Humidity Gauge bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth((current.humidity / 100f).coerceIn(0f, 1f))
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF38BDF8), Color(0xFF0284C7))
                                    )
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (current.humidity < 40) "أجواء جافة" else if (current.humidity < 70) "أجواء مريحة" else "رطوبة مرتفعة",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
            }
        }

        // Row 2: UV Index & Air Quality
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // UV Index Card
            MetricCard(
                title = "مؤشر الأشعة (UV)",
                icon = Icons.Default.WbSunny,
                iconTint = Color(0xFFFFCA28),
                modifier = Modifier.weight(1f)
            ) {
                val uv = current.uvIndex.toInt()
                val (uvDesc, uvColor) = getUvInfo(uv)

                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$uv",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(uvColor.copy(alpha = 0.25f))
                                .border(1.dp, uvColor, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = uvDesc,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = uvColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (uv <= 2) "لا حاجة للحماية" else if (uv <= 5) "استخدم نظارة شمسية" else if (uv <= 7) "استخدم واقي شمس وقبعة" else "تجنب التعرض المباشر",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
            }

            // Air Quality Card
            MetricCard(
                title = "جودة الهواء (AQI)",
                icon = Icons.Default.Eco,
                iconTint = Color(0xFF4ADE80),
                modifier = Modifier.weight(1f)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${airQuality.aqiValue}",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF4ADE80).copy(alpha = 0.25f))
                                .border(1.dp, Color(0xFF4ADE80), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = airQuality.categoryAr,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4ADE80)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "PM2.5: ${airQuality.pm25.toInt()} µg/m³",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
            }
        }

        // Row 3: Sunrise/Sunset & Atmospheric Pressure
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Sun Arc Card
            MetricCard(
                title = "الشروق والغروب",
                icon = Icons.Default.WbTwilight,
                iconTint = Color(0xFFFF9800),
                modifier = Modifier.weight(1f)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "الشروق",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Text(
                                text = todayForecast?.sunrise ?: "06:15 ص",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "الغروب",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Text(
                                text = todayForecast?.sunset ?: "05:45 م",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Minimal Sun Arc Visualization
                    SunTrajectoryArc(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                    )
                }
            }

            // Pressure & Visibility Card
            MetricCard(
                title = "الضغط الجوي",
                icon = Icons.Default.Speed,
                iconTint = Color(0xFFA78BFA),
                modifier = Modifier.weight(1f)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${current.pressureHpa.toInt()}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "hPa",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (current.pressureHpa >= 1013) "ضغط جوي مرتفع - استقرار" else "ضغط منخفض - تقلبات محتملة",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.15f))
            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(22.dp))
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        content()
    }
}

@Composable
fun CompassNeedle(degrees: Float, modifier: Modifier = Modifier) {
    val animatedDegrees by animateFloatAsState(
        targetValue = degrees,
        animationSpec = tween(durationMillis = 800),
        label = "compass"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h / 2f)
        val r = w / 2f

        // Outer ring
        drawCircle(
            color = Color.White.copy(alpha = 0.25f),
            radius = r,
            style = Stroke(width = 2.dp.toPx())
        )

        // North indicator dot
        drawCircle(
            color = Color(0xFFEF4444),
            radius = 2.5.dp.toPx(),
            center = Offset(center.x, center.y - r + 3.dp.toPx())
        )

        // Arrow needle
        val rad = Math.toRadians((animatedDegrees - 90).toDouble())
        val arrowLength = r * 0.72f
        val needleTip = Offset(
            (center.x + arrowLength * cos(rad)).toFloat(),
            (center.y + arrowLength * sin(rad)).toFloat()
        )
        val needleBack = Offset(
            (center.x - arrowLength * 0.5f * cos(rad)).toFloat(),
            (center.y - arrowLength * 0.5f * sin(rad)).toFloat()
        )

        drawLine(
            color = Color(0xFF67E8F9),
            start = center,
            end = needleTip,
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White.copy(alpha = 0.5f),
            start = center,
            end = needleBack,
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawCircle(color = Color.White, radius = 2.5.dp.toPx(), center = center)
    }
}

@Composable
fun SunTrajectoryArc(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val path = Path().apply {
            moveTo(6.dp.toPx(), h - 2.dp.toPx())
            quadraticTo(w / 2f, 2.dp.toPx(), w - 6.dp.toPx(), h - 2.dp.toPx())
        }

        // Draw dotted/dashed arc
        drawPath(
            path = path,
            color = Color(0xFFFFCA28).copy(alpha = 0.45f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        // Mid-day Sun position
        val sunCenter = Offset(w / 2f, 8.dp.toPx())
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFFFFF176), Color(0xFFFF9800)),
                center = sunCenter,
                radius = 7.dp.toPx()
            ),
            radius = 5.dp.toPx(),
            center = sunCenter
        )
    }
}

private fun getWindDirectionArabic(degrees: Double): String {
    val norm = (degrees % 360 + 360) % 360
    return when {
        norm >= 337.5 || norm < 22.5 -> "شمالية"
        norm < 67.5 -> "شمالية شرقية"
        norm < 112.5 -> "شرقية"
        norm < 157.5 -> "جنوبية شرقية"
        norm < 202.5 -> "جنوبية"
        norm < 247.5 -> "جنوبية غربية"
        norm < 292.5 -> "غربية"
        else -> "شمالية غربية"
    }
}

private fun getUvInfo(uv: Int): Pair<String, Color> {
    return when {
        uv <= 2 -> "منخفض" to Color(0xFF4ADE80)
        uv <= 5 -> "معتدل" to Color(0xFFFACC15)
        uv <= 7 -> "مرتفع" to Color(0xFFFB923C)
        uv <= 10 -> "مرتفع جداً" to Color(0xFFF87171)
        else -> "شديد الخطورة" to Color(0xFFA855F7)
    }
}
