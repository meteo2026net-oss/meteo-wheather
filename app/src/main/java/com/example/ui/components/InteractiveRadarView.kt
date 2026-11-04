package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun InteractiveRadarView(
    cityName: String,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(true) }
    var zoomLevel by remember { mutableFloatStateOf(1.0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "radarSweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepAngle"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.16f))
            .border(1.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Radar,
                    contentDescription = null,
                    tint = Color(0xFF67E8F9),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "رادار الأمطار والسحب التفاعلي",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "عرض تجريبي توضيحي لمنطقة $cityName",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
            }

            // Radar Control Buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable { isPlaying = !isPlaying },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "إيقاف مؤقت" else "تشغيل",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable {
                            zoomLevel = if (zoomLevel == 1.0f) 1.35f else 1.0f
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (zoomLevel == 1.0f) Icons.Default.ZoomIn else Icons.Default.ZoomOut,
                        contentDescription = "تقريب",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Radar Canvas Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF0F172A))
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val center = Offset(w / 2f, h / 2f)
                val maxRadius = (minOf(w, h) / 2f) * 0.9f * zoomLevel

                // Concentric Range Rings
                val ringCount = 3
                for (i in 1..ringCount) {
                    val r = (maxRadius / ringCount) * i
                    drawCircle(
                        color = Color(0xFF1E293B),
                        radius = r,
                        center = center,
                        style = Stroke(width = 1.2.dp.toPx())
                    )
                }

                // Crosshairs
                drawLine(
                    color = Color(0xFF1E293B),
                    start = Offset(center.x - maxRadius, center.y),
                    end = Offset(center.x + maxRadius, center.y),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = Color(0xFF1E293B),
                    start = Offset(center.x, center.y - maxRadius),
                    end = Offset(center.x, center.y + maxRadius),
                    strokeWidth = 1.dp.toPx()
                )

                // Simulated Weather Cells (clouds & rain echoes)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF22C55E).copy(alpha = 0.55f), Color(0xFF10B981).copy(alpha = 0.25f), Color.Transparent),
                        center = Offset(center.x + 35.dp.toPx() * zoomLevel, center.y - 25.dp.toPx() * zoomLevel),
                        radius = 42.dp.toPx() * zoomLevel
                    ),
                    radius = 42.dp.toPx() * zoomLevel,
                    center = Offset(center.x + 35.dp.toPx() * zoomLevel, center.y - 25.dp.toPx() * zoomLevel)
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFEAB308).copy(alpha = 0.65f), Color(0xFF22C55E).copy(alpha = 0.35f), Color.Transparent),
                        center = Offset(center.x - 45.dp.toPx() * zoomLevel, center.y + 15.dp.toPx() * zoomLevel),
                        radius = 35.dp.toPx() * zoomLevel
                    ),
                    radius = 35.dp.toPx() * zoomLevel,
                    center = Offset(center.x - 45.dp.toPx() * zoomLevel, center.y + 15.dp.toPx() * zoomLevel)
                )

                // High Intensity Core in one of the clouds
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFEF4444).copy(alpha = 0.75f), Color(0xFFF97316).copy(alpha = 0.4f), Color.Transparent),
                        center = Offset(center.x - 45.dp.toPx() * zoomLevel, center.y + 15.dp.toPx() * zoomLevel),
                        radius = 16.dp.toPx() * zoomLevel
                    ),
                    radius = 16.dp.toPx() * zoomLevel,
                    center = Offset(center.x - 45.dp.toPx() * zoomLevel, center.y + 15.dp.toPx() * zoomLevel)
                )

                // Animated Radar Sweep Sector & Line
                if (isPlaying) {
                    val rad = Math.toRadians(sweepAngle.toDouble())
                    val sweepEnd = Offset(
                        (center.x + maxRadius * cos(rad)).toFloat(),
                        (center.y + maxRadius * sin(rad)).toFloat()
                    )

                    // Sweep Line
                    drawLine(
                        brush = Brush.linearGradient(
                            listOf(Color(0xFF06B6D4), Color(0xFF22D3EE))
                        ),
                        start = center,
                        end = sweepEnd,
                        strokeWidth = 2.dp.toPx()
                    )
                }

                // Center City Pin Dot
                drawCircle(color = Color(0xFF38BDF8), radius = 4.dp.toPx(), center = center)
                drawCircle(color = Color.White, radius = 2.dp.toPx(), center = center)
            }

            // Radar City Label overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "نطاق الرادار: 120 كم",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Intensity Color Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "شدة الهطول:",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                LegendPill("خفيف", Color(0xFF22C55E))
                Spacer(modifier = Modifier.width(6.dp))
                LegendPill("متوسط", Color(0xFFEAB308))
                Spacer(modifier = Modifier.width(6.dp))
                LegendPill("غزير", Color(0xFFEF4444))
            }
        }
    }
}

@Composable
private fun LegendPill(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.85f)
        )
    }
}
