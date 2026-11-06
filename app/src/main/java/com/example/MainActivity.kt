package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.ui.screens.WeatherScreen
import com.example.ui.viewmodel.WeatherViewModel

class MainActivity : ComponentActivity() {

    private val weatherViewModel: WeatherViewModel by viewModels()

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            // Location denial must not block the rest of the weather app.
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestInitialLocationPermission()

        setContent {
            // Weather App Theme with dark glassmorphic styling
            val weatherColorScheme = darkColorScheme(
                primary = Color(0xFF38BDF8),
                secondary = Color(0xFFFFD54F),
                tertiary = Color(0xFF67E8F9),
                background = Color(0xFF0F172A),
                surface = Color(0xFF1E293B),
                onPrimary = Color(0xFF0F172A),
                onSecondary = Color(0xFF0F172A),
                onBackground = Color.White,
                onSurface = Color.White
            )

            MaterialTheme(colorScheme = weatherColorScheme) {
                // Arabic RTL Layout direction provider
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        WeatherScreen(
                            viewModel = weatherViewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
    private fun requestInitialLocationPermission() {
        val fineGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!fineGranted && !coarseGranted) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }
}
