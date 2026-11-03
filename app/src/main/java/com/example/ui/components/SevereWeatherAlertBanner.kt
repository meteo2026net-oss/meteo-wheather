package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AlertSeverity
import com.example.data.model.SevereWeatherAlert

@Composable
fun SevereWeatherAlertBanner(
    alerts: List<SevereWeatherAlert>,
    onTestNotification: (SevereWeatherAlert) -> Unit,
    modifier: Modifier = Modifier
) {
    if (alerts.isEmpty()) return

    var isExpanded by remember { mutableStateOf(false) }
    var selectedAlertIndex by remember { mutableIntStateOf(0) }
    val currentAlert = alerts.getOrElse(selectedAlertIndex) { alerts.first() }

    val infiniteTransition = rememberInfiniteTransition(label = "pulseAlert")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val (badgeBg, borderColor, iconTint) = when (currentAlert.severity) {
        AlertSeverity.EMERGENCY -> Triple(Color(0xFFDC2626), Color(0xFFEF4444), Color(0xFFFFE4E6))
        AlertSeverity.WARNING -> Triple(Color(0xFFEA580C), Color(0xFFF97316), Color(0xFFFFEDD5))
        AlertSeverity.WATCH -> Triple(Color(0xFFD97706), Color(0xFFFBBF24), Color(0xFFFEF3C7))
        AlertSeverity.ADVISORY -> Triple(Color(0xFFCA8A04), Color(0xFFFACC15), Color(0xFFFEF9C3))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.verticalGradient(
                    listOf(badgeBg.copy(alpha = 0.35f * pulseAlpha), badgeBg.copy(alpha = 0.15f))
                )
            )
            .border(1.5.dp, borderColor.copy(alpha = 0.8f), RoundedCornerShape(22.dp))
            .padding(14.dp)
            .animateContentSize()
            .testTag("severe_weather_alert_banner")
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(badgeBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(badgeBg)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = when (currentAlert.severity) {
                                    AlertSeverity.EMERGENCY -> "إنذار طوارئ قصوى"
                                    AlertSeverity.WARNING -> "تحذير جوي رسمي"
                                    AlertSeverity.WATCH -> "متابعة وتقلبات جوية"
                                    AlertSeverity.ADVISORY -> "إرشاد وتنبيه"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        if (alerts.size > 1) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(${selectedAlertIndex + 1}/${alerts.size})",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = currentAlert.titleAr,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            IconButton(
                onClick = { isExpanded = !isExpanded },
                modifier = Modifier.testTag("expand_alert_button")
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "طي" else "توسيع التفاصيل",
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Short Summary
        Text(
            text = currentAlert.descriptionAr,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.9f),
            lineHeight = 17.sp
        )

        // Expanded Details
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                // Safety Instructions Box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black.copy(alpha = 0.3f))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.HealthAndSafety,
                            contentDescription = null,
                            tint = Color(0xFF4ADE80),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "إرشادات السلامة الوقائية:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4ADE80)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = currentAlert.safetyInstructionsAr,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.92f),
                        lineHeight = 17.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Source & Validity Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "المصدر: ${currentAlert.source}",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "الصلاحية: ${currentAlert.validUntil}",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    // Test Push Notification Button
                    OutlinedButton(
                        onClick = { onTestNotification(currentAlert) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("test_push_alert_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "إرسال إشعار فوري", fontSize = 11.sp)
                    }
                }

                // If multiple alerts, allow switching
                if (alerts.size > 1) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        alerts.forEachIndexed { idx, _ ->
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .size(if (idx == selectedAlertIndex) 18.dp else 8.dp, 8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (idx == selectedAlertIndex) Color.White else Color.White.copy(alpha = 0.35f)
                                    )
                                    .clickable { selectedAlertIndex = idx }
                            )
                        }
                    }
                }
            }
        }
    }
}
