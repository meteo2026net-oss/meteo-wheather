package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyForecast

@Composable
fun DailyForecastSection(
    dailyList: List<DailyForecast>,
    isFahrenheit: Boolean,
    modifier: Modifier = Modifier
) {
    var expandedDayIndex by remember { mutableStateOf<Int?>(null) }

    // Find week min and max to draw normalized temperature range bars
    val globalMin = dailyList.minOfOrNull { it.minTempCelsius } ?: 10.0
    val globalMax = dailyList.maxOfOrNull { it.maxTempCelsius } ?: 35.0
    val tempSpread = (globalMax - globalMin).coerceAtLeast(1.0)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.16f))
            .border(1.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(24.dp))
            .padding(16.dp)
            .animateContentSize()
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "توقعات 7 أيام القادمة",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Text(
                text = "انقر للتفاصيل",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Daily Rows
        dailyList.forEachIndexed { index, day ->
            val isExpanded = expandedDayIndex == index

            val minTemp = if (isFahrenheit) {
                (day.minTempCelsius * 9 / 5 + 32).toInt()
            } else {
                day.minTempCelsius.toInt()
            }
            val maxTemp = if (isFahrenheit) {
                (day.maxTempCelsius * 9 / 5 + 32).toInt()
            } else {
                day.maxTempCelsius.toInt()
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isExpanded) Color.Black.copy(alpha = 0.18f) else Color.Transparent
                    )
                    .clickable {
                        expandedDayIndex = if (isExpanded) null else index
                    }
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Day Name & Date
                    Column(modifier = Modifier.width(76.dp)) {
                        Text(
                            text = day.dayNameAr,
                            fontSize = 14.sp,
                            fontWeight = if (index == 0) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (index == 0) Color(0xFFFFD54F) else Color.White
                        )
                        Text(
                            text = day.formattedDate,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    }

                    // Condition Icon
                    AnimatedWeatherIcon(
                        condition = day.conditionType,
                        isDay = true,
                        size = 32.dp,
                        interactiveTapEnabled = false
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Rain Chance (if > 0)
                    if (day.precipitationProb > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.width(42.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = null,
                                tint = Color(0xFF67E8F9),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${day.precipitationProb}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF67E8F9)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(42.dp))
                    }

                    // Min Temp
                    Text(
                        text = "$minTemp°",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.width(32.dp)
                    )

                    // Visual Temperature Range Slider Bar
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.20f))
                    ) {
                        val startFrac = ((day.minTempCelsius - globalMin) / tempSpread).coerceIn(0.0, 1.0).toFloat()
                        val endFrac = ((day.maxTempCelsius - globalMin) / tempSpread).coerceIn(0.0, 1.0).toFloat()
                        val widthFrac = (endFrac - startFrac).coerceAtLeast(0.08f)

                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(widthFrac)
                                .align(Alignment.CenterStart)
                                .offset(x = (startFrac * 100).dp) // approximate visual offset
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF38BDF8), Color(0xFFFFD54F), Color(0xFFF97316))
                                    )
                                )
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Max Temp
                    Text(
                        text = "$maxTemp°",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.width(32.dp)
                    )

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Expanded Info
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, start = 8.dp, end = 8.dp)
                    ) {
                        Text(
                            text = "الحالة: ${day.conditionNameAr}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFFD54F)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.WbSunny,
                                    contentDescription = null,
                                    tint = Color(0xFFFFCA28),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "شروق: ${day.sunrise}",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.WbTwilight,
                                    contentDescription = null,
                                    tint = Color(0xFFFF7043),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "غروب: ${day.sunset}",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }

                            Text(
                                text = "مؤشر UV: ${day.uvIndexMax.toInt()}",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }
        }
    }
}
