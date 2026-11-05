package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// DTOs for Open-Meteo Weather API
@JsonClass(generateAdapter = true)
data class WeatherResponseDto(
    val latitude: Double,
    val longitude: Double,
    @Json(name = "utc_offset_seconds") val utcOffsetSeconds: Int = 0,
    val timezone: String? = null,
    val current: CurrentWeatherDto? = null,
    val hourly: HourlyWeatherDto? = null,
    val daily: DailyWeatherDto? = null
)

@JsonClass(generateAdapter = true)
data class CurrentWeatherDto(
    val time: String? = null,
    @Json(name = "temperature_2m") val temperature: Double? = null,
    @Json(name = "relative_humidity_2m") val relativeHumidity: Int? = null,
    @Json(name = "apparent_temperature") val apparentTemperature: Double? = null,
    @Json(name = "is_day") val isDay: Int? = null,
    val precipitation: Double? = null,
    @Json(name = "weather_code") val weatherCode: Int? = null,
    @Json(name = "surface_pressure") val surfacePressure: Double? = null,
    @Json(name = "wind_speed_10m") val windSpeed: Double? = null,
    @Json(name = "wind_direction_10m") val windDirection: Double? = null,
    @Json(name = "cloud_cover") val cloudCover: Int? = null,
    @Json(name = "uv_index") val uvIndex: Double? = null
)

