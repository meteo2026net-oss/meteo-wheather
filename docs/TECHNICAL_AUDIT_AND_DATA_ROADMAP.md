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

## Missing current-observation fields are no longer fabricated (4 November 2026)

- Every field in `CurrentWeatherDto` is nullable when absent from the Open-Meteo response; omitted values no longer become synthetic defaults such as 0 °C, 0% humidity, calm wind or 1013.25 hPa.
- Before constructing the current-weather domain object, the repository validates the required current measurements for presence, finiteness and basic physical ranges. An incomplete current observation now returns a clear failure instead of presenting invented values as measured weather.
- Optional current UV remains nullable and is exposed through its availability flag.
- Added Moshi parsing regression tests for a partially populated current-weather JSON object and an empty object.
- GitHub Actions run [41768313852](https://github.com/meteo2026net-oss/meteo-wheather/actions/runs/41768313852) completed successfully on commit `c099cb8867dd4da459168c4ef4a582ab0da2e484`, including unit tests, debug APK assembly and artifact upload.
- This check verifies the source and tests on that commit; it does not establish live provider uptime, station observations, official alerts or production-release readiness.


## Hourly and daily measurement range validation (4 November 2026)

- Forecast precipitation probability and relative humidity are accepted only within 0–100%.
- Hourly wind speed and visibility reject negative or non-finite values; current and daily UV reject negative or non-finite values.
- Daily maximum/minimum temperatures are included only when both are finite and maximum is not below minimum.
- Invalid optional values remain unavailable through the existing per-field availability flags.
- Added JVM tests for percentage boundaries, non-finite/negative measurements and reversed daily temperature ranges.
- CI verification is pending for this source change; no live provider or station feed is implied.


## Environmental measurement range validation (4 November 2026)

- Air-quality pollutant concentrations (PM2.5, PM10, ozone and NO₂) are considered available only when finite and non-negative.
- European AQI is considered available only when the provider value is non-negative; missing or negative sentinel-like values do not trigger an AQI advisory.
- Grass, birch, olive and ragweed pollen readings reject negative and non-finite values before completeness checks and category calculation.
- Availability flags now reflect validated values rather than merely checking whether the JSON field was present.
- Added JVM regression tests for negative, non-finite and zero environmental measurements.
- CI verification is pending for this commit. Values and thresholds remain subject to provider documentation and units; this validation does not certify medical or official warning categories.

## Map provider attribution and radar frame validation (4 November 2026)

- Base-map tile configuration is shared across the interactive map, full-screen map and city radar views. Each selected provider now displays its own attribution and supported zoom limit: CARTO, OpenStreetMap or Esri.
- Switching base-map styles in the full-screen map updates attribution and subdomain configuration together with the tile URL.
- The RainViewer metadata response is treated as untrusted input: only HTTPS hosts under `rainviewer.com` are accepted, radar paths must match the expected versioned radar path format, and frame timestamps must be finite, positive and not materially in the future.
- The newest valid frame is selected by timestamp rather than assuming the provider array is ordered.
- Frames older than two hours remain visibly marked as stale; unavailable metadata or invalid frames produce an explicit unavailable status and must not be interpreted as evidence of dry weather.
- The app continues to expose only the historical precipitation radar frame. Temperature, wind and cloud overlays remain unavailable until connected to verified data sources.
- CI verification for these latest map and radar changes is pending. This does not establish radar coverage quality, provider uptime or production readiness.

## Historical radar timeline controls (4 November 2026)

- Added a compact Arabic timeline to the interactive weather map, full-screen Leaflet map and city radar panel.
- Valid historical RainViewer frames are sorted chronologically. The newest valid frame is selected initially.
- Users can move one frame backward or forward, scrub the frame slider, and play/pause the historical sequence.
- The timeline is shown only when at least two valid historical frames exist; the full-screen map hides it when the precipitation overlay is not selected.
- Playback advances through provider-supplied historical frames at a visual interval of 900 ms. This is a display animation, not a precipitation forecast or a promise of fixed observation intervals.
- RainViewer tiles remain limited to zoom level 7. Coverage over Syria and actual frame availability must be checked at runtime.
- Android CI for this commit is pending; no successful build or device validation is claimed yet.

## Latest Android CI verification (4 November 2026)

- Commit `82e4a58ea04a5707def4783a1ca45273703d392e` completed GitHub Actions run [41773062187](https://github.com/meteo2026net-oss/meteo-wheather/actions/runs/41773062187) successfully.
- The `testDebugUnitTest` and `assembleDebug` work completed successfully, and the debug APK artifact upload also succeeded.
- Artifact: `mcnc-weather-debug-apk`, ID `13295752530`, size 22,693,940 bytes, SHA-256 ZIP digest `30136f3e224bb2feccf7750fbcac4daec50e07e7bc32b0c0251df1182203d66b`, expires 2 February 2027.
- The preceding CI failure on commit `47bb89c085dbc098f8b12d51a66d364f893ef72c` was a Kotlin compilation error caused by references to XMLConstants property-name constants unavailable in the Android compile environment. The parser now uses the equivalent standard property URI strings; its existing fail-closed XML features and entity resolver remain in place.
- This artifact is a CI debug build signed with an ephemeral runner-only debug key. It is not a production-signed release and does not establish real-device, live-feed, official-station or operational-alert readiness.


## Latest CI verification — 4 November 2026

- Current verified development-branch head: `7910f0df453a1226cedb106e3f145cbd87831de5`.
- GitHub Actions run [41777064307](https://github.com/meteo2026net-oss/meteo-wheather/actions/runs/41777064307) completed successfully on that exact commit.
- The job completed both `testDebugUnitTest` and `assembleDebug`, then uploaded the `mcnc-weather-debug-apk` artifact (artifact ID `13296873929`, 22,706,308 bytes).
- Artifact ZIP SHA-256: `f348ca81491eb79c822f84e3ac798e89d3920534a20168abafa85eed85a784bf`. The artifact expires on 2 February 2027.
- Commit `7910f0d` adds an `AndroidView.onRelease` cleanup path for the Leaflet WebView: stop loading, remove the JavaScript bridge, clear the WebChromeClient, remove child views and destroy the WebView.
- This is a CI debug build signed with a temporary runner-only key. It is not a production release or a release-signing validation.
- PR #1 remains open and unmerged; the `main` branch was not modified by this verification.

## Final verification update — 4 November 2026

This section supersedes earlier "pending CI" statements in the chronological notes above for the exact branch head listed here.

- Verified PR head: `500de0dde407e5582ee06cf8c07816e1f73cc8ad`.
- GitHub Actions run [41841431321](https://github.com/meteo2026net-oss/meteo-wheather/actions/runs/41841431321) completed successfully.
- Unit-test report: **53 tests, 0 failures, 0 skipped**.
- CAP XML parser suite: **10 tests, 0 failures**, including exact CAP namespace enforcement, foreign-namespace spoofing rejection, expiry/current-time chronology and XML external-entity protections.
- Robolectric context test: **1 test, 0 failures**. The transient HTTP artifact-fetch failure seen in the preceding run did not recur.
- Debug APK assembly and both artifact uploads completed successfully.
- Debug APK artifact: `mcnc-weather-debug-apk`, ID `13328967533`, 22,709,098 bytes; expires 2 February 2027.
- Unit-test report artifact: `android-unit-test-reports`, ID `13329007588`, 17,948 bytes; expires 18 November 2026.
- These artifacts are CI outputs. The APK is signed with an ephemeral runner-only Debug key, not the production Release signing identity.
- This verification establishes JVM unit-test success, Android Debug compilation and APK packaging for this exact head. It does not establish official Syrian station ingestion, authenticated CAP feed connectivity, real radar coverage, production credentials, release signing or physical-device behavior.
- PR #1 remains open and unmerged. The development branch is 163 commits ahead of `main` and 0 commits behind it. No changes were made to `main`.


## Latest Android CI verification — 4 November 2026 (documentation follow-up head)

- Commit `ced0a1c4378d4619f363d20fcd34d907c5fbda2d` completed GitHub Actions run [41850527272](https://github.com/meteo2026net-oss/meteo-wheather/actions/runs/41850527272) successfully.
- `testDebugUnitTest` and `assembleDebug` both succeeded; the Debug APK and unit-test report uploads succeeded.
- Debug APK artifact: `mcnc-weather-debug-apk`, ID `13335666084`, 22,709,103 bytes; expires 2 February 2027. Artifact ZIP SHA-256: `230b25b4016b6487d7381c44bf0f331d34bb96b209c7bd698ed072cc7312aeac`.
- Unit-test report artifact: `android-unit-test-reports`, ID `13335471187`, 17,934 bytes; expires 18 November 2026.
- The APK uses an ephemeral runner-only Debug key and is not a production Release build.
- This exact-head CI result verifies JVM unit tests, Android Debug compilation and APK packaging only. It does not establish live station ingestion, CAP RSS availability or authenticity, official alert distribution, map provider coverage, release signing or physical-device behavior.


## Latest Android CI verification — 4 November 2026 (verified current branch head)

- Commit `e166108a66fc2216e0d59fbbbd179e321beb9c4d` completed GitHub Actions run [41852741361](https://github.com/meteo2026net-oss/meteo-wheather/actions/runs/41852741361) successfully.
- The `testDebugUnitTest` and `assembleDebug` step completed successfully; both the Debug APK and unit-test report artifact uploads succeeded.
- Debug APK artifact: `mcnc-weather-debug-apk`, ID `13336213838`, 22,709,099 bytes; expires 2 February 2027. Artifact ZIP SHA-256: `7c7b60c499a32eaf351711393bbf766fa5181a51a5e0da7c066729a34b48baba`.
- Unit-test report artifact: `android-unit-test-reports`, ID `13335728905`, 17,930 bytes; expires 18 November 2026.
- The APK is signed with an ephemeral runner-only Debug key and is not a production Release build.
- This exact-head result verifies the Android JVM unit-test task, Debug compilation/packaging and artifact upload. It does not verify live Syrian station ingestion, CAP RSS reachability or authenticity, official alert distribution, radar coverage, production signing or physical-device behavior.
- PR #1 remains open and unmerged. The branch is 167 commits ahead of `main` and 0 behind; no changes were made to `main`.
