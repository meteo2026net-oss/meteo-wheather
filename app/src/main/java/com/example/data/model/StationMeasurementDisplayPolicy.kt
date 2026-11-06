package com.example.data.model

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Field-level guards for legacy station records.
 *
 * Station freshness/verification is necessary but not sufficient: each physical
 * measurement must also be finite and within a deliberately broad valid range.
 */
object StationMeasurementDisplayPolicy {
    /** Formats a provider timestamp for station cards; invalid timestamps stay hidden. */
    fun formatObservationTime(
        observedAt: String,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String? = runCatching {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.getDefault())
        Instant.parse(observedAt).atZone(zoneId).format(formatter)
    }.getOrNull()

    fun temperatureCelsius(value: Double): Boolean =
        value.isFinite() && value in -100.0..70.0

    fun relativeHumidityPercent(value: Int): Boolean =
        value in 0..100

    fun pressureHpa(value: Double): Boolean =
        value.isFinite() && value in 800.0..1100.0

    fun windSpeedKmh(value: Double): Boolean =
        value.isFinite() && value in 0.0..300.0
}

fun SyrianWeatherStation.canShowLiveTemperature(
    nowEpochMillis: Long = System.currentTimeMillis()
): Boolean = canShowLiveMeasurements(nowEpochMillis) &&
    StationMeasurementDisplayPolicy.temperatureCelsius(temperatureCelsius)

fun SyrianWeatherStation.canShowLiveHumidity(
    nowEpochMillis: Long = System.currentTimeMillis()
): Boolean = canShowLiveMeasurements(nowEpochMillis) &&
    StationMeasurementDisplayPolicy.relativeHumidityPercent(humidity)

fun SyrianWeatherStation.canShowLivePressure(
    nowEpochMillis: Long = System.currentTimeMillis()
): Boolean = canShowLiveMeasurements(nowEpochMillis) &&
    StationMeasurementDisplayPolicy.pressureHpa(pressureHpa)

fun SyrianWeatherStation.canShowLiveWindSpeed(
    nowEpochMillis: Long = System.currentTimeMillis()
): Boolean = canShowLiveMeasurements(nowEpochMillis) &&
    StationMeasurementDisplayPolicy.windSpeedKmh(windSpeedKmh)
