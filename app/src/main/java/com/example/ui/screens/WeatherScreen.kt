package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.WeatherConditionType
import com.example.ui.components.*
import com.example.ui.viewmodel.WeatherViewModel
import com.google.android.gms.location.LocationServices
import java.util.Locale

@SuppressLint("MissingPermission")
@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Location Permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchGpsLocation(context, viewModel)
        }
    }

    // Notification Permission launcher (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.toggleAlertsNotification(isGranted)
    }

    val requestGpsAction = {
        val hasFine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            fetchGpsLocation(context, viewModel)
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val weather = uiState.weatherData
    val condition = weather?.current?.conditionType ?: WeatherConditionType.CLEAR
    val isDay = weather?.current?.isDay ?: true

    DynamicWeatherBackground(
        condition = condition,
        isDay = isDay,
        modifier = modifier
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                WeatherTopBar(
                    cityName = uiState.selectedCity.name,
                    isFavorite = uiState.selectedCity.isFavorite,
                    isFahrenheit = uiState.isFahrenheit,
                    isLoading = uiState.isLoading,
                    onSearchClick = { viewModel.openSearchSheet() },
                    onCustomizeClick = { viewModel.openCustomizeSheet() },
                    onAdminClick = { viewModel.openAdminDashboard() },
                    onGpsClick = { requestGpsAction() },
                    onUnitToggle = { viewModel.toggleUnit() },
                    onRefresh = { viewModel.refreshWeather() },
                    onToggleFavorite = { viewModel.toggleFavorite(uiState.selectedCity) },
                    onMapClick = { viewModel.openLeafletMapScreen() },
                    onGoogleSignInClick = { viewModel.openGoogleSignInModal() },
                    userEmail = uiState.userGoogleEmail
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (uiState.isLoading && weather == null) {
                    // Initial Loading Screen
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AnimatedWeatherIcon(
                            condition = WeatherConditionType.CLEAR,
                            isDay = true,
                            size = 110.dp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        CircularProgressIndicator(
                            color = Color(0xFFFFD54F),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "جاري جلب تنبؤات المركز الوطني (MCNC)...",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                } else if (uiState.errorMessage != null && weather == null) {
                    // Error state with retry
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = Color(0xFFFFCC80),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = uiState.errorMessage ?: "حدث خطأ في الاتصال",
                            fontSize = 16.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { viewModel.refreshWeather() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("retry_button")
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "إعادة المحاولة", fontWeight = FontWeight.Bold)
                        }
                    }
                } else if (weather != null) {
                    // Full Weather Content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 18.dp)
                            .padding(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        // Favorite Cities Chips (Syrian Governorates)
                        FavoriteCitiesBar(
                            cities = uiState.favoriteCities,
                            selectedCity = uiState.selectedCity,
                            onCitySelected = { viewModel.loadWeather(it) },
                            onAddCityClick = { viewModel.openSearchSheet() }
                        )

                        // Syrian Meteorological Stations Live Cards
                        SyrianStationsSection(
                            stations = uiState.syrianStations,
                            onStationSelected = { st ->
                                viewModel.loadWeather(
                                    com.example.data.model.CityLocation(
                                        name = st.nameAr.replace("محطة ", ""),
                                        country = "سوريا",
                                        latitude = st.latitude,
                                        longitude = st.longitude
                                    )
                                )
                            }
                        )

                        // Severe Weather Alerts Banner (CAP Alerts)
                        if (weather.alerts.isNotEmpty()) {
                            SevereWeatherAlertBanner(
                                alerts = weather.alerts,
                                onTestNotification = { alert ->
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        viewModel.testPushNotification(alert)
                                    }
                                }
                            )
                        }

                        // Main Hero Section (Large Animated Icon, Temperature, Condition)
                        MainWeatherHero(
                            weather = weather,
                            isFahrenheit = uiState.isFahrenheit
                        )

                        // Audio Bulletin Reader Section
                        AudioBulletinSection(bulletin = uiState.audioBulletin)

                        // Smart Insights & Advice Card
                        SmartAdviceCard(insights = weather.insights)

                        // 24-Hour Forecast
                        HourlyForecastSection(
                            hourlyList = weather.hourly,
                            isFahrenheit = uiState.isFahrenheit
                        )

                        // 7-Day Forecast
                        DailyForecastSection(
                            dailyList = weather.daily,
                            isFahrenheit = uiState.isFahrenheit
                        )

                        // Interactive Weather Maps (Precipitation, Temperature, Wind, Clouds)
                        InteractiveWeatherMap(
                            currentCity = weather.location,
                            onCitySelected = { viewModel.loadWeather(it) }
                        )

                        // Customizable Forecast Metrics (UV, Air Quality, Pollen, Wind, Humidity, Pressure, Visibility)
                        CustomMetricsSection(
                            preferences = uiState.metricPreferences,
                            current = weather.current,
                            todayForecast = weather.daily.firstOrNull(),
                            airQuality = weather.airQuality,
                            pollen = weather.pollen,
                            onCustomizeClick = { viewModel.openCustomizeSheet() }
                        )

                        // Gemini AI Voice Conversation Section
                        GeminiVoiceConversationSection(
                            weatherData = weather,
                            isListening = uiState.isVoiceListening,
                            isSpeaking = uiState.isVoiceSpeaking,
                            lastVoiceQuery = uiState.lastVoiceQuery,
                            lastAiVoiceResponse = uiState.lastAiVoiceResponse,
                            onStartListeningClick = {
                                viewModel.setVoiceListening(!uiState.isVoiceListening)
                            },
                            onStopVoiceClick = {
                                viewModel.stopVoiceSpeaking()
                            }
                        )

                        // Gemini AI Weather Chat Section
                        GeminiChatSection(
                            weatherData = weather,
                            messages = uiState.chatMessages,
                            isGenerating = uiState.isGeneratingAiChat,
                            onSendMessage = { text ->
                                viewModel.sendGeminiChatMessage(text)
                            },
                            onConnectApiClick = {
                                viewModel.openGoogleSignInModal()
                            }
                        )

                        // Simulated Precipitation Radar
                        InteractiveRadarView(cityName = weather.location.name)
                    }
                }

                // Google Sign In & Gemini API Link Modal Sheet
                GoogleSignInModal(
                    isOpen = uiState.isGoogleSignInModalOpen,
                    onDismiss = { viewModel.closeGoogleSignInModal() },
                    userEmail = uiState.userGoogleEmail,
                    apiKeyInput = uiState.geminiApiKey,
                    onApiKeyChange = { viewModel.setGeminiApiKey(it) },
                    onGoogleSignInClick = { viewModel.simulateGoogleSignIn() },
                    onSaveApiKeyClick = { viewModel.closeGoogleSignInModal() }
                )

                // City Search Bottom Sheet Modal
                CitySearchSheet(
                    isOpen = uiState.isSearchSheetOpen,
                    onDismiss = { viewModel.closeSearchSheet() },
                    searchQuery = uiState.searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChanged(it) },
                    searchResults = uiState.searchResults,
                    isLoadingSearch = uiState.isLoadingSearch,
                    onCitySelected = { viewModel.loadWeather(it) },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onRequestCurrentLocation = { requestGpsAction() }
                )

                // Customizable Metrics & Alert Settings Sheet
                CustomizeMetricsSheet(
                    isOpen = uiState.isCustomizeSheetOpen,
                    onDismiss = { viewModel.closeCustomizeSheet() },
                    currentPreferences = uiState.metricPreferences,
                    onSavePreferences = { viewModel.saveMetricPreferences(it) },
                    alertsNotificationEnabled = uiState.areAlertsEnabled,
                    onToggleAlertsNotification = { enabled ->
                        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.toggleAlertsNotification(enabled)
                        }
                    }
                )

                // MCNC Admin Dashboard Sheet
                AdminDashboardScreen(
                    isOpen = uiState.isAdminDashboardOpen,
                    onDismiss = { viewModel.closeAdminDashboard() },
                    stations = uiState.syrianStations,
                    activeAlerts = weather?.alerts ?: emptyList(),
                    onBroadcastCapAlert = { capAlert ->
                        viewModel.broadcastCapAlert(capAlert)
                    },
                    onPublishAudioBulletin = { title, script ->
                        viewModel.publishAudioBulletin(title, script)
                    }
                )

                // Full Screen Leaflet Weather Map Screen Overlay
                if (uiState.isLeafletMapScreenOpen) {
                    LeafletWeatherMapScreen(
                        currentCity = uiState.selectedCity,
                        onCitySelected = { viewModel.loadWeather(it) },
                        onBackClick = { viewModel.closeLeafletMapScreen() }
                    )
                }
            }
        }
    }
}

