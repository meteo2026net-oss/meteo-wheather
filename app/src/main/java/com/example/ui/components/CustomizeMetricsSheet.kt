package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.example.data.model.MetricPreference
import com.example.data.model.WeatherMetricType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizeMetricsSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    currentPreferences: List<MetricPreference>,
    onSavePreferences: (List<MetricPreference>) -> Unit,
    alertsNotificationEnabled: Boolean,
    onToggleAlertsNotification: (Boolean) -> Unit
) {
    if (!isOpen) return

    var localPreferences by remember(currentPreferences) {
        mutableStateOf(currentPreferences.toMutableList())
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E293B),
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 36.dp)
        ) {
            // Sheet Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF38BDF8).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "تخصيص وترتيب بيانات الطقس",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "حدد المؤشرات التي تهمك ورتّب أولويتها",
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

            // Severe Alerts Push Notification Toggle Box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFEA580C).copy(alpha = 0.18f))
                    .border(1.dp, Color(0xFFEA580C).copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.NotificationImportant,
                        contentDescription = null,
                        tint = Color(0xFFFB923C),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "إشعارات الإنذار المبكر والطقس الشديد",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "استلام تنبيهات فورية للعواصف والسيول وموجات الحر",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                }

                Switch(
                    checked = alertsNotificationEnabled,
                    onCheckedChange = onToggleAlertsNotification,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFFEA580C)
                    ),
                    modifier = Modifier.testTag("toggle_alerts_notification_switch")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action row: Reset to default
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ترتيب ظهور البطاقات (استخدم الأسهم للتقديم):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.85f)
                )

                TextButton(
                    onClick = {
                        val reset = com.example.data.repository.WeatherRepository.DEFAULT_METRIC_PREFERENCES.toMutableList()
                        localPreferences = reset
                        onSavePreferences(reset)
                    }
                ) {
                    Text(text = "استعادة الافتراضي", fontSize = 11.sp, color = Color(0xFFFFD54F))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Reorderable & Toggleable Metric Items List
            LazyColumn(
                modifier = Modifier.heightIn(max = 340.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(localPreferences) { index, pref ->
                    val isFirst = index == 0
                    val isLast = index == localPreferences.size - 1

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (pref.isEnabled) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.05f)
                            )
                            .border(
                                1.dp,
                                if (pref.isEnabled) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f),
                                RoundedCornerShape(14.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Checkbox & Title
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Checkbox(
                                checked = pref.isEnabled,
                                onCheckedChange = { isChecked ->
                                    val updated = localPreferences.toMutableList()
                                    updated[index] = pref.copy(isEnabled = isChecked)
                                    localPreferences = updated
                                    onSavePreferences(updated)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF38BDF8),
                                    uncheckedColor = Color.White.copy(alpha = 0.4f)
                                )
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            Column {
                                Text(
                                    text = pref.type.titleAr,
                                    fontSize = 13.sp,
                                    fontWeight = if (pref.isEnabled) FontWeight.Bold else FontWeight.Normal,
                                    color = if (pref.isEnabled) Color.White else Color.White.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = pref.type.subtitleAr,
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }

                        // Up / Down Priority Buttons
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    if (!isFirst) {
                                        val updated = localPreferences.toMutableList()
                                        val item = updated.removeAt(index)
                                        updated.add(index - 1, item)
                                        localPreferences = updated
                                        onSavePreferences(updated)
                                    }
                                },
                                enabled = !isFirst,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = "تحريك لأعلى",
                                    tint = if (!isFirst) Color.White else Color.White.copy(alpha = 0.2f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (!isLast) {
                                        val updated = localPreferences.toMutableList()
                                        val item = updated.removeAt(index)
                                        updated.add(index + 1, item)
                                        localPreferences = updated
                                        onSavePreferences(updated)
                                    }
                                },
                                enabled = !isLast,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = "تحريك لأسفل",
                                    tint = if (!isLast) Color.White else Color.White.copy(alpha = 0.2f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Done Button
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_customization_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
            ) {
                Text(
                    text = "تم وحفظ التفضيلات",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
