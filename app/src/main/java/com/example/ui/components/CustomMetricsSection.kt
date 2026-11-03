package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*

@Composable
fun CustomMetricsSection(
    preferences: List<MetricPreference>,
    current: CurrentWeather,
    todayForecast: DailyForecast?,
    airQuality: AirQualityInfo,
    pollen: PollenInfo,
    onCustomizeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val enabledPreferences = preferences.filter { it.isEnabled }

    Column(modifier = modifier.fillMaxWidth()) {
        // Section Header with Customize Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DashboardCustomize,
                    contentDescription = null,
                    tint = Color(0xFFFFD54F),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "بيانات الطقس المخصصة",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Customize Button
            TextButton(
                onClick = onCustomizeClick,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.testTag("customize_metrics_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "تخصيص وترتيب",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8)
                )
            }
        }

        // Render enabled metrics in pairs or full cards
        val chunks = enabledPreferences.chunked(2)
        chunks.forEach { pair ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                pair.forEach { pref ->
                    Box(modifier = Modifier.weight(1f)) {
                        RenderMetricCard(
                            metricType = pref.type,
                            current = current,
                            todayForecast = todayForecast,
                            airQuality = airQuality,
                            pollen = pollen
                        )
                    }
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun RenderMetricCard(
    metricType: WeatherMetricType,
    current: CurrentWeather,
    todayForecast: DailyForecast?,
    airQuality: AirQualityInfo,
    pollen: PollenInfo
) {
    when (metricType) {
        WeatherMetricType.UV_INDEX -> {
            val uv = current.uvIndex.toInt()
            val (desc, color) = when {
                uv <= 2 -> "منخفض" to Color(0xFF4ADE80)
                uv <= 5 -> "معتدل" to Color(0xFFFACC15)
                uv <= 7 -> "مرتفع" to Color(0xFFFB923C)
                uv <= 10 -> "مرتفع جداً" to Color(0xFFF87171)
                else -> "شديد الخطورة" to Color(0xFFA855F7)
            }
            MetricCard(
                title = "مؤشر الأشعة (UV)",
                icon = Icons.Default.WbSunny,
                iconTint = Color(0xFFFFCA28)
            ) {
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
                            .background(color.copy(alpha = 0.25f))
                            .border(1.dp, color, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(text = desc, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (uv <= 2) "لا حاجة للحماية" else if (uv <= 5) "نظارة شمسية" else "واقي شمس وقبعة",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }
        }

        WeatherMetricType.AIR_QUALITY -> {
            MetricCard(
                title = "جودة الهواء (AQI)",
                icon = Icons.Default.Eco,
                iconTint = Color(0xFF4ADE80)
            ) {
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
                        Text(text = airQuality.categoryAr, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4ADE80))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "PM2.5: ${airQuality.pm25.toInt()} | O₃: ${airQuality.ozone.toInt()}",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }
        }

        WeatherMetricType.POLLEN_COUNT -> {
            val (pollenColor, pollenTag) = when (pollen.overallLevelAr) {
                "منخفض جداً" -> Color(0xFF4ADE80) to "منخفض"
                "معتدل" -> Color(0xFFFACC15) to "معتدل"
                "مرتفع" -> Color(0xFFFB923C) to "مرتفع"
                else -> Color(0xFFEF4444) to "تحذير"
            }
            MetricCard(
                title = "حبوب اللقاح والحساسية",
                icon = Icons.Default.Grass,
                iconTint = Color(0xFFA3E635)
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = pollenTag,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = pollenColor
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "أعشاب: ${pollen.grassPollen.toInt()} | زيتون: ${pollen.olivePollen.toInt()}",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = pollen.adviceAr,
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 2
                )
            }
        }

        WeatherMetricType.WIND -> {
            MetricCard(
                title = "سرعة الرياح",
                icon = Icons.Default.Air,
                iconTint = Color(0xFF67E8F9)
            ) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CompassNeedle(
                        degrees = current.windDirectionDegrees.toFloat(),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "اتجاه الرياح",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        WeatherMetricType.HUMIDITY -> {
            MetricCard(
                title = "الرطوبة النسبية",
                icon = Icons.Default.WaterDrop,
                iconTint = Color(0xFF38BDF8)
            ) {
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
                            .background(Brush.horizontalGradient(listOf(Color(0xFF38BDF8), Color(0xFF0284C7))))
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

        WeatherMetricType.SUN_CYCLE -> {
            MetricCard(
                title = "الشروق والغروب",
                icon = Icons.Default.WbTwilight,
                iconTint = Color(0xFFFF9800)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "الشروق", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                        Text(text = todayForecast?.sunrise ?: "06:15 ص", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "الغروب", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                        Text(text = todayForecast?.sunset ?: "05:45 م", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                SunTrajectoryArc(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                )
            }
        }

        WeatherMetricType.PRESSURE -> {
            MetricCard(
                title = "الضغط الجوي",
                icon = Icons.Default.Speed,
                iconTint = Color(0xFFA78BFA)
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${current.pressureHpa.toInt()}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "hPa", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (current.pressureHpa >= 1013) "ضغط مرتفع - طقس مستقر" else "منخفض جوي متقلب",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }
        }

        WeatherMetricType.VISIBILITY -> {
            MetricCard(
                title = "مدى الرؤية الأفقية",
                icon = Icons.Default.Visibility,
                iconTint = Color(0xFF2DD4BF)
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${current.visibilityKm.toInt()}",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "كم", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (current.visibilityKm >= 10) "رؤية ممتازة وصافية" else if (current.visibilityKm >= 5) "رؤية جيدة" else "رؤية ضبابية - انتبه",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }
        }
    }
}