@Composable
fun WeatherTopBar(
    cityName: String,
    isFavorite: Boolean,
    isFahrenheit: Boolean,
    isLoading: Boolean,
    onSearchClick: () -> Unit,
    onCustomizeClick: () -> Unit,
    onAdminClick: () -> Unit,
    onGpsClick: () -> Unit,
    onUnitToggle: () -> Unit,
    onRefresh: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMapClick: () -> Unit,
    onGoogleSignInClick: () -> Unit,
    userEmail: String? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "refreshAnim")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // City Name & Search Trigger
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .clickable { onSearchClick() }
                .padding(vertical = 4.dp, horizontal = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = Color(0xFFFFD54F),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = cityName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "تغيير المدينة",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = "MCNC Weather",
                    fontSize = 11.sp,
                    color = Color(0xFF38BDF8),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Action Buttons Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Google Account / API Button
            Box(
                modifier = Modifier
                    .height(38.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(if (userEmail != null) Color(0xFF166534) else Color(0xFF4285F4))
                    .clickable { onGoogleSignInClick() }
                    .padding(horizontal = 10.dp)
                    .testTag("google_signin_topbar_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (userEmail != null) Icons.Default.AccountCircle else Icons.Default.Login,
                        contentDescription = "حساب Google",
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (userEmail != null) "جوجل متصل" else "دخول Google",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            // Leaflet Map Screen Button
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0284C7).copy(alpha = 0.85f))
                    .clickable { onMapClick() }
                    .testTag("leaflet_map_topbar_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Public,
                    contentDescription = "فتح خريطة Leaflet",
                    tint = Color.White,
                    modifier = Modifier.size(19.dp)
                )
            }

            // GPS Location Button
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f))
                    .clickable { onGpsClick() }
                    .testTag("gps_location_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "تحديد موقعي GPS",
                    tint = Color.White,
                    modifier = Modifier.size(19.dp)
                )
            }

            // Admin Panel Button
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEA580C).copy(alpha = 0.8f))
                    .clickable { onAdminClick() }
                    .testTag("admin_panel_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = "لوحة الإدارة",
                    tint = Color.White,
                    modifier = Modifier.size(19.dp)
                )
            }

            // Customize Dashboard Button
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f))
                    .clickable { onCustomizeClick() }
                    .testTag("top_bar_customize_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "تخصيص البيانات",
                    tint = Color(0xFF67E8F9),
                    modifier = Modifier.size(19.dp)
                )
            }

            // Favorite Button
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f))
                    .clickable { onToggleFavorite() }
                    .testTag("favorite_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "المفضلة",
                    tint = if (isFavorite) Color(0xFFFFD54F) else Color.White,
                    modifier = Modifier.size(19.dp)
                )
            }

            // Unit Switcher (°C / °F)
            Box(
                modifier = Modifier
                    .height(38.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(Color.White.copy(alpha = 0.18f))
                    .clickable { onUnitToggle() }
                    .padding(horizontal = 10.dp)
                    .testTag("unit_toggle_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isFahrenheit) "°F" else "°C",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Refresh Button
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f))
                    .clickable { onRefresh() }
                    .testTag("refresh_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "تحديث",
                    tint = Color.White,
                    modifier = Modifier
                        .size(19.dp)
                        .then(if (isLoading) Modifier.rotate(rotation) else Modifier)
                )
            }
        }
    }
}

