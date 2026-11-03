package com.example.ui.components

import android.speech.tts.TextToSpeech
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AudioBulletinItem
import kotlinx.coroutines.delay
import java.util.*

@Composable
fun AudioBulletinSection(
    bulletin: AudioBulletinItem,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var playbackProgress by remember { mutableFloatStateOf(0f) }
    var ttsInstance by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsInitialized by remember { mutableStateOf(false) }

    // Initialize TextToSpeech engine for Arabic speech synthesis
    DisposableEffect(context) {
        val tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = ttsInstance?.setLanguage(Locale("ar"))
                ttsInitialized = (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED)
            }
        }
        ttsInstance = tts
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    // Handle playback state & TTS speech
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            if (ttsInitialized && ttsInstance != null) {
                ttsInstance?.speak(
                    bulletin.bulletinScriptAr,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "mcnc_bulletin_utterance"
                )
            }
            while (isPlaying) {
                delay(400)
                playbackProgress += 0.04f
                if (playbackProgress >= 1f) {
                    playbackProgress = 0f
                    isPlaying = false
                }
            }
        } else {
            ttsInstance?.stop()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0284C7).copy(alpha = 0.38f), Color(0xFF0F172A).copy(alpha = 0.55f))
                )
            )
            .border(1.5.dp, Color(0xFF38BDF8).copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            .padding(16.dp)
            .testTag("audio_bulletin_card")
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
                        .background(Color(0xFF0284C7)),
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
                        text = "النشرة الصوتية الرسمية (MCNC TTS)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "قراءة صوتية حية: ${bulletin.announcerName} | ${bulletin.dateFormatted}",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFFFD54F).copy(alpha = 0.25f))
                    .border(1.dp, Color(0xFFFFD54F), RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "صوت ناطق",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD54F)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Bulletin Title
        Text(
            text = bulletin.title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFFD54F)
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Bulletin Script Text
        Text(
            text = bulletin.bulletinScriptAr,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.92f),
            lineHeight = 19.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Playback Bar & Controls
        Column(modifier = Modifier.fillMaxWidth()) {
            LinearProgressIndicator(
                progress = { playbackProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = Color(0xFFFFD54F),
                trackColor = Color.White.copy(alpha = 0.2f),
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isPlaying) "جاري قراءة النشرة الصوتية بصوت واضح..." else "اضغط لتشغيل القراءة الصوتية للنشرة",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Replay 10s button
                    IconButton(
                        onClick = { playbackProgress = (playbackProgress - 0.1f).coerceAtLeast(0f) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Replay10, contentDescription = "تخلف 10 ثواني", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Play / Pause Button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0284C7))
                            .clickable { isPlaying = !isPlaying }
                            .testTag("audio_play_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "إيقاف مؤقت" else "تشغيل",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Forward 10s button
                    IconButton(
                        onClick = { playbackProgress = (playbackProgress + 0.1f).coerceAtMost(1f) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Forward10, contentDescription = "تقدم 10 ثواني", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
