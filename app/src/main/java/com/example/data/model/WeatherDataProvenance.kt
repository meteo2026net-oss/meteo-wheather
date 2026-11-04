package com.example.data.model

/**
 * Provenance and quality metadata for weather values.
 *
 * Keep absent measurements as null. A missing value must never be replaced
 * with a plausible-looking zero or a climatological/default value.
 */
enum class WeatherDataKind {
    OBSERVATION,
    FORECAST,
    MODEL_ANALYSIS,
    SIMULATION,
    MANUAL,
    UNKNOWN
}

enum class WeatherDataStatus {
    AVAILABLE,
    STALE,
    UNAVAILABLE,
    SIMULATED
}

data class WeatherDataProvenance(
    val provider: String,
    val kind: WeatherDataKind,
    val status: WeatherDataStatus,
    /** Unix epoch milliseconds, nullable when the provider supplies no timestamp. */
    val observedAtEpochMillis: Long? = null,
    /** Unix epoch milliseconds when this app retrieved the record. */
    val retrievedAtEpochMillis: Long? = null,
    val attribution: String? = null,
    val sourceRecordId: String? = null
)

/**
 * A measurement with an explicit unit and provenance.
 * [value] is nullable to distinguish missing data from a measured zero.
 */
data class QualifiedWeatherValue(
    val value: Double?,
    val unit: String,
    val provenance: WeatherDataProvenance
) {
    val isDisplayable: Boolean
        get() = value != null &&
            provenance.status != WeatherDataStatus.UNAVAILABLE
}

/**
 * A real station report. This model deliberately allows missing measurements;
 * station identity and coordinates can still be shown when observations are absent.
 */
data class StationObservation(
    val stationId: String,
    val stationNameAr: String,
    val governorateAr: String,
    val latitude: Double,
    val longitude: Double,
    val temperatureCelsius: QualifiedWeatherValue?,
    val relativeHumidityPercent: QualifiedWeatherValue?,
    val pressureHpa: QualifiedWeatherValue?,
    val windSpeedKmh: QualifiedWeatherValue?,
    val windDirectionDegrees: QualifiedWeatherValue?,
    val conditionCode: Int?,
    val provenance: WeatherDataProvenance
)
