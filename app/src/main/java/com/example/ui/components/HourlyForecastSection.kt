package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.example.data.model.HourlyForecast

@Composable
fun HourlyForecastSection(
    hourlyList: List<HourlyForecast>,
    isFahrenheit: Boolean,
    modifier: Modifier = Modifier
) {
    var selectedHourIndex by remember { mutableIntStateOf(0) }
    val selectedHour = hourlyList.getOrNull(selectedHourIndex)

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
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "التوقعات الساعية (24 ساعة)",
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

        // Hourly Horizontal List
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            itemsIndexed(hourlyList) { index, hour ->
                val isSelected = index == selectedHourIndex
                val temp = if (isFahrenheit) {
                    (hour.temperatureCelsius * 9 / 5 + 32).toInt()
                } else {
                    hour.temperatureCelsius.toInt()
                }

                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isSelected) {
                                Brush.verticalGradient(
                                    listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.22f))
                                )
                            } else {
                                Brush.verticalGradient(
                                    listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.05f))
                                )
                            }
                        )
                        .border(
                            width = if (isSelected) 1.8.dp else 1.dp,
                            color = if (isSelected) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.20f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { selectedHourIndex = index }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = hour.hourLabel,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color(0xFFFFD54F) else Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    AnimatedWeatherIcon(
                        condition = hour.conditionType,
                        isDay = hour.isDay,
                        size = 38.dp,
                        interactiveTapEnabled = false
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "$temp°",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    if (hour.precipitationProb > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = null,
                                tint = Color(0xFF67E8F9),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${hour.precipitationProb}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF67E8F9)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(15.dp))
                    }
                }
            }
        }

        // Detailed Inspector for Selected Hour
        selectedHour?.let { hour ->
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black.copy(alpha = 0.20f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // Time
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "الساعة: ",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                    Text(
                        text = hour.hourLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD54F)
                    )
                }

                // Humidity
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${hour.humidity}% رطوبة",
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }

                // Wind
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Air,
                        contentDescription = null,
                        tint = Color(0xFFE2E8F0),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${hour.windSpeedKmh.toInt()} كم/س",
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}
