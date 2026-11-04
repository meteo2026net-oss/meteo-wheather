package com.example.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ForecastValueValidationTest {
    @Test
    fun finiteRejectsMissingAndNonFiniteValuesButKeepsNegativeTemperatures() {
        assertNull(ForecastValueValidation.finite(null))
        assertNull(ForecastValueValidation.finite(Double.NaN))
        assertNull(ForecastValueValidation.finite(Double.POSITIVE_INFINITY))
        assertEquals(-12.5, ForecastValueValidation.finite(-12.5)!!, 0.0)
        assertEquals(0.0, ForecastValueValidation.finite(0.0)!!, 0.0)
    }

    @Test
    fun nonNegativeFiniteRejectsNegativeAndNonFiniteMeasurements() {
        assertNull(ForecastValueValidation.nonNegativeFinite(-0.1))
        assertNull(ForecastValueValidation.nonNegativeFinite(Double.NaN))
        assertNull(ForecastValueValidation.nonNegativeFinite(null))
        assertEquals(0.0, ForecastValueValidation.nonNegativeFinite(0.0)!!, 0.0)
        assertEquals(18.2, ForecastValueValidation.nonNegativeFinite(18.2)!!, 0.0)
    }

    @Test
    fun percentageValidatorsEnforceInclusiveZeroToOneHundredRange() {
        assertEquals(0, ForecastValueValidation.probabilityPercent(0))
        assertEquals(100, ForecastValueValidation.probabilityPercent(100))
        assertNull(ForecastValueValidation.probabilityPercent(-1))
        assertNull(ForecastValueValidation.probabilityPercent(101))
        assertEquals(0, ForecastValueValidation.humidityPercent(0))
        assertEquals(100, ForecastValueValidation.humidityPercent(100))
        assertNull(ForecastValueValidation.humidityPercent(-1))
        assertNull(ForecastValueValidation.humidityPercent(101))
    }

    @Test
    fun validIsoDateRejectsImpossibleDatesAndAcceptsLeapDays() {
        assertEquals("2024-02-29", ForecastValueValidation.validIsoDate("2024-02-29"))
        assertNull(ForecastValueValidation.validIsoDate("2026-02-29"))
        assertNull(ForecastValueValidation.validIsoDate("2026-02-30"))
        assertNull(ForecastValueValidation.validIsoDate("2026-13-01"))
        assertNull(ForecastValueValidation.validIsoDate("2026-04-31"))
        assertNull(ForecastValueValidation.validIsoDate("2026-4-01"))
        assertNull(ForecastValueValidation.validIsoDate(null))
    }

    @Test
    fun currentHourIndexSelectsTheProviderCurrentLocalHour() {
        val times = listOf(
            "2026-11-05T00:00",
            "2026-11-05T01:00",
            "2026-11-05T02:00",
            "2026-11-05T03:00"
        )
        assertEquals(2, ForecastValueValidation.currentHourIndex(
            times, "2026-11-05T02:17", "2026-11-05T01"
        ))
    }

    @Test
    fun currentHourIndexUsesFallbackHourWhenProviderTimeIsMissingOrInvalid() {
        val times = listOf("2026-11-05T00:00", "2026-11-05T01:00", "2026-11-05T02:00")
        assertEquals(1, ForecastValueValidation.currentHourIndex(
            times, null, "2026-11-05T01"
        ))
        assertEquals(2, ForecastValueValidation.currentHourIndex(
            times, "invalid", "2026-11-05T02"
        ))
    }

    @Test
    fun currentHourIndexUsesFallbackWhenProviderTimestampIsLongButMalformed() {
        val times = listOf("2026-11-05T01:00", "2026-11-05T02:00")
        assertEquals(1, ForecastValueValidation.currentHourIndex(
            times, "not-a-valid-provider-timestamp", "2026-11-05T02"
        ))
        assertEquals(0, ForecastValueValidation.currentHourIndex(
            times, "2026-02-30T02:00", "invalid-fallback-timestamp"
        ))
    }

    @Test
    fun currentHourIndexIgnoresMalformedHourlySlotsAndMalformedMinuteSuffixes() {
        val times = listOf(
            "2026-11-05T02garbage",
            "2026-11-05T02:99",
            "2026-11-05T01:00"
        )
        assertEquals(2, ForecastValueValidation.currentHourIndex(
            times, "2026-11-05T02:30", "2026-11-05T01"
        ))
        assertEquals(2, ForecastValueValidation.currentHourIndex(
            times, "2026-11-05T02:99", "2026-11-05T01"
        ))
    }

    @Test
    fun currentHourIndexFallsBackToFirstSlotWhenNoHourMatches() {
        val times = listOf("2026-11-05T00:00", "2026-11-05T01:00")
        assertEquals(0, ForecastValueValidation.currentHourIndex(
            times, "2026-11-06T12:00", "2026-11-06T12"
        ))
    }

    @Test
    fun currentHourIndexHandlesAnEmptyForecast() {
        assertEquals(0, ForecastValueValidation.currentHourIndex(
            emptyList(), "2026-11-05T12:00", "2026-11-05T12"
        ))
    }

    @Test
    fun temperatureRangeRequiresTwoFiniteValuesAndMaximumNotBelowMinimum() {
        assertEquals(12.0 to -3.0, ForecastValueValidation.temperatureRange(12.0, -3.0))
        assertEquals(0.0 to 0.0, ForecastValueValidation.temperatureRange(0.0, 0.0))
        assertNull(ForecastValueValidation.temperatureRange(null, 0.0))
        assertNull(ForecastValueValidation.temperatureRange(Double.NaN, 0.0))
        assertNull(ForecastValueValidation.temperatureRange(2.0, 3.0))
    }

    @Test
    fun nonNegativeIntegerRejectsNegativeValues() {
        assertEquals(0, ForecastValueValidation.nonNegativeInt(0))
        assertEquals(42, ForecastValueValidation.nonNegativeInt(42))
        assertNull(ForecastValueValidation.nonNegativeInt(-1))
        assertNull(ForecastValueValidation.nonNegativeInt(null))
    }
    @Test
    fun sunTimeFormatterRejectsMissingMalformedAndOutOfRangeValues() {
        assertNull(ForecastValueValidation.formatSunTime(null))
        assertNull(ForecastValueValidation.formatSunTime(""))
        assertNull(ForecastValueValidation.formatSunTime("not-a-time"))
        assertNull(ForecastValueValidation.formatSunTime("2026-11-04T24:00"))
        assertNull(ForecastValueValidation.formatSunTime("2026-11-04T12:60"))
        assertNull(ForecastValueValidation.formatSunTime("2026-13-04T12:00"))
        assertNull(ForecastValueValidation.formatSunTime("2026-02-30T12:00"))
        assertEquals("2:00 ص", ForecastValueValidation.formatSunTime("2024-02-29T02:00"))
    }

    @Test
    fun sunTimeFormatterUsesTwelveHourArabicLabels() {
        assertEquals("12:05 ص", ForecastValueValidation.formatSunTime("2026-11-04T00:05"))
        assertEquals("11:59 ص", ForecastValueValidation.formatSunTime("2026-11-04T11:59"))
        assertEquals("12:30 م", ForecastValueValidation.formatSunTime("2026-11-04T12:30"))
        assertEquals("1:09 م", ForecastValueValidation.formatSunTime("2026-11-04T13:09"))
        assertEquals("11:59 م", ForecastValueValidation.formatSunTime("2026-11-04T23:59:00"))
    }

}


