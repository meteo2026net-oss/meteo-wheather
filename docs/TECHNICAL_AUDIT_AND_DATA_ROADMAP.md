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
- Station measurements remain hidden unless a station is explicitly marked as verified, online and non-demo; the verification flag defaults to false and has a regression test.
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
- Unit tests cover provenance, missing values, demo station defaults, unavailable environmental data, local CAP defaults, non-official alerts and demo bulletins.
- Previous validated branch head: `511bf205c566f3365680d48c9e1a42d521a7a7ef` (4 November 2026); run `41760395138` succeeded and uploaded debug artifact `13289999184`.
- Subsequent maintenance commits replaced deprecated `Locale("ar")` construction with `Locale.forLanguageTag("ar")` in the audio bulletin, voice conversation and weather screen; replaced deprecated direction-sensitive Compose icons with `Icons.AutoMirrored.Filled` variants in the admin, chat, voice, map and weather screens; retained explicit Moshi field annotations; removed deprecated WebView database settings; and added Leaflet JavaScript bridge allowlist/coordinate validation tests.
- The maintenance source head before the following documentation-only commit was `eae99196a74191b1f9e92aa05158801c37d98df0`.
- Latest verified branch head: `de0b4ee2286ba0f66dbd99f4efd5924964d18f1d` (4 November 2026). GitHub Actions run `41761465682` completed successfully on that exact commit: the `testDebugUnitTest` and `assembleDebug` steps succeeded, and the debug APK upload succeeded.
- Latest artifact: `mcnc-weather-debug-apk`, artifact ID `13290862035`, size 22,674,409 bytes, SHA-256 ZIP digest `789c8b1cbabc972bfea444b52929806058917c96d8f53eb2f3da5e0879bc189d`, expires 2 February 2027. This is a CI debug build signed with a temporary runner-only debug key, not a production-signed release.
- CI success verifies compilation, JVM unit tests and debug APK packaging for the exact commit above. It does not verify live station ingestion, official alert distribution, provider coverage, production credentials or real-device behavior.
- No external Syrian station feed or operational official radar provider was verified.
- No production data was changed by this audit.

## Verification notes

- CI builds the debug APK and runs JVM unit tests on GitHub Actions. A temporary debug signing key is created in the runner only; it is not the production signing key.
- The existing Robolectric context test uses Android API 35. A machine-specific offline SDK cache is optional and is only enabled through `ROBOLECTRIC_DEPENDENCY_DIR` when that directory exists.
- The map WebViews disallow mixed content, file/content access and automatic window opening. Remote map resources use HTTPS.
- Latest source review: the legacy RainViewer nowcast URL has been replaced by the provider metadata manifest and the latest available historical frame. CI success applies only to the exact validated commit stated above; subsequent branch commits require their own successful verification.

## AQI classification correction (4 November 2026)

- The Open-Meteo European AQI value is now classified using explicit bands: 0–20, 21–40, 41–60, 61–80, 81–100 and above 100.
- Previously, every value above 80 shared one broad label, which obscured the distinction between very poor and extremely poor air quality. The classification is now centralized in `EuropeanAqi` and negative/unavailable values are never labelled as good air.
- Added boundary-focused JVM tests for every band and the unavailable sentinel.
- These labels are descriptive estimates based on the provider's European AQI field; they are not official public-health alerts and do not replace local health guidance.
- The new AQI helper and tests are committed on the development branch; CI verification is pending for the new branch head.

## Air-quality alert wording correction (4 November 2026)

- AQI-triggered advisory wording no longer attributes poor air quality to dust or a dust storm. European AQI is an aggregate air-quality indicator and does not, by itself, identify the pollutant source.
- The advisory now names the European AQI explicitly, avoids claiming an unverified dust cause, and directs users to local health guidance.
- The advisory remains an automated, non-official estimate. It is not an official warning and does not replace public-health advice.
- CI verification is pending for the updated branch head.

- Automated weather-rule alerts no longer invent fixed expiry times such as "until midnight" or "for six hours". Until the source provides a defensible validity interval, the UI states that the alert end time is not available. Official CAP expiry remains a separate field that must come from the authenticated CAP source.


## Wind alert wording correction (4 November 2026)

- The automated wind advisory no longer labels high wind speed alone as a dust storm or claims that dust is occurring.
- The alert now reports the measured/modelled wind speed and explicitly distinguishes the possibility of airborne dust from a confirmed dust event.
- This remains an automated, non-official forecast-rule advisory, not an observation or official warning.
- GitHub Actions run [41763192852](https://github.com/meteo2026net-oss/meteo-wheather/actions/runs/41763192852) completed successfully on the previous head `b72d875f785279c8c2e5dd2214952320eaf5af08`, including `testDebugUnitTest`, `assembleDebug` and artifact upload. The wording follow-up in this section requires its own CI run.


## Non-finite measurement guard (4 November 2026)

- `QualifiedWeatherValue.isDisplayable` now rejects non-finite numeric values (`NaN`, positive infinity and negative infinity), in addition to null and explicitly unavailable values.
- A measured zero remains displayable; missing and invalid numeric values are not converted into plausible readings.
- Added a regression test covering all three non-finite cases. The latest CI run for the previous branch head is still being monitored; this new change requires its own successful CI run.


## Current UV availability and forecast UI refresh fixes (4 November 2026)

- The current-weather UV DTO field is nullable instead of defaulting to zero when the provider omits it.
- The repository now records current UV availability separately; the custom UV metric displays an unavailable state rather than classifying a missing value as a low UV reading.
- Added a regression test that confirms an omitted current UV field remains null in the DTO.
- Hourly forecast selection resets after list refresh, and a daily expanded row is cleared if its index no longer exists.
- Daily temperature-range bars now position themselves relative to their actual available width rather than a fixed 100 dp offset.
- CI verification is required for the latest source head; these changes are not yet marked as build-verified.
