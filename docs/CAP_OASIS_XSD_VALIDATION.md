# OASIS CAP 1.2 schema regression gate

The test-only schema at `app/src/test/resources/cap/CAP-v1.2.xsd` is the CAP 1.2 XML Schema, retaining the OASIS copyright notice.

- Normative schema URI: https://docs.oasis-open.org/emergency/cap/v1.2/CAP-v1.2.xsd
- Standard: OASIS Common Alerting Protocol Version 1.2, 1 July 2010.
- Namespace: `urn:oasis:names:tc:emergency:cap:1.2`
- Mirror used to obtain the schema source: `kelvinn/capparselib`, `src/schema/CAP-v1.2.xsd`, commit `127a343e342cd4c23e8320e93db99df98bc3bed5`.

The JVM regression tests compile the bundled schema with JAXP and check a valid CAP message plus invalid element ordering and invalid timezone formatting. External DTD and schema access are disabled in the test validator.

This is a schema-validation regression gate for test fixtures. It does not certify a live feed, sender authenticity, or the application as a complete CAP producer/consumer. The parser is not yet wired to schema validation in production runtime.
