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
     * True when a finite value may be shown in any context.
     * Simulated values remain eligible only for explicitly labelled demo UI.
     * Stale and unavailable values are never displayable.
     */
    val isDisplayable: Boolean
        get() = value?.isFinite() == true &&
            provenance.status in setOf(
                WeatherDataStatus.AVAILABLE,
                WeatherDataStatus.SIMULATED
            )

    /**
     * Strict gate for live readings: only finite, available measurements pass.
     * Demo/simulated values must use [isDisplayable] only in a clearly labelled demo UI.
     */
    val isLiveDisplayable: Boolean
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
 * A station can provide live values only after its source was verified,
 * the station is online, and its provider timestamp is within the freshness window.
 */
fun SyrianWeatherStation.canShowLiveMeasurements(
    nowEpochMillis: Long = System.currentTimeMillis()
): Boolean =
    hasVerifiedLiveData &&
        isOnline &&
        !isDemoData &&
        StationReadingFreshnessPolicy.isFresh(
            observedAtEpochMillis = lastReadingEpochMillis,
            nowEpochMillis = nowEpochMillis
        )

/**
 * Converts a legacy station record without allowing unverified or stale values
 * to leak into the qualified live-observation model.
 *
 * Demo fixtures remain explicitly simulated. A verified but old/offline reading
 * is marked stale and its numeric values are withheld.
 */
fun SyrianWeatherStation.toQualifiedObservation(
    nowEpochMillis: Long = System.currentTimeMillis()
): StationObservation {
    val isSimulation = isDemoData
    val canShowLive = canShowLiveMeasurements(nowEpochMillis)
    val status = when {
        isSimulation -> WeatherDataStatus.SIMULATED
        canShowLive -> WeatherDataStatus.AVAILABLE
        hasVerifiedLiveData -> WeatherDataStatus.STALE
        else -> WeatherDataStatus.UNAVAILABLE
    }
    val source = WeatherDataProvenance(
        provider = when {
            isSimulation -> "MCNC local demonstration fixtures"
            hasVerifiedLiveData -> "Verified station feed"
            else -> "Unverified station source"
        },
        kind = when {
            isSimulation -> WeatherDataKind.SIMULATION
            hasVerifiedLiveData -> WeatherDataKind.OBSERVATION
            else -> WeatherDataKind.UNKNOWN
        },
        status = status,
        observedAtEpochMillis = lastReadingEpochMillis.takeIf { canShowLive },
        retrievedAtEpochMillis = nowEpochMillis.takeIf { canShowLive },
        attribution = if (isSimulation) {
            "قيم ثابتة للتوضيح فقط؛ ليست قياسات رصد فعلية."
        } else {
            null
        },
        sourceRecordId = id.takeIf { canShowLive }
    )

    fun measurement(
        value: Double,
        unit: String,
        isValid: (Double) -> Boolean = { it.isFinite() }
    ): QualifiedWeatherValue? {
        if (!isSimulation && !canShowLive) return null
        if (!value.isFinite() || !isValid(value)) return null
        return QualifiedWeatherValue(value, unit, source)
    }

    return StationObservation(
        stationId = id,
        stationNameAr = nameAr,
        governorateAr = governorate,
        latitude = latitude,
        longitude = longitude,
        temperatureCelsius = measurement(temperatureCelsius, "°C"),
        relativeHumidityPercent = measurement(humidity.toDouble(), "%") { it in 0.0..100.0 },
        pressureHpa = measurement(pressureHpa, "hPa") { it > 0.0 },
        windSpeedKmh = measurement(windSpeedKmh, "km/h") { it >= 0.0 },
        windDirectionDegrees = null,
        conditionCode = null,
        provenance = source
    )
}
