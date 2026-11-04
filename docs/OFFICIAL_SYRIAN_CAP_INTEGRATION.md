# Official Syrian CAP integration — implementation status

## Verified source identity

The WMO Register of Alerting Authorities lists the Syrian Arab Republic issuing authority as the Ministry of Emergency and Disaster Management and publishes this CAP RSS endpoint:

- https://climweb.med.gov.sy/api/cap/rss.xml
- WMO authority record: https://alertingauthority.wmo.int/authorities.php?recId=338

This establishes the registered source identity and endpoint listing. It does **not** prove that the feed is reachable at every moment or that a current alert is present. The feed XML could not be independently retrieved during this implementation session; therefore the app does not yet fetch or publish feed entries.

## Implemented in this branch

- Added a side-effect-free CAP XML parser for CAP alert documents.
- Parser disables DTD and external entity processing, rejects oversized documents, and requires a CAP `alert` root plus an identifier.
- Arabic `info` content is preferred when multiple language blocks are present.
- CAP severity values are mapped to the app's existing alert severity enum.
- Public display gate accepts only `status=Actual`, `scope=Public`, `msgType=Alert|Update`, and a valid future `expires` timestamp.
- Test, Exercise, Private, Cancel, expired, and expiry-less messages are not treated as active public warnings.
- CAP references are retained so a later feed processor can reconcile Update and Cancel messages.

## Not yet implemented / not claimed

- No network client is connected to the RSS feed.
- No RSS item-to-CAP document downloader exists yet.
- No sender-signature or transport authenticity verification is implemented by this parser. A trusted configured source and HTTPS transport remain mandatory.
- No update/cancel reconciliation store is connected to the UI.
- No official CAP alert is currently displayed by the application.

## Next engineering steps

1. Validate the live RSS response and confirm its item-link structure and update cadence.
2. Add a fixed-host HTTPS client for the registered WMO feed; reject off-host redirects and links.
3. Fetch and parse each linked CAP document with this parser.
4. Persist identifiers/references and reconcile Update/Cancel messages without resurrecting expired alerts.
5. Add integration tests using captured, sanitized feed fixtures before enabling the official-alert UI.


## Latest Android CI verification — 4 November 2026

- Development-branch head `74055aea7d7b50384d2a5bb2614d15c3d0b49f3f` completed GitHub Actions run [41845812390](https://github.com/meteo2026net-oss/meteo-wheather/actions/runs/41845812390) successfully.
- The `testDebugUnitTest` and `assembleDebug` tasks both completed successfully; the workflow also uploaded the Debug APK and unit-test reports.
- Debug APK artifact: `mcnc-weather-debug-apk`, ID `13330578595`, 22,709,100 bytes; expires 2 February 2027. ZIP SHA-256: `c52d1a8aeef3a2fd8e93fa14c0584246107b4e7723ed1da0c0795bd7ea06bebd`.
- Unit-test report artifact: `android-unit-test-reports`, ID `13330433957`, 17,922 bytes; expires 18 November 2026.
- The APK is signed with an ephemeral runner-only Debug key, not the production Release signing identity.
- This verifies Android Debug compilation, JVM unit tests and packaging for this exact branch head. It does not verify live CAP feed reachability, RSS item structure, sender authenticity, official alert distribution, station ingestion, or physical-device behavior.


## Latest Android CI verification — 4 November 2026 (current branch head)

- Commit `2972f302bf60d8df120b86a22454ad62b64405e0` completed GitHub Actions run [41849490776](https://github.com/meteo2026net-oss/meteo-wheather/actions/runs/41849490776) successfully.
- The `testDebugUnitTest` and `assembleDebug` steps both succeeded; both the Debug APK and unit-test report uploads succeeded.
- Debug APK artifact: `mcnc-weather-debug-apk`, ID `13334906287`, 22,709,102 bytes; expires 2 February 2027. ZIP SHA-256: `08ac1baf4fedfb6a83ceef3313e137a920d56c38a399c6e9055a193cde34f00b`.
- Unit-test report artifact: `android-unit-test-reports`, ID `13334321811`, 17,946 bytes; expires 18 November 2026.
- The APK is signed with an ephemeral runner-only Debug key. This is not a production Release build or release-signing validation.
- This run verifies JVM unit tests, Android Debug compilation and APK packaging for this exact head. It does not verify live CAP feed reachability, RSS item structure, sender authenticity, official alert distribution, station ingestion or physical-device behavior.
