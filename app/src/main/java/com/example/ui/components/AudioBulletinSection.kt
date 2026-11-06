package com.example.ui.components

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
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
    var ttsInstance by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsInitialized by remember { mutableStateOf(false) }

    // Initialize TextToSpeech engine for Arabic speech synthesis
    DisposableEffect(context) {
        val tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = ttsInstance?.setLanguage(Locale.forLanguageTag("ar"))
                ttsInitialized = (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED)
            }
        }
        ttsInstance = tts
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isPlaying = true
            }

            override fun onDone(utteranceId: String?) {
                isPlaying = false
            }

            override fun onError(utteranceId: String?) {
                isPlaying = false
            }
        })
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    // Handle playback using Android TTS lifecycle callbacks; do not simulate a timeline.
    LaunchedEffect(isPlaying, ttsInitialized) {
        if (isPlaying) {
            if (ttsInitialized && ttsInstance != null) {
                val result = ttsInstance?.speak(
                    bulletin.bulletinScriptAr,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "mcnc_bulletin_utterance"
                )
                if (result != TextToSpeech.SUCCESS) isPlaying = false
            } else {
                isPlaying = false
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
                        text = if (bulletin.isDemoData) "نشرة تجريبية — تحويل النص إلى صوت" else "النشرة الصوتية",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "صوت اصطناعي: ${bulletin.announcerName} | ${bulletin.dateFormatted}",
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
                    text = if (bulletin.isDemoData) "تجريبي" else "TTS",
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

        // Playback state indicator (TTS does not expose a reliable seek timeline).
        Column(modifier = Modifier.fillMaxWidth()) {
            if (isPlaying) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = Color(0xFFFFD54F),
                    trackColor = Color.White.copy(alpha = 0.2f)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isPlaying) "تجري قراءة النص بصوت اصطناعي..." else "اضغط لتشغيل القراءة الصوتية للنص",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )

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
                        contentDescription = if (isPlaying) "إيقاف القراءة" else "تشغيل القراءة",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