@JsonClass(generateAdapter = true)
data class HourlyWeatherDto(
    val time: List<String> = emptyList(),
    @Json(name = "temperature_2m") val temperature: List<Double> = emptyList(),
    @Json(name = "relative_humidity_2m") val relativeHumidity: List<Int> = emptyList(),
    @Json(name = "precipitation_probability") val precipitationProbability: List<Int>? = emptyList(),
    @Json(name = "weather_code") val weatherCode: List<Int> = emptyList(),
    @Json(name = "wind_speed_10m") val windSpeed: List<Double> = emptyList(),
    @Json(name = "visibility") val visibility: List<Double>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class DailyWeatherDto(
    val time: List<String> = emptyList(),
    @Json(name = "weather_code") val weatherCode: List<Int> = emptyList(),
    @Json(name = "temperature_2m_max") val temperatureMax: List<Double> = emptyList(),
    @Json(name = "temperature_2m_min") val temperatureMin: List<Double> = emptyList(),
    val sunrise: List<String> = emptyList(),
    val sunset: List<String> = emptyList(),
    @Json(name = "uv_index_max") val uvIndexMax: List<Double>? = emptyList(),
    @Json(name = "precipitation_probability_max") val precipitationProbabilityMax: List<Int>? = emptyList()
)

/**
 * Country-aware city matching for saved favorites.
 *
 * Older saved entries may not contain a country; in that legacy case, retain
 * name-only matching rather than making existing favorites disappear.
 */
object CityFavoriteMatching {
    fun matches(
        savedName: String,
        savedCountry: String,
        queryName: String,
        queryCountry: String
    ): Boolean {
        if (!savedName.trim().equals(queryName.trim(), ignoreCase = true)) return false
        val storedCountry = savedCountry.trim()
        val requestedCountry = queryCountry.trim()
        return storedCountry.isEmpty() || requestedCountry.isEmpty() ||
            storedCountry.equals(requestedCountry, ignoreCase = true)
    }
}

/** Range checks for forecast measurements before they enter UI/domain models. */
object ForecastValueValidation {
    fun finite(value: Double?): Double? = value?.takeIf { it.isFinite() }
    fun nonNegativeFinite(value: Double?): Double? = value?.takeIf { it.isFinite() && it >= 0.0 }
    fun probabilityPercent(value: Int?): Int? = value?.takeIf { it in 0..100 }
    fun humidityPercent(value: Int?): Int? = value?.takeIf { it in 0..100 }
    fun nonNegativeInt(value: Int?): Int? = value?.takeIf { it >= 0 }

    /** Returns an ISO calendar date only when its month/day combination is valid. */
    fun validIsoDate(value: String?): String? {
        val date = value?.takeIf { Regex("""^\d{4}-\d{2}-\d{2}$""").matches(it) } ?: return null
        val year = date.substring(0, 4).toInt()
        val month = date.substring(5, 7).toInt()
        val day = date.substring(8, 10).toInt()
        val maxDay = when (month) {
            2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
            4, 6, 9, 11 -> 30
            in 1..12 -> 31
            else -> return null
        }
        return date.takeIf { day in 1..maxDay }
    }

    /** Returns a validated 0..23 hour for an ISO local timestamp. */
    fun hourOfDay(value: String?): Int? {
        val raw = value?.takeIf { it.length >= 13 } ?: return null
        val prefix = raw.take(13)
        val date = validIsoDate(prefix.take(10)) ?: return null
        if (!prefix.startsWith(date + "T")) return null
        val hourText = prefix.substring(11, 13)
        if (!Regex("""^([01]\d|2[0-3])$""").matches(hourText)) return null
        val suffix = raw.substring(13)
        if (!Regex("""^(:[0-5]\d(?::[0-5]\d(?:\.\d+)?)?)?$""").matches(suffix)) return null
        return hourText.toInt()
    }

    /**
     * Finds the hourly forecast slot corresponding to the provider's current local time.
     * Falls back to the device's local hour prefix, then to the first available slot.
     */
    fun currentHourIndex(
        hourlyTimes: List<String>,
        currentTime: String?,
        fallbackHourPrefix: String
    ): Int {
        if (hourlyTimes.isEmpty()) return 0
        fun validHourPrefix(value: String?): String? {
            val prefix = value?.takeIf { it.length >= 13 }?.take(13) ?: return null
            val validDate = validIsoDate(prefix.take(10)) ?: return null
            val validHour = Regex("""^([01]\d|2[0-3])$""").matches(prefix.substring(11, 13))
            val suffix = value.substring(13)
            val validRemainder = Regex("""^(:[0-5]\d(?::[0-5]\d(?:\.\d+)?)?)?$""").matches(suffix)
            return prefix.takeIf { it.startsWith(validDate + "T") && validHour && validRemainder }
        }

        fun findMatchingHour(prefix: String?): Int? =
            prefix?.let { expected ->
                hourlyTimes.indexOfFirst { validHourPrefix(it) == expected }
            }?.takeIf { it >= 0 }

        // A valid provider timestamp may be absent from a partial or malformed
        // hourly response, so try the device-hour fallback before using slot zero.
        return findMatchingHour(validHourPrefix(currentTime))
            ?: findMatchingHour(validHourPrefix(fallbackHourPrefix))
            ?: 0
    }

    /** Formats an Open-Meteo local sunrise/sunset timestamp; malformed values stay unavailable. */
    fun formatSunTime(raw: String?): String? {
        val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val match = Regex("""^(\d{4})-(0[1-9]|1[0-2])-(0[1-9]|[12]\d|3[01])T([01]\d|2[0-3]):([0-5]\d)(?::[0-5]\d)?$""")
            .matchEntire(value) ?: return null
        if (validIsoDate(value.take(10)) == null) return null
        val hour = match.groupValues[4].toInt()
        val minute = match.groupValues[5]
        return if (hour < 12) {
            "${if (hour == 0) 12 else hour}:$minute ص"
        } else {
            "${if (hour == 12) 12 else hour - 12}:$minute م"
        }
    }

    fun temperatureRange(max: Double?, min: Double?): Pair<Double, Double>? {
        val validMax = finite(max) ?: return null
        val validMin = finite(min) ?: return null
        return if (validMax >= validMin) validMax to validMin else null
    }

}

// DTOs for Open-Meteo Geocoding API
@JsonClass(generateAdapter = true)
data class GeocodingResponseDto(
    val results: List<GeocodingResultDto>? = null
)

@JsonClass(generateAdapter = true)
data class GeocodingResultDto(
    val id: Long = 0,
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val country: String? = null,
    @Json(name = "country_code") val countryCode: String? = null,
    val admin1: String? = null,
    val timezone: String? = null
)

// DTO for Air Quality & Pollen API
@JsonClass(generateAdapter = true)
data class AirQualityResponseDto(
    val current: AirQualityCurrentDto? = null
)

@JsonClass(generateAdapter = true)
data class AirQualityCurrentDto(
    val pm10: Double? = null,
    @field:Json(name = "pm2_5") val pm25: Double? = null,
    @field:Json(name = "carbon_monoxide") val carbonMonoxide: Double? = null,
    @field:Json(name = "nitrogen_dioxide") val nitrogenDioxide: Double? = null,
    @field:Json(name = "ozone") val ozone: Double? = null,
    @field:Json(name = "european_aqi") val europeanAqi: Int? = null,
    @field:Json(name = "alder_pollen") val alderPollen: Double? = null,
    @field:Json(name = "birch_pollen") val birchPollen: Double? = null,
    @field:Json(name = "grass_pollen") val grassPollen: Double? = null,
    @field:Json(name = "mugwort_pollen") val mugwortPollen: Double? = null,
    @field:Json(name = "olive_pollen") val olivePollen: Double? = null,
    @field:Json(name = "ragweed_pollen") val ragweedPollen: Double? = null
)

// Domain Models used in the UI
enum class WeatherConditionType {
    CLEAR,
    PARTLY_CLOUDY,
    CLOUDY,
    FOG,
    DRIZZLE,
    RAIN,
    HEAVY_RAIN,
    SNOW,
    THUNDERSTORM
}

data class CityLocation(
    val name: String,
    val nameEn: String = "",
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val admin1: String? = null,
    val isFavorite: Boolean = false
)

data class CurrentWeather(
    val temperatureCelsius: Double,
    val feelsLikeCelsius: Double,
    val humidity: Int,
    val windSpeedKmh: Double,
    val windDirectionDegrees: Double,
    val pressureHpa: Double,
    val uvIndex: Double,
    val weatherCode: Int,
    val conditionType: WeatherConditionType,
    val conditionNameAr: String,
    val conditionNameEn: String,
    val isDay: Boolean,
    val precipitationMm: Double,
    val visibilityKm: Double = 0.0,
    val visibilityAvailable: Boolean = false,
    val uvIndexAvailable: Boolean = false
)

data class HourlyForecast(
    val timeRaw: String,
    val hourLabel: String,
    val temperatureCelsius: Double,
    val conditionType: WeatherConditionType,
    val weatherCode: Int,
    val precipitationProb: Int,
    val humidity: Int,
    val windSpeedKmh: Double,
    val isDay: Boolean = true,
    val temperatureAvailable: Boolean = true,
    val precipitationProbabilityAvailable: Boolean = true,
    val humidityAvailable: Boolean = true,
    val windSpeedAvailable: Boolean = true
)

data class DailyForecast(
    val dateRaw: String,
    val dayNameAr: String,
    val dayNameEn: String,
    val formattedDate: String,
    val conditionType: WeatherConditionType,
    val weatherCode: Int,
    val conditionNameAr: String,
    val maxTempCelsius: Double,
    val minTempCelsius: Double,
    val precipitationProb: Int,
    val uvIndexMax: Double,
    val sunrise: String,
    val sunset: String,
    val precipitationProbabilityAvailable: Boolean = true,
    val uvIndexAvailable: Boolean = true,
    val sunriseAvailable: Boolean = true,
    val sunsetAvailable: Boolean = true
)

data class AirQualityInfo(
    val aqiValue: Int,
    val categoryAr: String,
    val adviceAr: String,
    val pm25: Double,
    val pm10: Double,
    val ozone: Double = 0.0,
    val no2: Double = 0.0,
    val isAvailable: Boolean = false,
    val isAqiAvailable: Boolean = false,
    val pm25Available: Boolean = false,
    val pm10Available: Boolean = false,
    val ozoneAvailable: Boolean = false,
    val no2Available: Boolean = false
)

data class PollenInfo(
    val grassPollen: Double,
    val birchPollen: Double,
    val olivePollen: Double,
    val ragweedPollen: Double,
    val overallLevelAr: String,
    val adviceAr: String,
    val isAvailable: Boolean = false,
    val grassAvailable: Boolean = false,
    val birchAvailable: Boolean = false,
    val oliveAvailable: Boolean = false,
    val ragweedAvailable: Boolean = false
)

enum class AlertSeverity {
    ADVISORY, // إرشاد / أصفر
    WATCH,    // متابعة / برتقالي خفيف
    WARNING,  // تحذير / برتقالي فاقع
    EMERGENCY // طوارئ قصوى / أحمر
}

data class SevereWeatherAlert(
    val id: String,
    val titleAr: String,
    val severity: AlertSeverity,
    val descriptionAr: String,
    val safetyInstructionsAr: String,
    val source: String,
    val validUntil: String,
    val isActive: Boolean = true,
    val isOfficial: Boolean = false,
    val isLocalDemo: Boolean = false
)

enum class WeatherMetricType(val id: String, val titleAr: String, val subtitleAr: String) {
    UV_INDEX("uv_index", "مؤشر الأشعة (UV)", "مستوى الحماية وشدة الشمس"),
    AIR_QUALITY("air_quality", "جودة الهواء (AQI)", "مستوى النقاء وتلوث الجو"),
    POLLEN_COUNT("pollen_count", "حبوب اللقاح والحساسية", "تنبيهات موسم الحساسية"),
    WIND("wind", "سرعة واتجاه الرياح", "بوصلة الرياح والهبات"),
    HUMIDITY("humidity", "الرطوبة النسبية", "نسبة الرطوبة ونقطة الندى"),
    SUN_CYCLE("sun_cycle", "الشروق والغروب", "مسار الشمس النهاري"),
    PRESSURE("pressure", "الضغط الجوي", "الاستقرار والتقلبات الجوية"),
    VISIBILITY("visibility", "مدى الرؤية الأفقية", "صفاء الرؤية على الطرقات")
}

data class MetricPreference(
    val type: WeatherMetricType,
    val isEnabled: Boolean = true
)

/**
 * Validates the geographic coordinates returned for an Open-Meteo model grid cell.
 * These coordinates identify the grid-cell centre and are not expected to equal
 * the requested city coordinates exactly.
 */
object MapForecastCoordinateValidation {
    fun isValidGridCell(latitude: Double, longitude: Double): Boolean =
        latitude.isFinite() && latitude in -90.0..90.0 &&
            longitude.isFinite() && longitude in -180.0..180.0
}


/**
 * Associates multi-location forecast responses with their requested cities by
 * returned model-grid coordinates, rather than relying on response array order.
 *
 * Open-Meteo reports grid-cell centres which can differ from requested points.
 * These conservative distance and separation checks prevent silently attaching
 * a neighbouring city's forecast when a response is malformed or ambiguous.
 */
object MapForecastResponseMatching {
    private const val MAX_GRID_CELL_DISTANCE_KM = 30.0
    private const val MIN_NEAREST_CITY_MARGIN_KM = 5.0

    fun match(
        cities: List<CityLocation>,
        responses: List<WeatherResponseDto>
    ): List<Pair<CityLocation, WeatherResponseDto>>? {
        if (cities.isEmpty() || cities.size != responses.size) return null
        if (cities.any {
                !MapForecastCoordinateValidation.isValidGridCell(it.latitude, it.longitude)
            }
        ) return null

        val matched = arrayOfNulls<WeatherResponseDto>(cities.size)
        for (response in responses) {
            if (!MapForecastCoordinateValidation.isValidGridCell(
                    response.latitude, response.longitude
                )
            ) return null

            val rankedCities = cities.indices
                .map { index ->
                    index to distanceKm(
                        response.latitude,
                        response.longitude,
                        cities[index].latitude,
                        cities[index].longitude
                    )
                }
                .sortedBy { it.second }

            val nearest = rankedCities.first()
            val secondNearest = rankedCities.getOrNull(1)
            if (nearest.second > MAX_GRID_CELL_DISTANCE_KM) return null
            if (secondNearest != null &&
                secondNearest.second - nearest.second < MIN_NEAREST_CITY_MARGIN_KM
            ) return null
            if (matched[nearest.first] != null) return null
            matched[nearest.first] = response
        }

        if (matched.any { it == null }) return null
        return cities.indices.map { index -> cities[index] to matched[index]!! }
    }

    private fun distanceKm(
        latitude1: Double,
        longitude1: Double,
        latitude2: Double,
        longitude2: Double
    ): Double {
        val earthRadiusKm = 6371.0
        val lat1 = Math.toRadians(latitude1)
        val lat2 = Math.toRadians(latitude2)
        val deltaLatitude = Math.toRadians(latitude2 - latitude1)
        val deltaLongitude = Math.toRadians(longitude2 - longitude1)
        val a = kotlin.math.sin(deltaLatitude / 2).let { it * it } +
            kotlin.math.cos(lat1) * kotlin.math.cos(lat2) *
            kotlin.math.sin(deltaLongitude / 2).let { it * it }
        return 2.0 * earthRadiusKm * kotlin.math.asin(kotlin.math.sqrt(a.coerceIn(0.0, 1.0)))
    }
}

/** Forecast values for a map location; these are model outputs, not station observations. */
data class CityMapForecast(
    val city: CityLocation,
    val time: String?,
    val temperatureCelsius: Double?,
    val humidityPercent: Int?,
    val windSpeedKmh: Double?,
    val windDirectionDegrees: Double?,
    val cloudCoverPercent: Int?,
    val precipitationMm: Double?,
    val weatherCode: Int?
)

enum class MapWeatherOverlay(val id: String, val titleAr: String) {
    PRECIPITATION("precip", "الأمطار والرادار"),
    TEMPERATURE("temp", "درجات الحرارة"),
    WIND("wind", "حركة وتيارات الرياح"),
    CLOUDS("clouds", "الغطاء السحابي")
}

data class MapLocationPin(
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val tempCelsius: Double,
    val conditionType: WeatherConditionType,
    val rainMm: Double,
    val windKmh: Double
)

data class WeatherInsights(
    val adviceTitle: String,
    val adviceSummary: String,
    val umbrellaNeeded: Boolean,
    val outdoorActivityScore: String,
    val clothingAdvice: String
)

data class FullWeatherData(
    val location: CityLocation,
    val current: CurrentWeather,
    val hourly: List<HourlyForecast>,
    val daily: List<DailyForecast>,
    val airQuality: AirQualityInfo,
    val pollen: PollenInfo,
    val alerts: List<SevereWeatherAlert>,
    val insights: WeatherInsights,
    val lastUpdated: Long = System.currentTimeMillis(),
    val currentDateRaw: String? = null
)

object HourlyForecastSelection {
    /** Selects up to [limit] valid hourly slots from [startIndex], skipping malformed times and missing weather codes. */
    fun validIndices(
        times: List<String>,
        weatherCodes: List<Int>,
        startIndex: Int,
        limit: Int = 24
    ): List<Int> {
        if (limit <= 0) return emptyList()
        val firstIndex = startIndex.coerceIn(0, times.size)
        return (firstIndex until times.size).filter { index ->
            ForecastValueValidation.hourOfDay(times[index]) != null &&
                weatherCodes.getOrNull(index) != null
        }.take(limit)
    }
}

object DailyForecastSelection {
    /** Selects up to [limit] usable days, skipping malformed or incomplete raw records. */
    fun validIndices(
        dates: List<String>,
        weatherCodes: List<Int>,
        maxTemperatures: List<Double?>,
        minTemperatures: List<Double?>,
        limit: Int = 7
    ): List<Int> {
        if (limit <= 0) return emptyList()
        return dates.indices.filter { index ->
            ForecastValueValidation.validIsoDate(dates[index]) != null &&
                weatherCodes.getOrNull(index) != null &&
                ForecastValueValidation.temperatureRange(
                    maxTemperatures.getOrNull(index), minTemperatures.getOrNull(index)
                ) != null
        }.take(limit)
    }

    fun forCurrentDate(daily: List<DailyForecast>, currentDateRaw: String?): DailyForecast? =
        currentDateRaw?.let { date -> daily.firstOrNull { it.dateRaw == date } }
}
