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
    val isOnline: Boolean = true,
    val lastReadingTime: String = "منذ 10 دقائق"
)

data class CapAlertItem(
    val identifier: String,
    val sender: String = "MCNC.SY.CAP@meteo.gov.sy",
    val sentTimestamp: String,
    val status: String = "Actual", // Actual, Exercise, Test
    val msgType: String = "Alert", // Alert, Update, Cancel
    val scope: String = "Public",
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
    val bulletinScriptAr: String
)

data class ChatMessageItem(
    val id: String,
    val sender: String, // "user" or "gemini"
    val text: String,
    val timestamp: String
)
