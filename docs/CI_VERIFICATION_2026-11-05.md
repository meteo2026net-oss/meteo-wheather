# Android CI verification — 5 November 2026

## Exact source revision

- Branch: `work/data-quality-foundation`
- Commit: `f4b8013dc3a2139b3cc8ed8675c7a8dcc431c497`
- Workflow run: [41949129217](https://github.com/meteo2026net-oss/meteo-wheather/actions/runs/41949129217)
- Result: completed successfully.
- Verified tasks: `testDebugUnitTest` and `assembleDebug`.
- Debug APK artifact: `mcnc-weather-debug-apk`, ID `13379738053`, size 22,716,346 bytes, SHA-256 `178fb2a59ca98aa680d23df7aed85434f1f13a704c3100c90aa77fd65920d1c3`.
- Unit-test report artifact: `android-unit-test-reports`, ID `13379643330`, size 23,083 bytes, SHA-256 `94b10bf6354bf898a545370d05f52c701ee57e200936d9e29fdadc62fd3feeb4`.

The APK is a CI Debug build signed with an ephemeral runner-only key. It is not a production Release build and does not validate production signing or physical-device behavior. The test report artifact is available for 14 days from the run; the APK artifact is available for 90 days.

## Multi-city forecast response association

The repository now associates each multi-location Open-Meteo response with its requested city using the returned latitude/longitude grid-cell centre, rather than assuming response list order matches request order.

The matcher rejects:
- empty city lists or mismatched city/response counts;
- invalid requested or returned coordinates;
- responses farther than 30 km from every requested city;
- ambiguous nearest-city assignments with less than a 5 km separation margin;
- duplicate assignments or any city left unmatched.

The distance and separation limits are conservative application safeguards, not claims about a documented provider ordering guarantee or a universal model-grid resolution. If a response cannot be matched confidently, the repository fails the multi-city result instead of silently displaying a neighbouring city's forecast. The thresholds should be revisited against observed provider grid coordinates and supported geographic coverage.

## Remaining verification boundaries

This run verifies JVM unit tests and Android Debug compilation/packaging for the exact commit above. It does not establish:
- live ingestion from Syrian meteorological stations;
- availability or authenticity of the official CAP feed;
- official alert publishing;
- verified temperature, wind or cloud raster overlays;
- RainViewer coverage over all Syrian regions;
- Gemini API, Google OAuth or speech-to-text integration;
- production Release signing or real-device behavior.

The workflow also reports a deprecated Android SDK Manager CLI notice and an advisory that Gradle 9.3.1 is out of date. These are maintenance warnings, not failures in this run. Toolchain upgrades require a separate compatibility check.
