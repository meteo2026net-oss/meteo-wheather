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
    /**
     * Only verified, current measurements are eligible for presentation as readings.
     * Stale and simulated values may be shown in a separately labelled history/demo UI,
     * but must never pass the live-measurement display gate.
     */
    val isDisplayable: Boolean
        get() = value?.isFinite() == true &&
            provenance.status == WeatherDataStatus.AVAILABLE
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

/**
 * Converts the legacy station record into the provenance-aware domain model.
 *
 * Existing hard-coded station fixtures are explicitly tagged as simulated.
 * A record marked non-demo is not assumed to be live: until a verified feed
 * supplies timestamps and source metadata, its measurements remain unavailable.
 */
fun SyrianWeatherStation.toQualifiedObservation(): StationObservation {
    val isSimulation = isDemoData
    val source = WeatherDataProvenance(
        provider = if (isSimulation) "MCNC local demonstration fixtures" else "Unverified station source",
        kind = if (isSimulation) WeatherDataKind.SIMULATION else WeatherDataKind.UNKNOWN,
        status = if (isSimulation) WeatherDataStatus.SIMULATED else WeatherDataStatus.UNAVAILABLE,
        attribution = if (isSimulation) "قيم ثابتة للتوضيح فقط؛ ليست قياسات رصد فعلية." else null
    )

    fun measurement(value: Double, unit: String): QualifiedWeatherValue? =
        if (isSimulation) QualifiedWeatherValue(value, unit, source) else null

    return StationObservation(
        stationId = id,
        stationNameAr = nameAr,
        governorateAr = governorate,
        latitude = latitude,
        longitude = longitude,
        temperatureCelsius = measurement(temperatureCelsius, "°C"),
        relativeHumidityPercent = measurement(humidity.toDouble(), "%"),
        pressureHpa = measurement(pressureHpa, "hPa"),
        windSpeedKmh = measurement(windSpeedKmh, "km/h"),
        windDirectionDegrees = null,
        conditionCode = null,
        provenance = source
    )
}
