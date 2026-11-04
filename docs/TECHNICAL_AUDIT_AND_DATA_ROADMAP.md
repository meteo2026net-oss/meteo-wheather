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

`WeatherRepository.getSyrianWeatherStations()` constructs the station list with fixed temperature, humidity, pressure, wind and condition values. The records are currently marked available/online in the constructor. These values must not be presented as live observations.

**Required correction:** introduce an explicit station observation source and metadata:
- station identifier and coordinates;
- measured values and units;
- observation timestamp with timezone;
- source/provider and source record identifier where available;
- freshness state (current, stale, unavailable);
- quality/validation flags;
- clear distinction between observation, forecast and demonstration data.

Do not fabricate live values when no station feed is configured. An unavailable state is safer than a plausible-looking invented reading.

### 2. Radar and map visuals are simulations

- `InteractiveRadarView.kt` draws synthetic precipitation echoes and an animated sweep.
- `InteractiveWeatherMap.kt` draws a stylized geographic background and synthetic overlay fields.

These are presentation prototypes, not verified radar or numerical weather-analysis layers. Keep a visible simulation label until a real data provider, timestamp, geographic bounds, projection and legend are implemented.

### 3. Default audio bulletin is static

`WeatherRepository.getAudioBulletin()` supplies a hard-coded default bulletin and display time. The audio component uses Android TextToSpeech to read the text; this is speech synthesis, not a verified recorded official broadcast.

The UI should identify synthesized speech and should display the actual publication/update timestamp supplied by the bulletin source.

### 4. Alerts need provenance and lifecycle

The current ViewModel can prepend locally broadcast admin alerts to forecast alerts. The next stage should model alert origin, identifier, issue time, effective time, expiry, severity, affected area, update/cancel relationship and acknowledgement state. Local test notifications must be labelled as tests and must not be confused with official CAP alerts.

### 5. Missing data must remain missing

Review the repository mapping for fallback values such as zero or dash for air quality and pollen. Missing, unsupported and unavailable values must be represented distinctly; zero is a real measurement and must not be used as a generic fallback.

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

## Verification status

- Source inspection completed for the main repository, Android Gradle configuration, manifest, Open-Meteo API interfaces, repository, ViewModel and key Compose components.
- No Android build was run in this inspection session.
- No external Syrian station feed or operational radar provider was verified.
- No production data was changed by this audit.