class DailyForecastSelectionTest {
    @org.junit.Test
    fun selectsForecastMatchingProviderCurrentDateRatherThanFirstAvailableDay() {
        val tomorrow = DailyForecast(
            dateRaw = "2026-11-06",
            dayNameAr = "غداً",
            dayNameEn = "Tomorrow",
            formattedDate = "06/11",
            conditionType = WeatherConditionType.CLEAR,
            weatherCode = 0,
            conditionNameAr = "صافٍ",
            maxTempCelsius = 20.0,
            minTempCelsius = 10.0,
            precipitationProb = 0,
            uvIndexMax = 1.0,
            sunrise = "--:--",
            sunset = "--:--"
        )
        val dayAfter = tomorrow.copy(dateRaw = "2026-11-07")
        assertNull(DailyForecastSelection.forCurrentDate(listOf(tomorrow, dayAfter), "2026-11-05"))
        assertEquals(tomorrow, DailyForecastSelection.forCurrentDate(listOf(tomorrow, dayAfter), "2026-11-06"))
    }

    @org.junit.Test
    fun returnsNullWhenProviderCurrentDateIsUnavailable() {
        val forecast = DailyForecast(
            dateRaw = "2026-11-06",
            dayNameAr = "غداً",
            dayNameEn = "Tomorrow",
            formattedDate = "06/11",
            conditionType = WeatherConditionType.CLEAR,
            weatherCode = 0,
            conditionNameAr = "صافٍ",
            maxTempCelsius = 20.0,
            minTempCelsius = 10.0,
            precipitationProb = 0,
            uvIndexMax = 1.0,
            sunrise = "--:--",
            sunset = "--:--"
        )
        assertNull(DailyForecastSelection.forCurrentDate(listOf(forecast), null))
    }
}
