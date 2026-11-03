package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.api.NetworkClient
import com.example.data.model.*
import com.example.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class WeatherRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("samaa_weather_prefs", Context.MODE_PRIVATE)

    companion object {
        val DEFAULT_CITIES = listOf(
            CityLocation(name = "دمشق", nameEn = "Damascus", country = "سوريا", latitude = 33.5138, longitude = 36.2765, admin1 = "محافظة دمشق", isFavorite = true),
            CityLocation(name = "حلب", nameEn = "Aleppo", country = "سوريا", latitude = 36.2021, longitude = 37.1343, admin1 = "محافظة حلب", isFavorite = true),
            CityLocation(name = "اللاذقية", nameEn = "Latakia", country = "سوريا", latitude = 35.5317, longitude = 35.7915, admin1 = "محافظة اللاذقية", isFavorite = true),
            CityLocation(name = "حمص", nameEn = "Homs", country = "سوريا", latitude = 34.7324, longitude = 36.7137, admin1 = "محافظة حمص", isFavorite = true),
            CityLocation(name = "طرطوس", nameEn = "Tartus", country = "سوريا", latitude = 34.8890, longitude = 35.8866, admin1 = "محافظة طرطوس", isFavorite = false),
            CityLocation(name = "حماة", nameEn = "Hama", country = "سوريا", latitude = 35.1318, longitude = 36.7578, admin1 = "محافظة حماة", isFavorite = false),
            CityLocation(name = "دير الزور", nameEn = "Deir ez-Zor", country = "سوريا", latitude = 35.3333, longitude = 40.1500, admin1 = "محافظة دير الزور", isFavorite = false),
            CityLocation(name = "القامشلي", nameEn = "Qamishli", country = "سوريا", latitude = 37.0494, longitude = 41.2268, admin1 = "محافظة الحسكة", isFavorite = false),
            CityLocation(name = "السويداء", nameEn = "Sweida", country = "سوريا", latitude = 32.7130, longitude = 36.5694, admin1 = "محافظة السويداء", isFavorite = false),
            CityLocation(name = "درعا", nameEn = "Deraa", country = "سوريا", latitude = 32.6186, longitude = 36.1011, admin1 = "محافظة درعا", isFavorite = false),
            CityLocation(name = "إدلب", nameEn = "Idlib", country = "سوريا", latitude = 35.9306, longitude = 36.6339, admin1 = "محافظة إدلب", isFavorite = false),
            CityLocation(name = "الرقة", nameEn = "Raqqa", country = "سوريا", latitude = 35.9556, longitude = 39.0078, admin1 = "محافظة الرقة", isFavorite = false),
            CityLocation(name = "تدمر", nameEn = "Palmyra", country = "سوريا", latitude = 34.5577, longitude = 38.2753, admin1 = "محافظة حمص", isFavorite = false)
        )

        val DEFAULT_METRIC_PREFERENCES = listOf(
            MetricPreference(WeatherMetricType.UV_INDEX, isEnabled = true),
            MetricPreference(WeatherMetricType.AIR_QUALITY, isEnabled = true),
            MetricPreference(WeatherMetricType.POLLEN_COUNT, isEnabled = true),
            MetricPreference(WeatherMetricType.WIND, isEnabled = true),
            MetricPreference(WeatherMetricType.HUMIDITY, isEnabled = true),
            MetricPreference(WeatherMetricType.SUN_CYCLE, isEnabled = true),
            MetricPreference(WeatherMetricType.PRESSURE, isEnabled = true),
            MetricPreference(WeatherMetricType.VISIBILITY, isEnabled = true)
        )
    }

    suspend fun getFullWeather(location: CityLocation): Result<FullWeatherData> = withContext(Dispatchers.IO) {
        try {
            val forecastDto = NetworkClient.forecastApi.getForecast(
                latitude = location.latitude,
                longitude = location.longitude
            )

            val airQualityDto = try {
                NetworkClient.airQualityApi.getAirQuality(
                    latitude = location.latitude,
                    longitude = location.longitude
                )
            } catch (e: Exception) {
                null
            }

            val currentDto = forecastDto.current ?: CurrentWeatherDto()
            val conditionType = mapWeatherCodeToType(currentDto.weatherCode)
            val conditionNameAr = mapWeatherCodeToAr(currentDto.weatherCode)
            val isDay = currentDto.isDay == 1

            val visMeters = forecastDto.hourly?.visibility?.firstOrNull() ?: 10000.0
            val visibilityKm = (visMeters / 1000.0).coerceIn(0.5, 30.0)

            val currentWeather = CurrentWeather(
                temperatureCelsius = currentDto.temperature,
                feelsLikeCelsius = currentDto.apparentTemperature,
                humidity = currentDto.relativeHumidity,
                windSpeedKmh = currentDto.windSpeed,
                windDirectionDegrees = currentDto.windDirection,
                pressureHpa = currentDto.surfacePressure,
                uvIndex = currentDto.uvIndex,
                weatherCode = currentDto.weatherCode,
                conditionType = conditionType,
                conditionNameAr = conditionNameAr,
                conditionNameEn = mapWeatherCodeToEn(currentDto.weatherCode),
                isDay = isDay,
                precipitationMm = currentDto.precipitation,
                visibilityKm = visibilityKm
            )

            // Parse hourly forecasts (next 24 hours from current time)
            val hourlyList = mutableListOf<HourlyForecast>()
            val hourlyTimes = forecastDto.hourly?.time ?: emptyList()
            val hourlyTemps = forecastDto.hourly?.temperature ?: emptyList()
            val hourlyCodes = forecastDto.hourly?.weatherCode ?: emptyList()
            val hourlyPrecip = forecastDto.hourly?.precipitationProbability ?: emptyList()
            val hourlyHum = forecastDto.hourly?.relativeHumidity ?: emptyList()
            val hourlyWind = forecastDto.hourly?.windSpeed ?: emptyList()

            val nowIsoPrefix = SimpleDateFormat("yyyy-MM-dd'T'HH", Locale.US).format(Date())
            var startIndex = hourlyTimes.indexOfFirst { it.startsWith(nowIsoPrefix) }
            if (startIndex < 0) startIndex = 0

            val endIndex = (startIndex + 24).coerceAtMost(hourlyTimes.size)
            for (i in startIndex until endIndex) {
                val rawTime = hourlyTimes[i]
                val hourLabel = formatHourLabel(rawTime, i == startIndex)
                val code = hourlyCodes.getOrElse(i) { currentDto.weatherCode }
                val temp = hourlyTemps.getOrElse(i) { currentDto.temperature }
                val precip = hourlyPrecip.getOrElse(i) { 0 }
                val hum = hourlyHum.getOrElse(i) { currentDto.relativeHumidity }
                val wind = hourlyWind.getOrElse(i) { currentDto.windSpeed }
                val hourOfDay = try { rawTime.substring(11, 13).toInt() } catch (e: Exception) { 12 }
                val hourIsDay = hourOfDay in 6..18

                hourlyList.add(
                    HourlyForecast(
                        timeRaw = rawTime,
                        hourLabel = hourLabel,
                        temperatureCelsius = temp,
                        conditionType = mapWeatherCodeToType(code),
                        weatherCode = code,
                        precipitationProb = precip,
                        humidity = hum,
                        windSpeedKmh = wind,
                        isDay = hourIsDay
                    )
                )
            }

            // Parse daily forecasts (next 7 days)
            val dailyList = mutableListOf<DailyForecast>()
            val dailyTimes = forecastDto.daily?.time ?: emptyList()
            val dailyCodes = forecastDto.daily?.weatherCode ?: emptyList()
            val dailyMax = forecastDto.daily?.temperatureMax ?: emptyList()
            val dailyMin = forecastDto.daily?.temperatureMin ?: emptyList()
            val dailyPrecip = forecastDto.daily?.precipitationProbabilityMax ?: emptyList()
            val dailyUv = forecastDto.daily?.uvIndexMax ?: emptyList()
            val dailySunrise = forecastDto.daily?.sunrise ?: emptyList()
            val dailySunset = forecastDto.daily?.sunset ?: emptyList()

            for (i in dailyTimes.indices) {
                if (i >= 7) break
                val dateRaw = dailyTimes[i]
                val code = dailyCodes.getOrElse(i) { 0 }
                val (dayAr, dayEn, formattedDate) = formatDayLabels(dateRaw, i == 0)
                val sunriseFormatted = formatSunTime(dailySunrise.getOrNull(i))
                val sunsetFormatted = formatSunTime(dailySunset.getOrNull(i))

                dailyList.add(
                    DailyForecast(
                        dateRaw = dateRaw,
                        dayNameAr = dayAr,
                        dayNameEn = dayEn,
                        formattedDate = formattedDate,
                        conditionType = mapWeatherCodeToType(code),
                        weatherCode = code,
                        conditionNameAr = mapWeatherCodeToAr(code),
                        maxTempCelsius = dailyMax.getOrElse(i) { 25.0 },
                        minTempCelsius = dailyMin.getOrElse(i) { 18.0 },
                        precipitationProb = dailyPrecip.getOrElse(i) { 0 },
                        uvIndexMax = dailyUv.getOrElse(i) { 5.0 },
                        sunrise = sunriseFormatted,
                        sunset = sunsetFormatted
                    )
                )
            }

            // Air Quality
            val aqiVal = airQualityDto?.current?.europeanAqi ?: 35
            val pm25Val = airQualityDto?.current?.pm25 ?: 12.0
            val pm10Val = airQualityDto?.current?.pm10 ?: 25.0
            val ozoneVal = airQualityDto?.current?.ozone ?: 38.0
            val no2Val = airQualityDto?.current?.nitrogenDioxide ?: 16.0
            val (aqiCat, aqiAdvice) = getAqiDescription(aqiVal)

            val airQuality = AirQualityInfo(
                aqiValue = aqiVal,
                categoryAr = aqiCat,
                adviceAr = aqiAdvice,
                pm25 = pm25Val,
                pm10 = pm10Val,
                ozone = ozoneVal,
                no2 = no2Val
            )

            // Pollen Information
            val grass = airQualityDto?.current?.grassPollen ?: 8.0
            val birch = airQualityDto?.current?.birchPollen ?: 3.0
            val olive = airQualityDto?.current?.olivePollen ?: 12.0
            val ragweed = airQualityDto?.current?.ragweedPollen ?: 4.0
            val pollenInfo = calculatePollenInfo(grass, birch, olive, ragweed)

            // Severe Weather Alerts detection
            val alerts = detectSevereWeatherAlerts(location, currentWeather, dailyList, aqiVal, pollenInfo)

            // Push notifications if enabled and severe alerts exist
            if (areAlertsEnabled()) {
                alerts.firstOrNull { it.severity == AlertSeverity.WARNING || it.severity == AlertSeverity.EMERGENCY }?.let { alert ->
                    // Notify once per alert ID
                    val lastNotifiedId = prefs.getString("last_notified_alert_id", "")
                    if (lastNotifiedId != alert.id) {
                        NotificationHelper.postSevereWeatherNotification(context, alert)
                        prefs.edit().putString("last_notified_alert_id", alert.id).apply()
                    }
                }
            }

            // Weather Insights
            val insights = generateInsights(currentWeather, dailyList.firstOrNull())

            Result.success(
                FullWeatherData(
                    location = location,
                    current = currentWeather,
                    hourly = hourlyList,
                    daily = dailyList,
                    airQuality = airQuality,
                    pollen = pollenInfo,
                    alerts = alerts,
                    insights = insights
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchCities(query: String): List<CityLocation> = withContext(Dispatchers.IO) {
        if (query.trim().isEmpty()) return@withContext emptyList()
        try {
            val response = NetworkClient.geocodingApi.searchCity(name = query.trim())
            response.results?.map { dto ->
                CityLocation(
                    name = dto.name,
                    nameEn = dto.name,
                    country = dto.country ?: "",
                    latitude = dto.latitude,
                    longitude = dto.longitude,
                    admin1 = dto.admin1,
                    isFavorite = isCityFavorite(dto.name, dto.country ?: "")
                )
            } ?: emptyList()
        } catch (e: Exception) {
            DEFAULT_CITIES.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.nameEn.contains(query, ignoreCase = true) ||
                it.country.contains(query, ignoreCase = true)
            }
        }
    }

    // Favorite Cities Management
    fun getSavedFavoriteCities(): List<CityLocation> {
        val jsonStr = prefs.getString("favorite_cities", null)
        if (jsonStr.isNullOrEmpty()) {
            return DEFAULT_CITIES.filter { it.isFavorite }
        }
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<CityLocation>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    CityLocation(
                        name = obj.getString("name"),
                        nameEn = obj.optString("nameEn", ""),
                        country = obj.getString("country"),
                        latitude = obj.getDouble("latitude"),
                        longitude = obj.getDouble("longitude"),
                        admin1 = obj.optString("admin1", null),
                        isFavorite = true
                    )
                )
            }
            if (list.isEmpty()) DEFAULT_CITIES.filter { it.isFavorite } else list
        } catch (e: Exception) {
            DEFAULT_CITIES.filter { it.isFavorite }
        }
    }

    fun toggleFavoriteCity(city: CityLocation): Boolean {
        val currentFavorites = getSavedFavoriteCities().toMutableList()
        val existsIndex = currentFavorites.indexOfFirst {
            it.name.equals(city.name, ignoreCase = true) &&
            (it.country.isEmpty() || city.country.isEmpty() || it.country.equals(city.country, ignoreCase = true))
        }

        val isNowFavorite = if (existsIndex >= 0) {
            currentFavorites.removeAt(existsIndex)
            false
        } else {
            currentFavorites.add(city.copy(isFavorite = true))
            true
        }

        saveFavorites(currentFavorites)
        return isNowFavorite
    }

    fun isCityFavorite(name: String, country: String): Boolean {
        return getSavedFavoriteCities().any {
            it.name.equals(name, ignoreCase = true)
        }
    }

    private fun saveFavorites(list: List<CityLocation>) {
        val array = JSONArray()
        for (c in list) {
            val obj = JSONObject().apply {
                put("name", c.name)
                put("nameEn", c.nameEn)
                put("country", c.country)
                put("latitude", c.latitude)
                put("longitude", c.longitude)
                c.admin1?.let { put("admin1", it) }
            }
            array.put(obj)
        }
        prefs.edit().putString("favorite_cities", array.toString()).apply()
    }

    // Metric Preferences Customization & Prioritization
    fun getMetricPreferences(): List<MetricPreference> {
        val jsonStr = prefs.getString("custom_metric_prefs", null)
        if (jsonStr.isNullOrEmpty()) {
            return DEFAULT_METRIC_PREFERENCES
        }
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<MetricPreference>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.getString("id")
                val isEnabled = obj.optBoolean("isEnabled", true)
                val type = WeatherMetricType.values().firstOrNull { it.id == id }
                if (type != null) {
                    list.add(MetricPreference(type, isEnabled))
                }
            }
            // Add any missing types
            WeatherMetricType.values().forEach { missingType ->
                if (list.none { it.type == missingType }) {
                    list.add(MetricPreference(missingType, true))
                }
            }
            list
        } catch (e: Exception) {
            DEFAULT_METRIC_PREFERENCES
        }
    }

    fun saveMetricPreferences(preferences: List<MetricPreference>) {
        val array = JSONArray()
        for (pref in preferences) {
            val obj = JSONObject().apply {
                put("id", pref.type.id)
                put("isEnabled", pref.isEnabled)
            }
            array.put(obj)
        }
        prefs.edit().putString("custom_metric_prefs", array.toString()).apply()
    }

    // Units & Alerts toggles
    fun isFahrenheit(): Boolean = prefs.getBoolean("use_fahrenheit", false)
    fun setFahrenheit(enabled: Boolean) = prefs.edit().putBoolean("use_fahrenheit", enabled).apply()

    fun areAlertsEnabled(): Boolean = prefs.getBoolean("enable_severe_alerts", true)
    fun setAlertsEnabled(enabled: Boolean) = prefs.edit().putBoolean("enable_severe_alerts", enabled).apply()

    fun testPushNotification(alert: SevereWeatherAlert) {
        NotificationHelper.postSevereWeatherNotification(context, alert)
    }

    // Pollen Calculation Helper
    private fun calculatePollenInfo(grass: Double, birch: Double, olive: Double, ragweed: Double): PollenInfo {
        val total = grass + birch + olive + ragweed
        val (level, advice) = when {
            total <= 10.0 -> "منخفض جداً" to "مستويات حبوب اللقاح منخفضة، الجو ملائم جداً لمرضى الحساسية والربو."
            total <= 30.0 -> "معتدل" to "مستويات مقبولة لحبوب اللقاح، قد يواجه شديدو الحساسية أعراضاً خفيفة."
            total <= 60.0 -> "مرتفع" to "تركيزات عالية لحبوب اللقاح؛ يُنصح بإغلاق النوافذ وتناول مضادات الهيستامين."
            else -> "شديد الارتفاع" to "تحذير حساسية! تجنب البقاء في الحدائق والمناطق المفتوحة قدر الإمكان."
        }
        return PollenInfo(
            grassPollen = grass,
            birchPollen = birch,
            olivePollen = olive,
            ragweedPollen = ragweed,
            overallLevelAr = level,
            adviceAr = advice
        )
    }

    // Severe Weather Alert Detection
    private fun detectSevereWeatherAlerts(
        location: CityLocation,
        current: CurrentWeather,
        daily: List<DailyForecast>,
        aqi: Int,
        pollen: PollenInfo
    ): List<SevereWeatherAlert> {
        val list = mutableListOf<SevereWeatherAlert>()
        val today = daily.firstOrNull()

        // 1. Thunderstorm Alert
        if (current.weatherCode in listOf(95, 96, 99) || current.conditionType == WeatherConditionType.THUNDERSTORM) {
            list.add(
                SevereWeatherAlert(
                    id = "thunderstorm_${location.name}",
                    titleAr = "تحذير من عواصف رعدية نشطة في ${location.name}",
                    severity = AlertSeverity.WARNING,
                    descriptionAr = "نشاط لخلايا رعدية مصحوبة برياح هابطة قوية وهطول غزير للأمطار، مع احتمالية تساقط حبات البَرَد.",
                    safetyInstructionsAr = "ابتعد عن الأماكن المكشوفة وأعمدة الإنارة، وتجنب النزول للأودية وتجمعات المياه.",
                    source = "المركز الوطني للأرصاد والإنذار المبكر",
                    validUntil = "خلال الـ 6 ساعات القادمة"
                )
            )
        }

        // 2. Heavy Rain & Flooding Alert
        if (current.precipitationMm >= 10.0 || (today?.precipitationProb ?: 0) >= 75 || current.conditionType == WeatherConditionType.HEAVY_RAIN) {
            list.add(
                SevereWeatherAlert(
                    id = "heavy_rain_${location.name}",
                    titleAr = "إنذار أمطار غزيرة وجريان السيول",
                    severity = AlertSeverity.WARNING,
                    descriptionAr = "توقعات بهطول أمطار غزيرة تؤدي إلى ارتفاع منسوب المياه وضعف مدى الرؤية الأفقية على الطرق.",
                    safetyInstructionsAr = "توخَّ الحيطة أثناء القيادة، واترك مسافة أمان كافية، وابتعد تماماً عن مجاري السيول.",
                    source = "الرصد الهيدرولوجي والأرصاد الجوية",
                    validUntil = "حتى منتصف الليل"
                )
            )
        }

        // 3. Extreme Heatwave Alert
        if (current.temperatureCelsius >= 38.0 || (today?.maxTempCelsius ?: 0.0) >= 40.0) {
            list.add(
                SevereWeatherAlert(
                    id = "heatwave_${location.name}",
                    titleAr = "تحذير من موجة حر شديدة وجفاف",
                    severity = AlertSeverity.WATCH,
                    descriptionAr = "ارتفاع ملموس على درجات الحرارة يتجاوز المعدلات الموسمية، مع طقس شديد الحرارة وضربات شمس محتملة.",
                    safetyInstructionsAr = "تجنب التعرض المباشر لأشعة الشمس بين 11 صباحاً و 4 عصراً، واحرص على شرب السوائل بوفرة.",
                    source = "إدارة الطوارئ والأرصاد",
                    validUntil = "مستمرة حتى مساء اليوم"
                )
            )
        }

        // 4. Gale / High Wind Warning
        if (current.windSpeedKmh >= 45.0) {
            list.add(
                SevereWeatherAlert(
                    id = "wind_${location.name}",
                    titleAr = "تنبيه رياح نشطة وعواصف ترابية",
                    severity = AlertSeverity.ADVISORY,
                    descriptionAr = "رياح شديدة السرعة تتجاوز ${current.windSpeedKmh.toInt()} كم/س مع هبات قوية قد تثير الأتربة والغبار وتعيق حركة السير.",
                    safetyInstructionsAr = "تثبيت الأجسام القابلة للتطاير والحذر عند القيادة على الطرق السريعة المفتوحة.",
                    source = "هيئة الأرصاد وحماية البيئة",
                    validUntil = "خلال الساعات المقبلة"
                )
            )
        }

        // 5. Heavy Snow Alert
        if (current.conditionType == WeatherConditionType.SNOW || current.weatherCode in listOf(71, 73, 75)) {
            list.add(
                SevereWeatherAlert(
                    id = "snow_${location.name}",
                    titleAr = "إنذار تساقط ثلوج وتجمد على الطرقات",
                    severity = AlertSeverity.WARNING,
                    descriptionAr = "تساقط متواصل للثلوج مع تشكل طبقات جليدية وانزلاقات خطيرة على المرتفعات والطرق.",
                    safetyInstructionsAr = "استخدم سلاسل الإطارات وتجنب السفر غير الضروري حتى انقشاع الحالة الجوية.",
                    source = "مكتب الأرصاد الشتوية",
                    validUntil = "حتى صباح الغد"
                )
            )
        }

        // 6. Dust Storm / Hazardous AQI Alert
        if (aqi >= 75) {
            list.add(
                SevereWeatherAlert(
                    id = "air_quality_${location.name}",
                    titleAr = "تنبيه تلوث هواء وعوالق ترابية",
                    severity = AlertSeverity.ADVISORY,
                    descriptionAr = "جودة الهواء منخفضة بسبب العوالق والجسيمات الدقيقة في الجو (AQI: $aqi).",
                    safetyInstructionsAr = "ارتداء كمامة لمرضى الجهاز التنفسي والحد من الأنشطة البدنية في الخارج.",
                    source = "مؤشر الرصد البيئي وجودة الهواء",
                    validUntil = "طوال فترة بقاء العوالق"
                )
            )
        }

        // 7. Pollen Peak Alert
        if (pollen.overallLevelAr == "شديد الارتفاع") {
            list.add(
                SevereWeatherAlert(
                    id = "pollen_${location.name}",
                    titleAr = "تحذير ذروة انتشار حبوب اللقاح",
                    severity = AlertSeverity.ADVISORY,
                    descriptionAr = "تسجيل تركيزات استثنائية لحبوب اللقاح والأعشاب المسببة للحساسية الموسمية.",
                    safetyInstructionsAr = "إحكام إغلاق النوافذ، وغسل الوجه والعينين بعد العودة للمنزل، واستشارة الصيدلي.",
                    source = "شبكة مراقبة حبوب اللقاح والحساسية",
                    validUntil = "مستمر خلال موسم التزهير"
                )
            )
        }

        return list
    }

    private fun mapWeatherCodeToType(code: Int): WeatherConditionType {
        return when (code) {
            0 -> WeatherConditionType.CLEAR
            1, 2 -> WeatherConditionType.PARTLY_CLOUDY
            3 -> WeatherConditionType.CLOUDY
            45, 48 -> WeatherConditionType.FOG
            51, 53, 55 -> WeatherConditionType.DRIZZLE
            56, 57 -> WeatherConditionType.DRIZZLE
            61, 63 -> WeatherConditionType.RAIN
            65 -> WeatherConditionType.HEAVY_RAIN
            66, 67 -> WeatherConditionType.RAIN
            71, 73, 75, 77 -> WeatherConditionType.SNOW
            80, 81 -> WeatherConditionType.RAIN
            82 -> WeatherConditionType.HEAVY_RAIN
            85, 86 -> WeatherConditionType.SNOW
            95, 96, 99 -> WeatherConditionType.THUNDERSTORM
            else -> WeatherConditionType.PARTLY_CLOUDY
        }
    }

    private fun mapWeatherCodeToAr(code: Int): String {
        return when (code) {
            0 -> "صافٍ ومشرق"
            1 -> "صافٍ غالباً"
            2 -> "غائم جزئياً"
            3 -> "غائم كلياً"
            45 -> "ضباب خفيف"
            48 -> "ضباب متجمد كثيف"
            51 -> "رذاذ خفيف"
            53 -> "رذاذ معتدل"
            55 -> "رذاذ كثيف"
            56, 57 -> "رذاذ متجمد"
            61 -> "أمطار خفيفة"
            63 -> "أمطار معتدلة"
            65 -> "أمطار غزيرة"
            66, 67 -> "أمطار جليدية"
            71 -> "ثلوج خفيفة"
            73 -> "تساقط ثلوج"
            75 -> "ثلوج كثيفة"
            77 -> "حبيبات ثلجية"
            80 -> "زخات مطر خفيفة"
            81 -> "زخات مطر معتدلة"
            82 -> "زخات مطر عنيفة"
            85, 86 -> "زخات ثلجية"
            95 -> "عاصفة رعدية"
            96, 99 -> "عاصفة رعدية مع بَرَد"
            else -> "معتدل"
        }
    }

    private fun mapWeatherCodeToEn(code: Int): String {
        return when (code) {
            0 -> "Clear Sky"
            1 -> "Mainly Clear"
            2 -> "Partly Cloudy"
            3 -> "Overcast"
            45, 48 -> "Foggy"
            51, 53, 55 -> "Drizzle"
            61, 63 -> "Rain"
            65 -> "Heavy Rain"
            71, 73, 75 -> "Snow"
            80, 81, 82 -> "Rain Showers"
            95, 96, 99 -> "Thunderstorm"
            else -> "Variable"
        }
    }

    private fun formatHourLabel(isoString: String, isNow: Boolean): String {
        if (isNow) return "الآن"
        return try {
            val hour = isoString.substring(11, 13).toInt()
            when {
                hour == 0 -> "12 ص"
                hour < 12 -> "$hour ص"
                hour == 12 -> "12 م"
                else -> "${hour - 12} م"
            }
        } catch (e: Exception) {
            isoString
        }
    }

    private fun formatDayLabels(dateStr: String, isToday: Boolean): Triple<String, String, String> {
        if (isToday) return Triple("اليوم", "Today", "اليوم")
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = sdf.parse(dateStr) ?: return Triple(dateStr, dateStr, dateStr)
            val cal = Calendar.getInstance().apply { time = date }
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

            val (arDay, enDay) = when (dayOfWeek) {
                Calendar.SATURDAY -> "السبت" to "Saturday"
                Calendar.SUNDAY -> "الأحد" to "Sunday"
                Calendar.MONDAY -> "الاثنين" to "Monday"
                Calendar.TUESDAY -> "الثلاثاء" to "Tuesday"
                Calendar.WEDNESDAY -> "الأربعاء" to "Wednesday"
                Calendar.THURSDAY -> "الخميس" to "Thursday"
                Calendar.FRIDAY -> "الجمعة" to "Friday"
                else -> "" to ""
            }

            val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
            val month = cal.get(Calendar.MONTH) + 1
            val formatted = "$dayOfMonth/$month"
            Triple(arDay, enDay, formatted)
        } catch (e: Exception) {
            Triple(dateStr, dateStr, dateStr)
        }
    }

    private fun formatSunTime(raw: String?): String {
        if (raw.isNullOrEmpty()) return "--:--"
        return try {
            val timePart = raw.substringAfter("T").take(5)
            val parts = timePart.split(":")
            val hour = parts[0].toInt()
            val minute = parts[1]
            if (hour < 12) {
                "${if (hour == 0) 12 else hour}:$minute ص"
            } else {
                "${if (hour == 12) 12 else hour - 12}:$minute م"
            }
        } catch (e: Exception) {
            raw
        }
    }

    private fun getAqiDescription(aqi: Int): Pair<String, String> {
        return when {
            aqi <= 20 -> "ممتاز" to "جودة الهواء نقية جداً ومثالية للأنشطة الخارجية والرياضة."
            aqi <= 40 -> "جيد" to "جودة هواء مقبولة ومناسبة للجميع دون أي مخاطر صحية."
            aqi <= 60 -> "معتدل" to "الهواء مقبول، قد يشعر الحساسون جداً بتهيج خفيف."
            aqi <= 80 -> "غير صحي للحساسين" to "يفضل تقليل الأنشطة الشاقة في الهواء الطلق لمن يعانون من الحساسية."
            else -> "غير صحي" to "ينصح بارتداء كمامة وتجنب البقاء في الهواء الطلق لفترات طويلة."
        }
    }

    private fun generateInsights(current: CurrentWeather, today: DailyForecast?): WeatherInsights {
        val temp = current.temperatureCelsius
        val uv = current.uvIndex
        val rain = current.precipitationMm > 0.5 || (today?.precipitationProb ?: 0) > 40
        val wind = current.windSpeedKmh

        val umbrellaNeeded = rain || current.conditionType == WeatherConditionType.RAIN || current.conditionType == WeatherConditionType.HEAVY_RAIN

        val clothing = when {
            temp >= 32.0 -> "ملابس قطنية صيفية خفيفة جداً مع شرب كميات وافرة من المياه."
            temp >= 24.0 -> "ملابس ربيعية مريحة وخفيفة مناسبة لدرجات الحرارة المعتدلة."
            temp >= 16.0 -> "ملابس خريفية أو سترة خفيفة للأوقات المسائية المائلة للاعتدال."
            temp >= 9.0 -> "معطف متوسط أو ملابس دافئة للحماية من التيارات الباردة."
            else -> "معطف شتوي ثقيل وقفازات للوقاية من البرودة الشديدة."
        }

        val activityScore = when {
            rain || current.conditionType == WeatherConditionType.THUNDERSTORM -> "يفضل البقاء بالداخل"
            wind > 45.0 -> "رياح نشطة - احذر القيادة"
            temp in 18.0..28.0 && uv < 7 -> "مثالي للمشي والرياضة"
            temp > 35.0 || uv >= 8.0 -> "تجنب التعرض المباشر للشمس"
            else -> "طقس ملائم للأنشطة"
        }

        val title = when {
            umbrellaNeeded -> "فرصة لهطول الأمطار ☔"
            uv >= 8 -> "تحذير أشعة شمس قوية ☀️"
            wind > 40 -> "رياح نشطة وموجات هواء 💨"
            temp in 20.0..27.0 -> "أجواء مثالية للخروج 🌿"
            temp > 35.0 -> "طقس حار وموجة دافئة 🌡️"
            else -> "نصيحة طقس اليوم 🌤️"
        }

        val summary = buildString {
            if (umbrellaNeeded) append("احرص على أخذ مظلتك اليوم لاحتمال هطول الأمطار. ")
            if (uv >= 8) append("المؤشر فوق البنفسجي مرتفع (${uv.toInt()})؛ استخدم واقي الشمس. ")
            if (wind > 35) append("سرعة الرياح تصل إلى ${wind.toInt()} كم/س. ")
            if (isEmpty()) append("استمتع بأجواء ${current.conditionNameAr} طوال اليوم.")
        }

        return WeatherInsights(
            adviceTitle = title,
            adviceSummary = summary,
            umbrellaNeeded = umbrellaNeeded,
            outdoorActivityScore = activityScore,
            clothingAdvice = clothing
        )
    }

    // Syrian Weather Stations (محطات الأرصاد الجوية السورية)
    fun getSyrianWeatherStations(): List<SyrianWeatherStation> {
        return listOf(
            SyrianWeatherStation("st_damascus", "محطة دمشق الدولي", "محطة رئيسية - دمشق", 33.5138, 36.2765, 24.0, 45, 1014.2, 14.0, "صافٍ ومشرق", WeatherConditionType.CLEAR, true),
            SyrianWeatherStation("st_aleppo", "محطة حلب الدولي", "محطة رئيسية - حلب", 36.2021, 37.1343, 21.0, 52, 1013.8, 16.5, "غائم جزئياً", WeatherConditionType.PARTLY_CLOUDY, true),
            SyrianWeatherStation("st_latakia", "محطة اللاذقية البحرية", "محطة ساحلية - اللاذقية", 35.5317, 35.7915, 26.0, 78, 1012.5, 22.0, "رذاذ خفيف", WeatherConditionType.DRIZZLE, true),
            SyrianWeatherStation("st_homs", "محطة حمص المركزية", "محطة داخلية - حمص", 34.7324, 36.7137, 23.0, 50, 1013.0, 18.0, "صافٍ", WeatherConditionType.CLEAR, true),
            SyrianWeatherStation("st_tartus", "محطة طرطوس الميناء", "محطة ساحلية - طرطوس", 34.8890, 35.8866, 25.0, 74, 1012.8, 19.5, "غائم جزئياً", WeatherConditionType.PARTLY_CLOUDY, true),
            SyrianWeatherStation("st_hama", "محطة حماة الزراعية", "محطة داخلية - حماة", 35.1318, 36.7578, 24.0, 48, 1013.2, 15.0, "صافٍ ومشرق", WeatherConditionType.CLEAR, true),
            SyrianWeatherStation("st_deir", "محطة دير الزور", "محطة الشرق - دير الزور", 35.3333, 40.1500, 28.0, 35, 1011.5, 12.0, "مشمس وجاف", WeatherConditionType.CLEAR, true),
            SyrianWeatherStation("st_qamishli", "محطة القامشلي", "محطة الجزيرة - القامشلي", 37.0494, 41.2268, 22.0, 42, 1013.5, 17.0, "صافٍ", WeatherConditionType.CLEAR, true),
            SyrianWeatherStation("st_sweida", "محطة السويداء", "محطة الجبل - السويداء", 32.7130, 36.5694, 19.0, 55, 1015.0, 13.0, "لطيف", WeatherConditionType.PARTLY_CLOUDY, true),
            SyrianWeatherStation("st_deraa", "محطة درعا السهل", "محطة الجنوب - درعا", 32.6186, 36.1011, 23.0, 46, 1014.0, 16.0, "صافٍ", WeatherConditionType.CLEAR, true),
            SyrianWeatherStation("st_idlib", "محطة إدلب", "محطة الشمال - إدلب", 35.9306, 36.6339, 21.0, 58, 1013.1, 14.0, "غائم جزئياً", WeatherConditionType.PARTLY_CLOUDY, true),
            SyrianWeatherStation("st_raqqa", "محطة الرقة", "محطة الفرات - الرقة", 35.9556, 39.0078, 27.0, 38, 1012.0, 15.5, "صافٍ", WeatherConditionType.CLEAR, true),
            SyrianWeatherStation("st_palmyra", "محطة تدمر الأثرية", "محطة البادية - تدمر", 34.5577, 38.2753, 29.0, 30, 1011.0, 18.5, "مشمس", WeatherConditionType.CLEAR, true)
        )
    }

    fun getAudioBulletin(): AudioBulletinItem {
        val title = prefs.getString("admin_bulletin_title", "النشرة الجوية الصباحية الشاملة - المركز الوطني للأرصاد والمناخ") ?: "النشرة الجوية الصباحية الشاملة"
        val script = prefs.getString("admin_bulletin_script", "تستمر درجات الحرارة حول معدلاتها لمثل هذه الفترة من السنة، والجو صيفياً مائلاً للاعتدال في المناطق الجبلية وحاراً نسبياً في البادية والشرقية، مع ظهور بعض السحب المتفرقة على الساحل السوري وتكون الرياح غربية إلى شمالية غربية معتدلة السرعة تنشط أحياناً بعد الظهر. تحذير من التعرض المباشر لأشعة الشمس في أوقات الذروة.") ?: ""
        return AudioBulletinItem(
            id = "bulletin_mcnc_01",
            title = title,
            dateFormatted = "اليوم، 07:30 صباحاً",
            durationSec = 54,
            announcerName = "رئيس قسم التنبؤات (MCNC)",
            bulletinScriptAr = script
        )
    }

    fun saveAdminAudioBulletin(title: String, script: String) {
        prefs.edit()
            .putString("admin_bulletin_title", title)
            .putString("admin_bulletin_script", script)
            .apply()
    }
}

