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
