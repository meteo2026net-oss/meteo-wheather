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
    fun uvIndexAcceptsPlausibleValuesAndRejectsCorruptValues() {
        assertEquals(0.0, ForecastValueValidation.uvIndex(0.0)!!, 0.0)
        assertEquals(11.5, ForecastValueValidation.uvIndex(11.5)!!, 0.0)
        assertEquals(30.0, ForecastValueValidation.uvIndex(30.0)!!, 0.0)
        assertNull(ForecastValueValidation.uvIndex(null))
        assertNull(ForecastValueValidation.uvIndex(-0.1))
        assertNull(ForecastValueValidation.uvIndex(30.1))
        assertNull(ForecastValueValidation.uvIndex(Double.NaN))
        assertNull(ForecastValueValidation.uvIndex(Double.POSITIVE_INFINITY))
    }

    @Test
    fun visibilityMetersRejectsMissingNegativeNonFiniteAndImplausibleValues() {
        assertEquals(0.0, ForecastValueValidation.visibilityMeters(0.0)!!, 0.0)
        assertEquals(24_100.0, ForecastValueValidation.visibilityMeters(24_100.0)!!, 0.0)
        assertEquals(100_000.0, ForecastValueValidation.visibilityMeters(100_000.0)!!, 0.0)
        assertNull(ForecastValueValidation.visibilityMeters(null))
        assertNull(ForecastValueValidation.visibilityMeters(-1.0))
        assertNull(ForecastValueValidation.visibilityMeters(100_000.1))
        assertNull(ForecastValueValidation.visibilityMeters(Double.NaN))
        assertNull(ForecastValueValidation.visibilityMeters(Double.POSITIVE_INFINITY))
    }

    @Test
    fun cloudCoverValidatorAcceptsOnlyPercentagesFromZeroToOneHundred() {
        assertEquals(0, ForecastValueValidation.cloudCoverPercent(0))
        assertEquals(50, ForecastValueValidation.cloudCoverPercent(50))
        assertEquals(100, ForecastValueValidation.cloudCoverPercent(100))
        assertNull(ForecastValueValidation.cloudCoverPercent(null))
        assertNull(ForecastValueValidation.cloudCoverPercent(-1))
        assertNull(ForecastValueValidation.cloudCoverPercent(101))
    }

    @Test
    fun hourlyPrecipitationAmountAcceptsZeroAndPositiveMillimetersButRejectsInvalidValues() {
        assertEquals(0.0, ForecastValueValidation.nonNegativeFinite(0.0)!!, 0.0)
        assertEquals(2.75, ForecastValueValidation.nonNegativeFinite(2.75)!!, 0.0)
        assertNull(ForecastValueValidation.nonNegativeFinite(-0.01))
        assertNull(ForecastValueValidation.nonNegativeFinite(Double.NaN))
        assertNull(ForecastValueValidation.nonNegativeFinite(Double.POSITIVE_INFINITY))
        assertNull(ForecastValueValidation.nonNegativeFinite(null))
    }

    @Test
    fun dailyPrecipitationSumAcceptsValidMillimetersAndRejectsInvalidValues() {
        assertEquals(0.0, ForecastValueValidation.nonNegativeFinite(0.0)!!, 0.0)
        assertEquals(24.6, ForecastValueValidation.nonNegativeFinite(24.6)!!, 0.0)
        assertNull(ForecastValueValidation.nonNegativeFinite(-0.1))
        assertNull(ForecastValueValidation.nonNegativeFinite(Double.NaN))
        assertNull(ForecastValueValidation.nonNegativeFinite(Double.NEGATIVE_INFINITY))
        assertNull(ForecastValueValidation.nonNegativeFinite(null))
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
    fun weatherCodeValidatorAcceptsWmoRangeAndRejectsMissingOrOutOfRangeCodes() {
        assertEquals(0, ForecastValueValidation.weatherCode(0))
        assertEquals(99, ForecastValueValidation.weatherCode(99))
        assertNull(ForecastValueValidation.weatherCode(null))
        assertNull(ForecastValueValidation.weatherCode(-1))
        assertNull(ForecastValueValidation.weatherCode(100))
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
    fun physicalMeasurementValidatorsRejectImplausibleProviderValues() {
        assertEquals(-100.0, ForecastValueValidation.temperatureCelsius(-100.0)!!, 0.0)
        assertEquals(70.0, ForecastValueValidation.temperatureCelsius(70.0)!!, 0.0)
        assertNull(ForecastValueValidation.temperatureCelsius(-100.1))
        assertNull(ForecastValueValidation.temperatureCelsius(70.1))
        assertNull(ForecastValueValidation.temperatureCelsius(Double.NaN))

        assertEquals(800.0, ForecastValueValidation.pressureHpa(800.0)!!, 0.0)
        assertEquals(1100.0, ForecastValueValidation.pressureHpa(1100.0)!!, 0.0)
        assertNull(ForecastValueValidation.pressureHpa(799.9))
        assertNull(ForecastValueValidation.pressureHpa(1100.1))

        assertEquals(0.0, ForecastValueValidation.windSpeedKmh(0.0)!!, 0.0)
        assertEquals(300.0, ForecastValueValidation.windSpeedKmh(300.0)!!, 0.0)
        assertNull(ForecastValueValidation.windSpeedKmh(-0.1))
        assertNull(ForecastValueValidation.windSpeedKmh(300.1))

        assertEquals(360.0, ForecastValueValidation.windDirectionDegrees(360.0)!!, 0.0)
        assertNull(ForecastValueValidation.windDirectionDegrees(-0.1))
        assertNull(ForecastValueValidation.windDirectionDegrees(360.1))
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
    fun hourOfDayRejectsMalformedDatesHoursAndSuffixes() {
        assertEquals(0, ForecastValueValidation.hourOfDay("2024-02-29T00:00"))
        assertEquals(23, ForecastValueValidation.hourOfDay("2026-11-05T23:59:59") )
        assertNull(ForecastValueValidation.hourOfDay("2026-02-30T12:00"))
        assertNull(ForecastValueValidation.hourOfDay("2026-11-05T24:00"))
        assertNull(ForecastValueValidation.hourOfDay("2026-11-05T12:99"))
        assertNull(ForecastValueValidation.hourOfDay("2026-11-05T12garbage"))
    }

    @Test
    fun validIsoLocalTimestampRejectsMalformedDateTimeAndKeepsValidProviderTime() {
        assertEquals("2026-11-05T03:15", ForecastValueValidation.validIsoLocalTimestamp("2026-11-05T03:15"))
        assertEquals("2024-02-29T23:59:59", ForecastValueValidation.validIsoLocalTimestamp("2024-02-29T23:59:59"))
        assertNull(ForecastValueValidation.validIsoLocalTimestamp(null))
        assertNull(ForecastValueValidation.validIsoLocalTimestamp("2026-02-30T03:15"))
        assertNull(ForecastValueValidation.validIsoLocalTimestamp("2026-11-05T24:00"))
        assertNull(ForecastValueValidation.validIsoLocalTimestamp("2026-11-05T03:99"))
        assertNull(ForecastValueValidation.validIsoLocalTimestamp("2026-11-05T03:15garbage"))
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
    fun validIndicesCanReturnTheFullSixteenDayForecastWindow() {
        val dates = (1..16).map { "2026-11-%02d".format(it) }
        val codes = List(16) { 0 }
        val maxima = List(16) { 20.0 }
        val minima = List(16) { 10.0 }

        assertEquals(
            (0..15).toList(),
            DailyForecastSelection.validIndices(
                dates = dates,
                weatherCodes = codes,
                maxTemperatures = maxima,
                minTemperatures = minima,
                limit = 16
            )
        )
    }

    @org.junit.Test
    fun validIndicesCountsUsableDaysInsteadOfStoppingAtSevenRawRecords() {
        val dates = listOf(
            "2026-02-30", "2026-11-05", "2026-11-06", "2026-11-07",
            "2026-11-08", "2026-11-09", "2026-11-10", "2026-11-11", "2026-11-12"
        )
        val codes = listOf(0, 0, 1, 2, 3, 45, 61, 0, 2)
        val maxima = listOf(20.0, 21.0, 22.0, 23.0, 24.0, 25.0, 26.0, 27.0, 28.0)
        val minima = listOf(10.0, 11.0, 12.0, 13.0, 14.0, 15.0, 16.0, 17.0, 18.0)

        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7), DailyForecastSelection.validIndices(
            dates, codes, maxima, minima
        ))
    }

    @org.junit.Test
    fun validIndicesSkipsMissingCodesAndInvalidTemperatureRanges() {
        val dates = listOf("2026-11-05", "2026-11-06", "2026-11-07")
        val codes = listOf(0, 1)
        val maxima = listOf(10.0, 20.0, 30.0)
        val minima = listOf(11.0, 10.0, 20.0)

        assertEquals(listOf(1), DailyForecastSelection.validIndices(dates, codes, maxima, minima))
        assertEquals(emptyList<Int>(), DailyForecastSelection.validIndices(dates, codes, maxima, minima, limit = 0))
    }

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

class HourlyForecastSelectionTest {
    @org.junit.Test
    fun validIndicesCountsUsableHoursInsteadOfStoppingAtTwentyFourRawSlots() {
        val times = listOf(
            "2026-11-05T02garbage",
            "2026-11-05T02:99",
            "2026-11-05T03:00",
            "2026-11-05T04:00",
            "2026-11-05T05:00"
        )
        val codes = listOf(0, 0, 1, 2, 3)

        assertEquals(listOf(2, 3, 4), HourlyForecastSelection.validIndices(
            times, codes, startIndex = 0, limit = 24
        ))
    }

    @org.junit.Test
    fun validIndicesSkipsMissingWeatherCodesAndHonorsLimit() {
        val times = listOf(
            "2026-11-05T00:00",
            "2026-11-05T01:00",
            "2026-11-05T02:00",
            "2026-11-05T03:00"
        )
        val codes = listOf(0, 1, 2)

        assertEquals(listOf(1, 2), HourlyForecastSelection.validIndices(
            times, codes, startIndex = 1, limit = 2
        ))
        assertEquals(emptyList<Int>(), HourlyForecastSelection.validIndices(
            times, codes, startIndex = 0, limit = 0
        ))
    }

    @org.junit.Test
    fun validIndicesReturnsEmptyWhenStartIsAfterAvailableForecast() {
        assertEquals(emptyList<Int>(), HourlyForecastSelection.validIndices(
            listOf("2026-11-05T00:00"), listOf(0), startIndex = 5
        ))
    }
}


class AirQualityMeasurementMapperTest {
    @Test
    fun mapsAllAvailableAirQualityReadingsWithoutChangingTheirValues() {
        val mapped = AirQualityMeasurementMapper.map(
            AirQualityCurrentDto(
                pm25 = 12.4,
                pm10 = 21.8,
                ozone = 83.0,
                nitrogenDioxide = 17.2,
                carbonMonoxide = 245.0,
                europeanAqi = 42
            )
        )

        assertEquals(42, mapped.europeanAqi)
        assertEquals(12.4, mapped.pm25!!, 0.0)
        assertEquals(21.8, mapped.pm10!!, 0.0)
        assertEquals(83.0, mapped.ozone!!, 0.0)
        assertEquals(17.2, mapped.nitrogenDioxide!!, 0.0)
        assertEquals(245.0, mapped.carbonMonoxide!!, 0.0)
        assertEquals(true, mapped.isAvailable)
    }

    @Test
    fun rejectsNegativeAndNonFinitePollutantsAndNegativeAqi() {
        val mapped = AirQualityMeasurementMapper.map(
            AirQualityCurrentDto(
                pm25 = -0.1,
                pm10 = Double.NaN,
                ozone = Double.POSITIVE_INFINITY,
                nitrogenDioxide = -3.0,
                carbonMonoxide = Double.NEGATIVE_INFINITY,
                europeanAqi = -1
            )
        )

        assertNull(mapped.europeanAqi)
        assertNull(mapped.pm25)
        assertNull(mapped.pm10)
        assertNull(mapped.ozone)
        assertNull(mapped.nitrogenDioxide)
        assertNull(mapped.carbonMonoxide)
        assertEquals(false, mapped.isAvailable)
    }

    @Test
    fun keepsPartialReadingsAvailableWithoutInventingMissingValues() {
        val mapped = AirQualityMeasurementMapper.map(
            AirQualityCurrentDto(pm25 = 0.0, carbonMonoxide = 0.0)
        )

        assertEquals(0.0, mapped.pm25!!, 0.0)
        assertEquals(0.0, mapped.carbonMonoxide!!, 0.0)
        assertNull(mapped.pm10)
        assertNull(mapped.ozone)
        assertNull(mapped.nitrogenDioxide)
        assertNull(mapped.europeanAqi)
        assertEquals(true, mapped.isAvailable)
    }

    @Test
    fun missingAirQualityResponseRemainsUnavailable() {
        val mapped = AirQualityMeasurementMapper.map(null)

        assertNull(mapped.europeanAqi)
        assertNull(mapped.pm25)
        assertNull(mapped.pm10)
        assertNull(mapped.ozone)
        assertNull(mapped.nitrogenDioxide)
        assertNull(mapped.carbonMonoxide)
        assertEquals(false, mapped.isAvailable)
    }
}
