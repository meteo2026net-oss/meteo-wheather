package com.example.data.model

data class SyrianWeatherStation(
    val id: String,
    val nameAr: String,
    val governorate: String,
    val latitude: Double,
    val longitude: Double,
    val temperatureCelsius: Double,
    val humidity: Int,
    val pressureHpa: Double,
    val windSpeedKmh: Double,
    val conditionAr: String,
    val conditionType: WeatherConditionType,
    val isOnline: Boolean = false,
    val lastReadingTime: String = "غير محدد",
    val isDemoData: Boolean = true
)

data class CapAlertItem(
    val identifier: String,
    val sender: String = "local-demo@device.invalid",
    val sentTimestamp: String,
    val status: String = "Test", // Actual, Exercise, Test
    val msgType: String = "Alert", // Alert, Update, Cancel
    val scope: String = "Private",
    val eventAr: String,
    val severity: AlertSeverity, // ADVISORY, WATCH, WARNING, EMERGENCY
    val urgency: String = "Immediate",
    val certainty: String = "Observed",
    val areaDescAr: String,
    val descriptionAr: String,
    val instructionAr: String,
    val expires: String
)

data class AudioBulletinItem(
    val id: String,
    val title: String,
    val dateFormatted: String,
    val durationSec: Int,
    val announcerName: String,
    val bulletinScriptAr: String,
    val isDemoData: Boolean = true
)


data class ChatMessageItem(
    val id: String,
    val sender: String,
    val text: String,
    val timestamp: String
)