@Composable
fun FavoriteCitiesBar(
    cities: List<com.example.data.model.CityLocation>,
    selectedCity: com.example.data.model.CityLocation,
    onCitySelected: (com.example.data.model.CityLocation) -> Unit,
    onAddCityClick: () -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(cities) { city ->
            val isSelected = city.name.equals(selectedCity.name, ignoreCase = true)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isSelected) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.16f)
                    )
                    .border(
                        1.dp,
                        if (isSelected) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.25f),
                        RoundedCornerShape(20.dp)
                    )
                    .clickable { onCitySelected(city) }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (city.isFavorite) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (isSelected) Color(0xFF1E293B) else Color(0xFFFFD54F),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = city.name,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color(0xFF1E293B) else Color.White
                    )
                }
            }
        }

        item {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                    .clickable { onAddCityClick() }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "إضافة مدينة",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "المزيد",
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun MainWeatherHero(
    weather: com.example.data.model.FullWeatherData,
    isFahrenheit: Boolean
) {
    val current = weather.current
    val today = weather.daily.firstOrNull()

    val currentTemp = if (isFahrenheit) {
        (current.temperatureCelsius * 9 / 5 + 32).toInt()
    } else {
        current.temperatureCelsius.toInt()
    }
    val feelsLike = if (isFahrenheit) {
        (current.feelsLikeCelsius * 9 / 5 + 32).toInt()
    } else {
        current.feelsLikeCelsius.toInt()
    }
    val maxTemp = if (isFahrenheit) {
        ((today?.maxTempCelsius ?: current.temperatureCelsius) * 9 / 5 + 32).toInt()
    } else {
        (today?.maxTempCelsius ?: current.temperatureCelsius).toInt()
    }
    val minTemp = if (isFahrenheit) {
        ((today?.minTempCelsius ?: current.temperatureCelsius) * 9 / 5 + 32).toInt()
    } else {
        (today?.minTempCelsius ?: current.temperatureCelsius).toInt()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.12f))
                )
            )
            .border(1.5.dp, Color.White.copy(alpha = 0.32f), RoundedCornerShape(28.dp))
            .padding(vertical = 22.dp, horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Interactive Animated Icon (Tap it to trigger spring bounce!)
        AnimatedWeatherIcon(
            condition = current.conditionType,
            isDay = current.isDay,
            size = 125.dp,
            interactiveTapEnabled = true,
            modifier = Modifier.testTag("hero_weather_icon")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Big Modern Temperature
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = "$currentTemp",
                fontSize = 76.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = if (isFahrenheit) "°F" else "°C",
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 14.dp, start = 2.dp)
            )
        }

        // Weather Condition Badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.22f))
                .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(
                text = current.conditionNameAr,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // High / Low & Feels Like Badges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // High / Low
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = Color(0xFFF97316),
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "$maxTemp°",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "$minTemp°",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(14.dp)
                    .background(Color.White.copy(alpha = 0.35f))
            )
            Spacer(modifier = Modifier.width(16.dp))

            // Feels Like
            Text(
                text = "الشعور: $feelsLike°",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
fun SmartAdviceCard(
    insights: com.example.data.model.WeatherInsights,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF0284C7).copy(alpha = 0.35f), Color(0xFF0369A1).copy(alpha = 0.20f))
                )
            )
            .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f), RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF38BDF8)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = Color(0xFF0C4A6E),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = insights.adviceTitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = insights.outdoorActivityScore,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFFFD54F)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = insights.adviceSummary,
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.9f),
            lineHeight = 19.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.18f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Checkroom,
                contentDescription = null,
                tint = Color(0xFF67E8F9),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = insights.clothingAdvice,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@SuppressLint("MissingPermission")
private fun fetchGpsLocation(context: Context, viewModel: WeatherViewModel) {
    try {
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        fusedClient.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                var detectedName = "موقعي الحالي"
                try {
                    val geocoder = Geocoder(context, Locale("ar"))
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        geocoder.getFromLocation(loc.latitude, loc.longitude, 1) { list ->
                            list.firstOrNull()?.let { addr ->
                                val name = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: "موقعي الحالي"
                                viewModel.onGpsLocationReceived(loc.latitude, loc.longitude, name)
                            }
                        }
                        return@addOnSuccessListener
                    } else {
                        @Suppress("DEPRECATION")
                        val list = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                        list?.firstOrNull()?.let { addr ->
                            detectedName = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: "موقعي الحالي"
                        }
                    }
                } catch (e: Exception) {
                    // Ignore geocoder failure, use fallback name
                }
                viewModel.onGpsLocationReceived(loc.latitude, loc.longitude, detectedName)
            }
        }
    } catch (e: Exception) {
        // Fallback
    }
}
