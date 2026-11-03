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
    val time: String = "",
    @Json(name = "temperature_2m") val temperature: Double = 0.0,
    @Json(name = "relative_humidity_2m") val relativeHumidity: Int = 0,
    @Json(name = "apparent_temperature") val apparentTemperature: Double = 0.0,
    @Json(name = "is_day") val isDay: Int = 1,
    val precipitation: Double = 0.0,
    @Json(name = "weather_code") val weatherCode: Int = 0,
    @Json(name = "surface_pressure") val surfacePressure: Double = 1013.25,
    @Json(name = "wind_speed_10m") val windSpeed: Double = 0.0,
    @Json(name = "wind_direction_10m") val windDirection: Double = 0.0,
    @Json(name = "uv_index") val uvIndex: Double = 0.0
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
    @Json(name = "pm2_5") val pm25: Double? = null,
    @Json(name = "carbon_monoxide") val carbonMonoxide: Double? = null,
    @Json(name = "nitrogen_dioxide") val nitrogenDioxide: Double? = null,
    @Json(name = "ozone") val ozone: Double? = null,
    @Json(name = "european_aqi") val europeanAqi: Int? = null,
    @Json(name = "alder_pollen") val alderPollen: Double? = null,
    @Json(name = "birch_pollen") val birchPollen: Double? = null,
    @Json(name = "grass_pollen") val grassPollen: Double? = null,
    @Json(name = "mugwort_pollen") val mugwortPollen: Double? = null,
    @Json(name = "olive_pollen") val olivePollen: Double? = null,
    @Json(name = "ragweed_pollen") val ragweedPollen: Double? = null
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
    val visibilityKm: Double = 10.0
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
    val isDay: Boolean = true
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
    val sunset: String
)

data class AirQualityInfo(
    val aqiValue: Int,
    val categoryAr: String,
    val adviceAr: String,
    val pm25: Double,
    val pm10: Double,
    val ozone: Double = 35.0,
    val no2: Double = 18.0
)

data class PollenInfo(
    val grassPollen: Double,
    val birchPollen: Double,
    val olivePollen: Double,
    val ragweedPollen: Double,
    val overallLevelAr: String,
    val adviceAr: String
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
    val isActive: Boolean = true
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
    val lastUpdated: Long = System.currentTimeMillis()
)
