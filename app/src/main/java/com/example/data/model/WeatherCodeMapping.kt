package com.example.data.model

/**
 * Centralized Open-Meteo/WMO weather-code descriptions.
 * Unknown provider codes receive an explicit label instead of being described as "moderate".
 */
data class WeatherCodeDescription(
    val conditionType: WeatherConditionType,
    val arabic: String,
    val english: String
)

object WeatherCodeMapping {
    fun description(code: Int): WeatherCodeDescription = when (code) {
        0 -> WeatherCodeDescription(WeatherConditionType.CLEAR, "صافٍ ومشرق", "Clear sky")
        1 -> WeatherCodeDescription(WeatherConditionType.PARTLY_CLOUDY, "صافٍ غالباً", "Mainly clear")
        2 -> WeatherCodeDescription(WeatherConditionType.PARTLY_CLOUDY, "غائم جزئياً", "Partly cloudy")
        3 -> WeatherCodeDescription(WeatherConditionType.CLOUDY, "غائم كلياً", "Overcast")
        45 -> WeatherCodeDescription(WeatherConditionType.FOG, "ضباب", "Fog")
        48 -> WeatherCodeDescription(WeatherConditionType.FOG, "ضباب مع ترسب الصقيع", "Depositing rime fog")
        51 -> WeatherCodeDescription(WeatherConditionType.DRIZZLE, "رذاذ خفيف", "Light drizzle")
        53 -> WeatherCodeDescription(WeatherConditionType.DRIZZLE, "رذاذ معتدل", "Moderate drizzle")
        55 -> WeatherCodeDescription(WeatherConditionType.DRIZZLE, "رذاذ كثيف", "Dense drizzle")
        56 -> WeatherCodeDescription(WeatherConditionType.DRIZZLE, "رذاذ متجمد خفيف", "Light freezing drizzle")
        57 -> WeatherCodeDescription(WeatherConditionType.DRIZZLE, "رذاذ متجمد كثيف", "Dense freezing drizzle")
        61 -> WeatherCodeDescription(WeatherConditionType.RAIN, "أمطار خفيفة", "Slight rain")
        63 -> WeatherCodeDescription(WeatherConditionType.RAIN, "أمطار معتدلة", "Moderate rain")
        65 -> WeatherCodeDescription(WeatherConditionType.HEAVY_RAIN, "أمطار غزيرة", "Heavy rain")
        66 -> WeatherCodeDescription(WeatherConditionType.RAIN, "أمطار متجمدة خفيفة", "Light freezing rain")
        67 -> WeatherCodeDescription(WeatherConditionType.RAIN, "أمطار متجمدة غزيرة", "Heavy freezing rain")
        71 -> WeatherCodeDescription(WeatherConditionType.SNOW, "ثلوج خفيفة", "Slight snow fall")
        73 -> WeatherCodeDescription(WeatherConditionType.SNOW, "تساقط ثلوج معتدل", "Moderate snow fall")
        75 -> WeatherCodeDescription(WeatherConditionType.SNOW, "ثلوج كثيفة", "Heavy snow fall")
        77 -> WeatherCodeDescription(WeatherConditionType.SNOW, "حبيبات ثلجية", "Snow grains")
        80 -> WeatherCodeDescription(WeatherConditionType.RAIN, "زخات مطر خفيفة", "Slight rain showers")
        81 -> WeatherCodeDescription(WeatherConditionType.RAIN, "زخات مطر معتدلة", "Moderate rain showers")
        82 -> WeatherCodeDescription(WeatherConditionType.HEAVY_RAIN, "زخات مطر عنيفة", "Violent rain showers")
        85 -> WeatherCodeDescription(WeatherConditionType.SNOW, "زخات ثلجية خفيفة", "Slight snow showers")
        86 -> WeatherCodeDescription(WeatherConditionType.SNOW, "زخات ثلجية غزيرة", "Heavy snow showers")
        95 -> WeatherCodeDescription(WeatherConditionType.THUNDERSTORM, "عاصفة رعدية", "Thunderstorm")
        96 -> WeatherCodeDescription(WeatherConditionType.THUNDERSTORM, "عاصفة رعدية مع بَرَد", "Thunderstorm with slight hail")
        99 -> WeatherCodeDescription(WeatherConditionType.THUNDERSTORM, "عاصفة رعدية مع بَرَد كثيف", "Thunderstorm with heavy hail")
        else -> WeatherCodeDescription(
            WeatherConditionType.UNKNOWN,
            "حالة جوية غير معروفة",
            "Unknown weather condition"
        )
    }
}
