package com.example.data.model

import org.junit.Assert.assertEquals
import com.squareup.moshi.Moshi
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherDataProvenanceTest {

    private fun provenance(
        status: WeatherDataStatus = WeatherDataStatus.AVAILABLE
    ) = WeatherDataProvenance(
        provider = "test-provider",
        kind = WeatherDataKind.OBSERVATION,
        status = status,
        observedAtEpochMillis = 1_700_000_000_000
    )

    @Test
    fun missingCurrentWeatherFieldsRemainNullWhenParsingPartialJson() {
        val adapter = Moshi.Builder().build().adapter(CurrentWeatherDto::class.java)
        val dto = adapter.fromJson("""{"temperature_2m": 18.5}""")!!

        assertEquals(18.5, dto.temperature ?: -1.0, 0.0)
        assertNull(dto.time)
        assertNull(dto.relativeHumidity)
        assertNull(dto.apparentTemperature)
        assertNull(dto.isDay)
        assertNull(dto.precipitation)
        assertNull(dto.weatherCode)
        assertNull(dto.surfacePressure)
        assertNull(dto.windSpeed)
        assertNull(dto.windDirection)
        assertNull(dto.uvIndex)
    }

    @Test
    fun emptyCurrentWeatherJsonDoesNotInventMeasurements() {
        val adapter = Moshi.Builder().build().adapter(CurrentWeatherDto::class.java)
        val dto = adapter.fromJson("{}")!!

        assertNull(dto.temperature)
        assertNull(dto.relativeHumidity)
        assertNull(dto.surfacePressure)
        assertNull(dto.windSpeed)
    }

    @Test
    fun measuredZeroIsDisplayable() {
        val value = QualifiedWeatherValue(
            value = 0.0,
            unit = "mm",
            provenance = provenance()
        )

        assertTrue(value.isDisplayable)
    }

    @Test
    fun nonFiniteMeasurementsAreNotDisplayable() {
        listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY).forEach { invalid ->
            val value = QualifiedWeatherValue(
                value = invalid,
                unit = "°C",
                provenance = provenance()
            )

            assertFalse(value.isDisplayable)
        }
    }

    @Test
    fun missingValueIsNotDisplayable() {
        val value = QualifiedWeatherValue(
            value = null,
            unit = "hPa",
            provenance = provenance()
        )

        assertFalse(value.isDisplayable)
    }

    @Test
    fun unavailableSourceIsNotDisplayable() {
        val value = QualifiedWeatherValue(
            value = 12.0,
            unit = "km/h",
            provenance = provenance(WeatherDataStatus.UNAVAILABLE)
        )

        assertFalse(value.isDisplayable)
    }

    @Test
    fun unverifiedStationDefaultsToOfflineDemo() {
        val station = SyrianWeatherStation(
            id = "test",
            nameAr = "محطة اختبار",
            governorate = "اختبار",
            latitude = 33.5,
            longitude = 36.2,
            temperatureCelsius = 0.0,
            humidity = 0,
            pressureHpa = 0.0,
            windSpeedKmh = 0.0,
            conditionAr = "غير متاح",
            conditionType = WeatherConditionType.CLEAR
        )

        assertFalse(station.isOnline)
        assertTrue(station.isDemoData)
    }

    @Test
    fun defaultAirQualityAndPollenAreUnavailable() {
        val air = AirQualityInfo(
            aqiValue = -1,
            categoryAr = "غير متاح",
            adviceAr = "لا توجد بيانات",
            pm25 = 0.0,
            pm10 = 0.0
        )
        val pollen = PollenInfo(
            grassPollen = 0.0,
            birchPollen = 0.0,
            olivePollen = 0.0,
            ragweedPollen = 0.0,
            overallLevelAr = "غير متاح",
            adviceAr = "لا توجد بيانات"
        )

        assertFalse(air.isAvailable)
        assertFalse(air.isAqiAvailable)
        assertFalse(pollen.isAvailable)
    }

    @Test
    fun localCapAlertsDefaultToTestAndPrivate() {
        val alert = CapAlertItem(
            identifier = "LOCAL_TEST",
            sentTimestamp = "now",
            eventAr = "اختبار",
            severity = AlertSeverity.ADVISORY,
            areaDescAr = "اختبار",
            descriptionAr = "اختبار",
            instructionAr = "اختبار",
            expires = "غير محدد"
        )

        assertTrue(alert.status == "Test")
        assertTrue(alert.scope == "Private")
        assertTrue(alert.sender == "local-demo@device.invalid")
    }

    @Test
    fun generatedWeatherAlertsAreNotOfficialByDefault() {
        val alert = SevereWeatherAlert(
            id = "test-alert",
            titleAr = "اختبار",
            severity = AlertSeverity.ADVISORY,
            descriptionAr = "اختبار",
            safetyInstructionsAr = "اختبار",
            source = "test",
            validUntil = "غير محدد"
        )

        assertFalse(alert.isOfficial)
        assertFalse(alert.isLocalDemo)
    }

    @Test
    fun audioBulletinsAreDemoByDefault() {
        val bulletin = AudioBulletinItem(
            id = "test-bulletin",
            title = "اختبار",
            dateFormatted = "غير محدد",
            durationSec = 0,
            announcerName = "صوت تجريبي",
            bulletinScriptAr = "نص اختبار"
        )

        assertTrue(bulletin.isDemoData)
    }

    @Test
    fun simulatedValueCanBeDisplayedButMustRemainIdentifiedAsSimulation() {
        val value = QualifiedWeatherValue(
            value = 4.0,
            unit = "mm",
            provenance = provenance(WeatherDataStatus.SIMULATED)
        )

        assertTrue(value.isDisplayable)
        assertTrue(value.provenance.status == WeatherDataStatus.SIMULATED)
    }

    @Test
    fun legacyDemoStationMapsToSimulatedQualifiedMeasurements() {
        val station = SyrianWeatherStation(
            id = "st-demo",
            nameAr = "محطة تجريبية",
            governorate = "دمشق",
            latitude = 33.5,
            longitude = 36.2,
            temperatureCelsius = 0.0,
            humidity = 0,
            pressureHpa = 1013.0,
            windSpeedKmh = 0.0,
            conditionAr = "تجريبي",
            conditionType = WeatherConditionType.CLEAR
        )

        val observation = station.toQualifiedObservation()

        assertEquals("st-demo", observation.stationId)
        assertEquals(WeatherDataKind.SIMULATION, observation.provenance.kind)
        assertEquals(WeatherDataStatus.SIMULATED, observation.provenance.status)
        assertTrue(observation.temperatureCelsius?.isDisplayable == true)
        assertEquals(0.0, observation.temperatureCelsius?.value ?: -1.0, 0.0)
        assertEquals(WeatherDataStatus.SIMULATED, observation.temperatureCelsius?.provenance?.status)
    }

    @Test
    fun nonDemoStationWithoutVerifiedFeedDoesNotExposeLegacyNumbers() {
        val station = SyrianWeatherStation(
            id = "st-unverified",
            nameAr = "محطة غير موثقة",
            governorate = "سوريا",
            latitude = 33.5,
            longitude = 36.2,
            temperatureCelsius = 24.0,
            humidity = 45,
            pressureHpa = 1014.0,
            windSpeedKmh = 12.0,
            conditionAr = "صافٍ",
            conditionType = WeatherConditionType.CLEAR,
            isOnline = true,
            isDemoData = false
        )

        assertFalse(station.hasVerifiedLiveData)

        val observation = station.toQualifiedObservation()

        assertEquals(WeatherDataKind.UNKNOWN, observation.provenance.kind)
        assertEquals(WeatherDataStatus.UNAVAILABLE, observation.provenance.status)
        assertTrue(observation.temperatureCelsius == null)
        assertTrue(observation.relativeHumidityPercent == null)
        assertTrue(observation.pressureHpa == null)
        assertTrue(observation.windSpeedKmh == null)
    }

    @Test
    fun forecastValidationRejectsOutOfRangePercentages() {
        assertNull(ForecastValueValidation.probabilityPercent(-1))
        assertNull(ForecastValueValidation.probabilityPercent(101))
        assertEquals(0, ForecastValueValidation.probabilityPercent(0))
        assertEquals(100, ForecastValueValidation.probabilityPercent(100))
        assertNull(ForecastValueValidation.humidityPercent(-1))
        assertNull(ForecastValueValidation.humidityPercent(101))
        assertEquals(50, ForecastValueValidation.humidityPercent(50))
    }

    @Test
    fun forecastValidationRejectsNegativeAndNonFiniteValues() {
        assertNull(ForecastValueValidation.nonNegativeFinite(-0.1))
        assertNull(ForecastValueValidation.nonNegativeFinite(Double.NaN))
        assertNull(ForecastValueValidation.nonNegativeFinite(Double.POSITIVE_INFINITY))
        assertEquals(0.0, ForecastValueValidation.nonNegativeFinite(0.0) ?: -1.0, 0.0)
    }

    @Test
    fun dailyTemperatureRangeMustBeFiniteAndOrdered() {
        assertNull(ForecastValueValidation.temperatureRange(10.0, 12.0))
        assertNull(ForecastValueValidation.temperatureRange(Double.NaN, 2.0))
        assertEquals(15.0 to 5.0, ForecastValueValidation.temperatureRange(15.0, 5.0))
    }
}
