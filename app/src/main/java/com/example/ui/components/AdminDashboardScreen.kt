package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AlertSeverity
import com.example.data.model.CapAlertItem
import com.example.data.model.SevereWeatherAlert
import com.example.data.model.SyrianWeatherStation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    stations: List<SyrianWeatherStation>,
    activeAlerts: List<SevereWeatherAlert>,
    onBroadcastCapAlert: (CapAlertItem) -> Unit,
    onPublishAudioBulletin: (String, String) -> Unit
) {
    if (!isOpen) return

    var selectedAdminTab by remember { mutableIntStateOf(0) } // 0: CAP Alerts, 1: Stations Telemetry, 2: Audio Bulletin Publisher

    // CAP Alert broadcast form states
    var alertTitle by remember { mutableStateOf("تحذير جوي عاجل - المركز الوطني (MCNC)") }
    var alertArea by remember { mutableStateOf("دمشق وريفها، حمص، الساحل السوري") }
    var alertDesc by remember { mutableStateOf("رياح نشطة هابطة وأمطار رعدية غزيرة متوقعة خلال الساعات القادمة.") }
    var alertInstructions by remember { mutableStateOf("تجنب القيادة السريعة، إحكام إغلاق النوافذ، والابتعاد عن مجاري السيول.") }
    var alertSeverity by remember { mutableStateOf(AlertSeverity.WARNING) }

    // Audio Bulletin form states
    var bulletinTitle by remember { mutableStateOf("النشرة الجوية الصباحية الشاملة") }
    var bulletinScript by remember { mutableStateOf("تستمر درجات الحرارة أدنى من معدلاتها بقليل مع بقاء الفرصة مهيأة لهطول أمطار متفرقة على المناطق الساحلية والشمالية...") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 36.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEA580C)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "لوحة إدارة الأرصاد السورية (MCNC Admin)",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "إدارة محطات الرصد، تنبيهات كاب (CAP)، ونشرk النشرات الصوتية",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق",
                        tint = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Admin Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val tabs = listOf("بث تنبيهات CAP", "حالة محطات الرصد", "إدارة النشرة الصوتية")
                tabs.forEachIndexed { idx, tabTitle ->
                    val isSelected = selectedAdminTab == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color(0xFF0284C7) else Color.White.copy(alpha = 0.1f))
                            .border(1.dp, if (isSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .clickable { selectedAdminTab = idx }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tabTitle,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab Content
            when (selectedAdminTab) {
                0 -> {
                    // CAP Alerts Publisher Form
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = "إرسال تنبيه طوارئ بروتوكول كاب (CAP Broadcast)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD54F)
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = alertTitle,
                                onValueChange = { alertTitle = it },
                                label = { Text("عنوان الإنذار", color = Color.White.copy(alpha = 0.7f)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = alertArea,
                                onValueChange = { alertArea = it },
                                label = { Text("المناطق المتأثرة", color = Color.White.copy(alpha = 0.7f)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = alertDesc,
                                onValueChange = { alertDesc = it },
                                label = { Text("تفاصيل الحالة الجوية", color = Color.White.copy(alpha = 0.7f)) },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 3,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = alertInstructions,
                                onValueChange = { alertInstructions = it },
                                label = { Text("تعليمات السلامة العامة", color = Color.White.copy(alpha = 0.7f)) },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 3,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                        }

                        item {
                            Button(
                                onClick = {
                                    val newAlert = CapAlertItem(
                                        identifier = "MCNC_CAP_${System.currentTimeMillis()}",
                                        sentTimestamp = "الآن",
                                        eventAr = alertTitle,
                                        severity = alertSeverity,
                                        areaDescAr = alertArea,
                                        descriptionAr = alertDesc,
                                        instructionAr = alertInstructions,
                                        expires = "خلال 12 ساعة"
                                    )
                                    onBroadcastCapAlert(newAlert)
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("broadcast_cap_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C))
                            ) {
                                Icon(imageVector = Icons.Default.Send, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "بث الإنذار الفوري لجميع المستخدمين", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                1 -> {
                    // Stations Telemetry View
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text(
                                text = "حالة ربط محطات الأرصاد الجوية السورية",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD54F)
                            )
                        }
                        items(stations) { st ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(if (st.isOnline) Color(0xFF4ADE80) else Color(0xFFEF4444))
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(text = st.nameAr, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text(text = "${st.governorate} | ضغط: ${st.pressureHpa} hPa", fontSize = 10.sp, color = Color.White.copy(alpha = 0.65f))
                                    }
                                }
                                Text(
                                    text = "${st.temperatureCelsius.toInt()}°C",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFD54F)
                                )
                            }
                        }
                    }
                }

                2 -> {
                    // Audio Bulletin Publisher Form
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = "تحديث ونشر النشرة الصوتية اليومية (MCNC)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD54F)
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = bulletinTitle,
                                onValueChange = { bulletinTitle = it },
                                label = { Text("عنوان النشرة", color = Color.White.copy(alpha = 0.7f)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = bulletinScript,
                                onValueChange = { bulletinScript = it },
                                label = { Text("نص النشرة الصوتية", color = Color.White.copy(alpha = 0.7f)) },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 5,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                        }

                        item {
                            Button(
                                onClick = {
                                    onPublishAudioBulletin(bulletinTitle, bulletinScript)
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("publish_audio_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                            ) {
                                Icon(imageVector = Icons.Default.Mic, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "نشر وتحديث النشرة الصوتية فورياً", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
