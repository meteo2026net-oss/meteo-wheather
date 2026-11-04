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
    val syrianStations: List<SyrianWeatherStation> = emptyList(),
    val audioBulletin: AudioBulletinItem = AudioBulletinItem("1", "النشرة الجوية الصباحية", "اليوم", 60, "رئيس التنبؤات (MCNC)", "جاري التحميل..."),
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
                if (it.name.equals(city.name, ignoreCase = true)) it.copy(isFavorite = isFav) else it
            }
            state.copy(
                favoriteCities = updatedFavorites,
                searchResults = updatedSearchResults,
                selectedCity = if (state.selectedCity.name.equals(city.name, ignoreCase = true)) {
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
}
