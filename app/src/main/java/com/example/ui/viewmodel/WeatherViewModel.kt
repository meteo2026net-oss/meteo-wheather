package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.WeatherRepository
import com.example.util.NotificationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WeatherUiState(
    val weatherData: FullWeatherData? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val favoriteCities: List<CityLocation> = emptyList(),
    val selectedCity: CityLocation = WeatherRepository.DEFAULT_CITIES[0],
    val isFahrenheit: Boolean = false,
    val isSearchSheetOpen: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<CityLocation> = emptyList(),
    val isLoadingSearch: Boolean = false,
    val isCustomizeSheetOpen: Boolean = false,
    val isAdminDashboardOpen: Boolean = false,
    val metricPreferences: List<MetricPreference> = WeatherRepository.DEFAULT_METRIC_PREFERENCES,
    val areAlertsEnabled: Boolean = true,
    val isLeafletMapScreenOpen: Boolean = false,
    val cityMapForecasts: List<CityMapForecast> = emptyList(),
    val isLoadingCityMapForecasts: Boolean = false,
    val cityMapForecastError: String? = null,
    val userGoogleEmail: String? = null,
    val isGoogleSignInModalOpen: Boolean = false,
    val chatMessages: List<ChatMessageItem> = emptyList(),
    val isGeneratingAiChat: Boolean = false,
    val isVoiceListening: Boolean = false,
    val isVoiceSpeaking: Boolean = false,
    val lastVoiceQuery: String = "",
    val lastAiVoiceResponse: String = "",
    val syrianStations: List<SyrianWeatherStation> = emptyList(),
    val audioBulletin: AudioBulletinItem = AudioBulletinItem("1", "نشرة تجريبية", "وقت النشر غير محدد", 0, "صوت آلي تجريبي", "لا يوجد نص نشرة محمل."),
    val customAdminAlerts: List<SevereWeatherAlert> = emptyList()
)

class WeatherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WeatherRepository(application)

    private val _uiState = MutableStateFlow(
        WeatherUiState(
            favoriteCities = repository.getSavedFavoriteCities(),
            isFahrenheit = repository.isFahrenheit(),
            metricPreferences = repository.getMetricPreferences(),
            areAlertsEnabled = repository.areAlertsEnabled(),
            syrianStations = repository.getSyrianWeatherStations(),
            audioBulletin = repository.getAudioBulletin()
        )
    )
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        NotificationHelper.createNotificationChannel(application)
        val initialCity = _uiState.value.favoriteCities.firstOrNull() ?: WeatherRepository.DEFAULT_CITIES[0]
        loadWeather(initialCity)
    }

    fun loadWeather(city: CityLocation) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, selectedCity = city) }
            val result = repository.getFullWeather(city)
            result.onSuccess { data ->
                // Prepend any custom admin CAP alerts if present
                val combinedAlerts = _uiState.value.customAdminAlerts + data.alerts
                val updatedData = data.copy(alerts = combinedAlerts)

                _uiState.update {
                    it.copy(
                        weatherData = updatedData,
                        isLoading = false,
                        errorMessage = null,
                        selectedCity = city
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "تعذر جلب بيانات الطقس، تحقق من الاتصال بالإنترنت"
                    )
                }
            }
        }
    }

    fun refreshWeather() {
        loadWeather(_uiState.value.selectedCity)
    }

    fun toggleUnit() {
        val newFahrenheit = !_uiState.value.isFahrenheit
        repository.setFahrenheit(newFahrenheit)
        _uiState.update { it.copy(isFahrenheit = newFahrenheit) }
    }

    fun openSearchSheet() {
        _uiState.update { it.copy(isSearchSheetOpen = true, searchQuery = "", searchResults = emptyList()) }
    }

    fun closeSearchSheet() {
        _uiState.update { it.copy(isSearchSheetOpen = false) }
    }

    fun openCustomizeSheet() {
        _uiState.update { it.copy(isCustomizeSheetOpen = true) }
    }

    fun closeCustomizeSheet() {
        _uiState.update { it.copy(isCustomizeSheetOpen = false) }
    }

    fun openAdminDashboard() {
        _uiState.update { it.copy(isAdminDashboardOpen = true) }
    }

    fun closeAdminDashboard() {
        _uiState.update { it.copy(isAdminDashboardOpen = false) }
    }

    fun openLeafletMapScreen() {
        _uiState.update { it.copy(isLeafletMapScreenOpen = true) }
        loadSyrianCityMapForecasts()
    }

    fun loadSyrianCityMapForecasts(forceRefresh: Boolean = false) {
        if (_uiState.value.isLoadingCityMapForecasts) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCityMapForecasts = true, cityMapForecastError = null) }
            try {
                val forecasts = repository.getSyrianCityMapForecasts(forceRefresh)
                _uiState.update { it.copy(cityMapForecasts = forecasts, isLoadingCityMapForecasts = false) }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(isLoadingCityMapForecasts = false, cityMapForecastError = error.localizedMessage ?: "تعذر تحميل توقعات المدن")
                }
            }
        }
    }

    fun closeLeafletMapScreen() {
        _uiState.update { it.copy(isLeafletMapScreenOpen = false) }
    }

    fun saveMetricPreferences(preferences: List<MetricPreference>) {
        repository.saveMetricPreferences(preferences)
        _uiState.update { it.copy(metricPreferences = preferences) }
    }

    fun toggleAlertsNotification(enabled: Boolean) {
        repository.setAlertsEnabled(enabled)
        _uiState.update { it.copy(areAlertsEnabled = enabled) }
    }

    fun testPushNotification(alert: SevereWeatherAlert) {
        repository.testPushNotification(alert)
    }

    fun broadcastCapAlert(capAlert: CapAlertItem) {
        val severeAlert = SevereWeatherAlert(
            id = capAlert.identifier,
            titleAr = capAlert.eventAr,
            severity = capAlert.severity,
            descriptionAr = capAlert.descriptionAr,
            safetyInstructionsAr = capAlert.instructionAr,
            source = "تنبيه تجريبي محلي — غير رسمي",
            validUntil = capAlert.expires,
            isOfficial = false,
            isLocalDemo = true
        )

        // Post notification
        NotificationHelper.postSevereWeatherNotification(getApplication(), severeAlert)

        _uiState.update { state ->
            val updatedCustomAlerts = listOf(severeAlert) + state.customAdminAlerts
            val currentWeatherData = state.weatherData
            val updatedWeatherData = currentWeatherData?.let { weather ->
                weather.copy(alerts = listOf(severeAlert) + weather.alerts)
            }
            state.copy(
                customAdminAlerts = updatedCustomAlerts,
                weatherData = updatedWeatherData
            )
        }
    }

    fun publishAudioBulletin(title: String, script: String) {
        repository.saveAdminAudioBulletin(title, script)
        val updatedBulletin = repository.getAudioBulletin()
        _uiState.update { it.copy(audioBulletin = updatedBulletin) }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        if (query.trim().isEmpty()) {
            _uiState.update { it.copy(searchResults = emptyList(), isLoadingSearch = false) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(400) // Debounce
            _uiState.update { it.copy(isLoadingSearch = true) }
            val results = repository.searchCities(query)
            _uiState.update { it.copy(searchResults = results, isLoadingSearch = false) }
        }
    }

    fun toggleFavorite(city: CityLocation) {
        val isFav = repository.toggleFavoriteCity(city)
        val updatedFavorites = repository.getSavedFavoriteCities()
        _uiState.update { state ->
            val updatedSearchResults = state.searchResults.map {
                if (CityFavoriteMatching.matches(
                        savedName = it.name,
                        savedCountry = it.country,
                        queryName = city.name,
                        queryCountry = city.country
                    )
                ) it.copy(isFavorite = isFav) else it
            }
            state.copy(
                favoriteCities = updatedFavorites,
                searchResults = updatedSearchResults,
                selectedCity = if (CityFavoriteMatching.matches(
                        savedName = state.selectedCity.name,
                        savedCountry = state.selectedCity.country,
                        queryName = city.name,
                        queryCountry = city.country
                    )
                ) {
                    state.selectedCity.copy(isFavorite = isFav)
                } else state.selectedCity
            )
        }
    }

    fun onGpsLocationReceived(latitude: Double, longitude: Double, cityName: String = "موقعي الحالي") {
        val location = CityLocation(
            name = cityName,
            nameEn = "Current Location",
            country = "سوريا",
            latitude = latitude,
            longitude = longitude,
            isFavorite = false
        )
        loadWeather(location)
    }

    fun openGoogleSignInModal() {
        _uiState.update { it.copy(isGoogleSignInModalOpen = true) }
    }

    fun closeGoogleSignInModal() {
        _uiState.update { it.copy(isGoogleSignInModalOpen = false) }
    }

    fun sendGeminiChatMessage(userText: String) {
        val cleanText = userText.trim()
        if (cleanText.isEmpty()) return

        val userMsg = ChatMessageItem(
            id = System.currentTimeMillis().toString(),
            sender = "user",
            text = cleanText,
            timestamp = "الآن"
        )
        _uiState.update { it.copy(chatMessages = it.chatMessages + userMsg, isGeneratingAiChat = true) }

        viewModelScope.launch {
            delay(250)
            val state = _uiState.value
            val weather = state.weatherData
            val current = weather?.current
            val airQuality = weather?.airQuality
            val pollen = weather?.pollen
            val cityName = state.selectedCity.name
            val rainProbability = weather?.hourly
                ?.filter { it.precipitationProbabilityAvailable }
                ?.maxOfOrNull { it.precipitationProb }

            val response = if (current == null) {
                "لا تتوفر بيانات طقس محمّلة حالياً لمدينة $cityName. حدّث بيانات الطقس أولاً ثم أعد السؤال."
            } else {
                when {
                    cleanText.contains("مطر") || cleanText.contains("أمطار") || cleanText.contains("هطول") ->
                        if (rainProbability != null) {
                            "في $cityName، أعلى احتمال هطول ضمن الساعات المتاحة في التوقعات هو $rainProbability%. هذه قراءة توقعية وليست رصداً مطرياً."
                        } else {
                            "بيانات احتمال الهطول للساعات القادمة غير متاحة حالياً لمدينة $cityName."
                        }

                    cleanText.contains("رياح") || cleanText.contains("رطوبة") ->
                        "في $cityName، سرعة الرياح الحالية ${current.windSpeedKmh.toInt()} كم/س، والرطوبة النسبية ${current.humidity}%. القيم من بيانات الطقس الحالية المحمّلة."

                    cleanText.contains("جودة الهواء") || cleanText.contains("تلوث") || cleanText.contains("هواء") ->
                        if (airQuality?.isAqiAvailable == true) {
                            "مؤشر جودة الهواء الأوروبي في $cityName هو ${airQuality.aqiValue} (${airQuality.categoryAr})."
                        } else {
                            "لا تتوفر قراءة موثوقة لمؤشر جودة الهواء حالياً في $cityName."
                        }

                    cleanText.contains("حبوب") || cleanText.contains("حساسية") ->
                        if (pollen?.isAvailable == true) {
                            pollen.adviceAr
                        } else {
                            "بيانات حبوب اللقاح غير متاحة حالياً لهذا الموقع أو الوقت."
                        }

                    cleanText.contains("زراعة") || cleanText.contains("ري") ->
                        "البيانات الحالية لا تتضمن رطوبة التربة أو التبخر-نتح أو احتياجات المحاصيل؛ لذلك لا يمكن إصدار توصية ري موثوقة اعتماداً على الطقس وحده."

                    cleanText.contains("ملابس") || cleanText.contains("ماذا ألبس") ->
                        when {
                            current.temperatureCelsius <= 10.0 -> "الحرارة الحالية في $cityName نحو ${current.temperatureCelsius.toInt()}°م؛ قد تكون طبقات الملابس الدافئة مناسبة."
                            current.temperatureCelsius >= 30.0 -> "الحرارة الحالية في $cityName نحو ${current.temperatureCelsius.toInt()}°م؛ اختر ملابس مريحة وخفيفة، وقلّل التعرض للشمس وقت الذروة."
                            else -> "الحرارة الحالية في $cityName نحو ${current.temperatureCelsius.toInt()}°م والجو ${current.conditionNameAr}؛ اختر ملابس مناسبة للطقس المتغير خلال اليوم."
                        }

                    else ->
                        "في $cityName، درجة الحرارة الحالية ${current.temperatureCelsius.toInt()}°م، والحالة ${current.conditionNameAr}، والرياح ${current.windSpeedKmh.toInt()} كم/س. هذا رد محلي تجريبي مبني على البيانات المتاحة، وليس إجابة من Gemini."
                }
            }

            val assistantMsg = ChatMessageItem(
                id = (System.currentTimeMillis() + 1).toString(),
                sender = "demo",
                text = response,
                timestamp = "الآن"
            )
            _uiState.update {
                it.copy(
                    chatMessages = it.chatMessages + assistantMsg,
                    isGeneratingAiChat = false,
                    lastAiVoiceResponse = response
                )
            }
        }
    }

    fun handleVoiceInput(speechText: String) {
        _uiState.update {
            it.copy(lastVoiceQuery = speechText, isVoiceListening = false, isVoiceSpeaking = false)
        }
        sendGeminiChatMessage(speechText)
    }

    fun setVoiceListening(listening: Boolean) {
        _uiState.update { it.copy(isVoiceListening = listening) }
    }

    fun stopVoiceSpeaking() {
        _uiState.update { it.copy(isVoiceSpeaking = false) }
    }
}
