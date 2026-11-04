package com.example.ui.components

import android.content.Context
import android.speech.tts.TextToSpeech
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FullWeatherData
import java.util.Locale

@Composable
fun GeminiVoiceConversationSection(
    weatherData: FullWeatherData?,
    isListening: Boolean,
    isSpeaking: Boolean,
    lastVoiceQuery: String,
    lastAiVoiceResponse: String,
    onStartListeningClick: () -> Unit,
    onStopVoiceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var ttsInstance by remember { mutableStateOf<TextToSpeech?>(null) }

    // Waveform Pulsing Animation for Voice Active State
    val infiniteTransition = rememberInfiniteTransition(label = "voicePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    DisposableEffect(Unit) {
        var tts: TextToSpeech? = null
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("ar")
            }
        }
        ttsInstance = tts
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Color.White.copy(alpha = 0.16f))
            .border(1.5.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(26.dp))
            .padding(16.dp)
            .testTag("gemini_voice_section")
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
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEA580C)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "قسم المحادثة الصوتية (Gemini Voice)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "تحدث مع المساعد الصوتي واسمعه يجيبك باللغة العربية",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
            }

            if (isSpeaking) {
                IconButton(onClick = {
                    ttsInstance?.stop()
                    onStopVoiceClick()
                }) {
                    Icon(
                        imageVector = Icons.Default.VolumeOff,
                        contentDescription = "إيقاف الصوت",
                        tint = Color(0xFFFF6B6B)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Center Microphone Active Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0F172A))
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size((64 * if (isListening || isSpeaking) pulseScale else 1.0f).dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isListening -> Color(0xFFEF4444)
                                isSpeaking -> Color(0xFF0284C7)
                                else -> Color(0xFFEA580C)
                            }
                        )
                        .clickable { onStartListeningClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            isListening -> Icons.Default.GraphicEq
                            isSpeaking -> Icons.Default.VolumeUp
                            else -> Icons.Default.Mic
                        },
                        contentDescription = "تحدث الآن",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = when {
                        isListening -> "جاري الاستماع لصوتك... اضغط للإلغاء"
                        isSpeaking -> "Gemini يتحدث بصوت جهوري..."
                        else -> "اضغط على الميكروفون للبدء بالمحادثة الصوتية"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Last Voice Query & AI Response Box
        if (lastVoiceQuery.isNotEmpty() || lastAiVoiceResponse.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1E293B))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Column {
                    if (lastVoiceQuery.isNotEmpty()) {
                        Text(
                            text = "🗣️ سُمع منك: \"$lastVoiceQuery\"",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD54F)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    if (lastAiVoiceResponse.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🤖 رد Gemini الصوتي:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8)
                            )

                            IconButton(
                                onClick = {
                                    ttsInstance?.stop()
                                    ttsInstance?.speak(
                                        lastAiVoiceResponse,
                                        TextToSpeech.QUEUE_FLUSH,
                                        null,
                                        "MCNC_AI_SPEECH"
                                    )
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "إعادة التشغيل الصوتي",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = lastAiVoiceResponse,
                            fontSize = 12.sp,
                            color = Color.White,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }
    }
}
