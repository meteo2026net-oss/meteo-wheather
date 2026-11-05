package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SyrianWeatherStation
import com.example.data.model.canShowLiveMeasurements

@Composable
fun SyrianStationsSection(
    stations: List<SyrianWeatherStation>,
    onStationSelected: (SyrianWeatherStation) -> Unit,
    modifier: Modifier = Modifier
) {
    val nowEpochMillis = System.currentTimeMillis()
    val hasFreshVerifiedReadings = stations.any { station ->
        station.canShowLiveMeasurements(nowEpochMillis)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CellTower,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "محطات الأرصاد الجوية السورية",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (hasFreshVerifiedReadings) {
                            "توجد قراءات حديثة من مصادر موثقة"
                        } else {
                            "لا توجد حالياً قراءات حية موثقة — القيم غير المتاحة مخفية"
                        },
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(stations) { station ->
                val hasVerifiedSource = station.hasVerifiedLiveData && !station.isDemoData
                val canShowLiveMeasurements = station.canShowLiveMeasurements(nowEpochMillis)
                Column(
                    modifier = Modifier
                        .width(160.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                        .clickable { onStationSelected(station) }
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = station.nameAr,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (station.isDemoData) Color(0xFFFACC15)
                                    else if (canShowLiveMeasurements) Color(0xFF4ADE80)
                                    else Color(0xFFEF4444)
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = station.governorate,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.65f)
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = when {
                            station.isDemoData -> "بيانات تجريبية غير حية"
                            !hasVerifiedSource -> "مصدر القياسات غير موثق"
                            !station.isOnline -> "المحطة غير متصلة"
                            canShowLiveMeasurements -> "متصلة — قراءة حديثة موثقة"
                            station.lastReadingEpochMillis != null -> "القراءة قديمة أو توقيتها غير صالح"
                            else -> "وقت آخر قراءة غير متاح"
                        },
                        fontSize = 9.sp,
                        color = if (station.isDemoData) Color(0xFFFACC15)
                        else if (canShowLiveMeasurements) Color(0xFF4ADE80)
                        else Color(0xFFFCA5A5)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = if (canShowLiveMeasurements) "${station.temperatureCelsius.toInt()}°" else "—",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD54F)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (canShowLiveMeasurements) station.conditionAr else "لا توجد قراءة موثقة",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.padding(bottom = 6.dp),
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (canShowLiveMeasurements) "رطوبة: ${station.humidity}%" else "رطوبة: —",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = if (canShowLiveMeasurements) "رياح: ${station.windSpeedKmh.toInt()} كم" else "رياح: —",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}
