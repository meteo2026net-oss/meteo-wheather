package com.example.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherCodeMappingTest {

    @Test
    fun mapsEverySupportedOpenMeteoWeatherCodeToTheExpectedConditionFamily() {
        val expected = mapOf(
            0 to WeatherConditionType.CLEAR,
            1 to WeatherConditionType.PARTLY_CLOUDY,
            2 to WeatherConditionType.PARTLY_CLOUDY,
            3 to WeatherConditionType.CLOUDY,
            45 to WeatherConditionType.FOG,
            48 to WeatherConditionType.FOG,
            51 to WeatherConditionType.DRIZZLE,
            53 to WeatherConditionType.DRIZZLE,
            55 to WeatherConditionType.DRIZZLE,
            56 to WeatherConditionType.DRIZZLE,
            57 to WeatherConditionType.DRIZZLE,
            61 to WeatherConditionType.RAIN,
            63 to WeatherConditionType.RAIN,
            65 to WeatherConditionType.HEAVY_RAIN,
            66 to WeatherConditionType.RAIN,
            67 to WeatherConditionType.RAIN,
            71 to WeatherConditionType.SNOW,
            73 to WeatherConditionType.SNOW,
            75 to WeatherConditionType.SNOW,
            77 to WeatherConditionType.SNOW,
            80 to WeatherConditionType.RAIN,
            81 to WeatherConditionType.RAIN,
            82 to WeatherConditionType.HEAVY_RAIN,
            85 to WeatherConditionType.SNOW,
            86 to WeatherConditionType.SNOW,
            95 to WeatherConditionType.THUNDERSTORM,
            96 to WeatherConditionType.THUNDERSTORM,
            99 to WeatherConditionType.THUNDERSTORM
        )

        expected.forEach { (code, condition) ->
            assertEquals("Unexpected condition family for code $code", condition,
                WeatherCodeMapping.description(code).conditionType)
        }
    }

    @Test
    fun freezingDrizzleAndFreezingRainHaveDistinctBilingualDescriptions() {
        assertEquals("رذاذ متجمد خفيف", WeatherCodeMapping.description(56).arabic)
        assertEquals("Light freezing drizzle", WeatherCodeMapping.description(56).english)
        assertEquals("أمطار متجمدة غزيرة", WeatherCodeMapping.description(67).arabic)
        assertEquals("Heavy freezing rain", WeatherCodeMapping.description(67).english)
    }

    @Test
    fun snowGrainsAndSnowShowersHaveEnglishDescriptions() {
        assertEquals("حبيبات ثلجية", WeatherCodeMapping.description(77).arabic)
        assertEquals("Snow grains", WeatherCodeMapping.description(77).english)
        assertEquals("Slight snow showers", WeatherCodeMapping.description(85).english)
        assertEquals("Heavy snow showers", WeatherCodeMapping.description(86).english)
    }

    @Test
    fun unsupportedCodesAreNotMislabelledAsModerateWeather() {
        val description = WeatherCodeMapping.description(999)
        assertEquals("حالة جوية غير معروفة", description.arabic)
        assertEquals("Unknown weather condition", description.english)
    }

    @Test
    fun allDescriptionsAreNonBlank() {
        val supportedCodes = listOf(
            0, 1, 2, 3, 45, 48, 51, 53, 55, 56, 57, 61, 63, 65, 66, 67,
            71, 73, 75, 77, 80, 81, 82, 85, 86, 95, 96, 99
        )
        supportedCodes.forEach { code ->
            val description = WeatherCodeMapping.description(code)
            assertEquals(true, description.arabic.isNotBlank())
            assertEquals(true, description.english.isNotBlank())
        }
    }
}
