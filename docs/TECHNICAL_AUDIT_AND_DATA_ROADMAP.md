# MCNC Weather — Technical Audit and Data Quality Roadmap

> Audit baseline: `main` at commit `4d4e1b4a4859cf99bdd5cd21f701cc09d2cc2756`.
> This document records code-level observations and a safe implementation order. It does not claim that the live service endpoints or station feeds have been verified operationally.

## Confirmed project structure

- Native Android application using Kotlin and Jetpack Compose.
- Forecast and geocoding clients use Retrofit/Moshi with Open-Meteo endpoints.
- Air-quality client uses the Open-Meteo Air Quality API.
- The app includes GPS location, city search, hourly/daily forecasts, alert UI, an audio bulletin reader, Syrian station cards, a weather map, and a radar-style view.
- Firebase BOM, Firebase AI and App Check dependencies are present. Firestore and Firebase Authentication dependencies are currently commented out in `app/build.gradle.kts`.
- The application requests internet, network-state, coarse/fine location and notification permissions.

## Data integrity findings

### 1. Syrian station readings are hard-coded

`WeatherRepository.getSyrianWeatherStations()` constructs the station list with fixed temperature, humidity, pressure, wind and condition values. The records have been changed to offline/demo by default. Their numeric values remain in the legacy model for compatibility but are hidden in the station cards and admin view. These values must not be presented as live observations.

**Required correction:** introduce an explicit station observation source and metadata:
- station identifier and coordinates;
- measured values and units;
- observation timestamp with timezone;
- source/provider and source record identifier where available;
- freshness state (current, stale, unavailable);
- quality/validation flags;
- clear distinction between observation, forecast and demonstration data.

Do not fabricate live values when no station feed is configured. An unavailable state is safer than a plausible-looking invented reading.

### 2. Radar and map data

- The map screens use RainViewer's public Weather Maps API metadata endpoint and select the newest frame in `radar.past`.
- RainViewer discontinued its public nowcast/future frames on 1 January 2026. The app no longer hard-codes the retired `nowcast_latest` tile path.
- The selected frame timestamp is displayed on the map. Tile zoom is capped at 7 according to the provider's current public API documentation.
- The provider does not guarantee coverage or availability; an empty tile area must not be interpreted as no precipitation.
- Temperature, wind and cloud map overlays remain unavailable; city pins intentionally show dashes rather than invented values.
- The RainViewer layer is a third-party composite, not an official MCNC radar product.

Keep source attribution and the historical-frame label visible. Do not call the latest historical frame "live" or "nowcast".

### 3. Default audio bulletin is static

`WeatherRepository.getAudioBulletin()` supplies a hard-coded default bulletin and display time. The audio component uses Android TextToSpeech to read the text; this is speech synthesis, not a verified recorded official broadcast.

The UI identifies synthesized speech; a verified official recording and publication timestamp are not configured.

### 4. Alerts need provenance and lifecycle

The current ViewModel can prepend locally broadcast admin alerts to forecast alerts. The next stage should model alert origin, identifier, issue time, effective time, expiry, severity, affected area, update/cancel relationship and acknowledgement state. The admin sheet now warns that its actions are local-only and not authenticated or centrally distributed. Button labels were changed to describe local demonstration actions. Local test notifications must not be confused with official CAP alerts.

### 5. Missing data must remain missing

Air-quality and pollen fields now have per-field availability flags; partial pollen input is not converted into an overall severity category. Hourly and daily forecast fields also carry availability flags, and missing precipitation/UV/sunrise/sunset values are shown as unavailable. Hourly selection now uses the provider's local current-time field rather than the device timezone. Missing, unsupported and unavailable values must remain distinct; zero is a real measurement.

## Implementation sequence

1. Add typed source/quality/freshness metadata to station and alert domain models without removing existing UI capabilities.
2. Replace hard-coded station readings with an unavailable/demo state until an approved feed is configured; retain station identity and coordinates.
3. Add repository tests for missing values, stale observations, unit conversion and alert expiry.
4. Add a provider adapter for the actual station feed once its endpoint and authentication contract are known.
5. Replace simulated map/radar layers only after selecting a provider with documented coverage, timestamps, attribution and permitted use.
6. Connect central administration and official CAP ingestion to an authenticated backend; local-only publishing is not a multi-user broadcast service.
7. Add CI build and test workflow, then validate a debug build before release.

## Configuration and security notes

- Do not commit API keys, Firebase service-account files, signing keys or real device tokens.
- Keep the existing AI Studio signing configuration intact.
- Firebase Firestore/Auth dependencies are commented out; do not assume a secure central admin backend already exists.
- Confirm release secrets and Firebase configuration in the deployment environment rather than storing credentials in source control.

## Changes implemented on the development branch

- Syrian station cards and their domain model now explicitly identify the existing fixed station values as demonstration data.
- Air-quality and pollen models now carry availability flags. The repository no longer substitutes plausible-looking AQI, pollutant or pollen readings when the provider omits them; the UI displays an unavailable state instead.
- AQI-based alert detection is skipped when no AQI reading exists.
- Visibility is no longer replaced with a fabricated 10 km reading when the hourly provider field is absent; the UI displays an unavailable state.
- Daily forecast entries without both maximum and minimum temperatures are skipped rather than filled with fixed 25°C/18°C values.
- Hourly and daily missing fields are represented by explicit availability flags; missing forecast weather codes no longer inherit a current/clear code.
- The map and radar UI explicitly identify synthetic layers as demonstrations; the radar warning states that it is not displaying actual radar measurements.
- The admin sheet warns that it is local-only, unauthenticated and not a central CAP broadcast service; its buttons no longer claim to notify all users.
- Unverified Syrian station models now default to offline/demo, and synthetic numeric readings are hidden in both the main station cards and admin panel.
- Added an Android GitHub Actions workflow for unit tests and debug APK assembly. The repository has no Gradle wrapper scripts/JAR, so CI provisions Gradle 9.3.1 directly.
- A missing current-weather object now produces a clear load failure rather than constructing a default all-zero weather record.

## Verification status

- Source inspection completed for the main repository, Android Gradle configuration, manifest, Open-Meteo API interfaces, repository, ViewModel and key Compose components.
- Source files were fetched again from the development branch after each change and the targeted fallback patterns were checked.
- Unit tests were added for provenance, missing values, demo station defaults and unavailable environmental data.
- GitHub Actions CI successfully completed `testDebugUnitTest` and `assembleDebug` on 4 November 2026 (run `41753593868`, commit `e7d0d4dfa9f2fbc1fae5fdc8b7ae70dbc86915d9`). The debug APK was uploaded as artifact `mcnc-weather-debug-apk` (22,670,955 bytes; artifact ID `13287204164`; expires 2 February 2027). The runner used a temporary debug keystore only; it is not a production-signed release APK.
- No external Syrian station feed or operational radar provider was verified.
- No production data was changed by this audit.


## Verification notes

- CI builds the debug APK and runs JVM unit tests on GitHub Actions. A temporary debug signing key is created in the runner only; it is not the production signing key.
- The existing Robolectric context test uses Android API 35. A machine-specific offline SDK cache is optional and is only enabled through `ROBOLECTRIC_DEPENDENCY_DIR` when that directory exists.
- The map WebViews disallow mixed content, file/content access and automatic window opening. Remote map resources use HTTPS.
- Latest source review: the legacy RainViewer nowcast URL has been replaced by the provider metadata manifest and the latest available historical frame. The current development-branch head passed the CI build and unit-test workflow; review any later branch commits independently before merge.
