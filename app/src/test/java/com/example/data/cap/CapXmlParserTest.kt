package com.example.data.cap

import com.example.data.model.AlertSeverity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class CapXmlParserTest {

    private fun cap(
        status: String = "Actual",
        scope: String = "Public",
        messageType: String = "Alert",
        expires: String = "2026-11-04T12:00:00+03:00",
        sent: String = "2026-11-04T06:00:00+03:00"
    ) = """
        <?xml version="1.0" encoding="UTF-8"?>
        <alert xmlns="urn:oasis:names:tc:emergency:cap:1.2">
          <identifier>sy-test-001</identifier>
          <sender>alerts@example.gov.sy</sender>
          <sent>$sent</sent>
          <status>$status</status>
          <msgType>$messageType</msgType>
          <scope>$scope</scope>
          <info>
            <language>en-US</language>
            <event>Heavy rain</event>
            <severity>Minor</severity>
            <expires>2026-11-04T12:00:00+03:00</expires>
          </info>
          <info>
            <language>ar-SY</language>
            <category>Met</category>
            <event>أمطار غزيرة</event>
            <urgency>Immediate</urgency>
            <severity>Severe</severity>
            <certainty>Likely</certainty>
            <expires>$expires</expires>
            <description>نص تجريبي للاختبار</description>
            <instruction>تابع النشرات الرسمية</instruction>
            <area><areaDesc>دمشق</areaDesc></area>
          </info>
        </alert>
    """.trimIndent()

    @Test
    fun parsesPublicActualAlertAndPrefersArabicInfoBlock() {
        val parsed = CapXmlParser.parse(cap())
        assertEquals("sy-test-001", parsed.identifier)
        assertEquals("أمطار غزيرة", parsed.event)
        assertEquals("دمشق", parsed.areaDescription)
        assertEquals(AlertSeverity.WARNING, parsed.severity)
        assertTrue(parsed.isPubliclyDisplayable(Instant.parse("2026-11-04T06:30:00Z")))
    }

    @Test
    fun testExerciseAndPrivateMessagesAreNeverPubliclyDisplayed() {
        assertFalse(CapXmlParser.parse(cap(status = "Test")).isPubliclyDisplayable(
            Instant.parse("2026-11-04T06:30:00Z")
        ))
        assertFalse(CapXmlParser.parse(cap(status = "Exercise")).isPubliclyDisplayable(
            Instant.parse("2026-11-04T06:30:00Z")
        ))
        assertFalse(CapXmlParser.parse(cap(scope = "Private")).isPubliclyDisplayable(
            Instant.parse("2026-11-04T06:30:00Z")
        ))
    }

    @Test
    fun expiredAndCancelledMessagesAreNotShownAsActiveWarnings() {
        assertFalse(CapXmlParser.parse(cap(expires = "2026-11-04T07:00:00+03:00"))
            .isPubliclyDisplayable(Instant.parse("2026-11-04T06:30:00Z")))
        assertFalse(CapXmlParser.parse(cap(messageType = "Cancel"))
            .isPubliclyDisplayable(Instant.parse("2026-11-04T06:30:00Z")))
    }

    @Test
    fun updateMessagesRequireEveryReferenceToMatchReconciledAlert() {
        val noReferences = CapXmlParser.parse(cap(messageType = "Update"))
        assertFalse(noReferences.isPubliclyDisplayable(Instant.parse("2026-11-04T06:30:00Z")))

        val referenceA = "issuer@example.gov.sy,base-001,2026-11-04T05:00:00Z"
        val referenceB = "issuer@example.gov.sy,base-002,2026-11-04T05:15:00Z"
        val withReferences = CapXmlParser.parse(cap(messageType = "Update").replace(
            "<scope>Public</scope>",
            "<scope>Public</scope><references>$referenceA $referenceB</references>"
        ))
        assertEquals(2, withReferences.parsedReferences.size)
        val reconciledA = withReferences.parsedReferences.first()

        assertFalse(withReferences.isPubliclyDisplayable(
            Instant.parse("2026-11-04T06:30:00Z"),
            setOf(reconciledA)
        ))
        assertTrue(withReferences.isPubliclyDisplayable(
            Instant.parse("2026-11-04T06:30:00Z"),
            withReferences.parsedReferences.toSet()
        ))
    }

    @Test
    fun rejectsMalformedCapReferenceAndTimezoneLessReferenceTimestamp() {
        val malformed = cap(messageType = "Update").replace(
            "<scope>Public</scope>",
            "<scope>Public</scope><references>issuer@example.gov.sy,missing-time</references>"
        )
        val timezoneLess = cap(messageType = "Update").replace(
            "<scope>Public</scope>",
            "<scope>Public</scope><references>issuer@example.gov.sy,base-001,2026-11-04T05:00:00</references>"
        )
        listOf(malformed, timezoneLess).forEach { xml ->
            try {
                CapXmlParser.parse(xml)
                throw AssertionError("Malformed CAP references must be rejected")
            } catch (_: IllegalArgumentException) {
                // Expected: a CAP reference is a strict sender, identifier, sent triple.
            }
        }
    }

    @Test
    fun rejectsMissingRequiredEnvelopeFieldsAndDuplicateReferences() {
        val missingSender = cap().replace(
            "<sender>alerts@example.gov.sy</sender>",
            ""
        )
        val missingStatus = cap().replace("<status>Actual</status>", "")
        val duplicateReferences = cap(messageType = "Update").replace(
            "<scope>Public</scope>",
            "<scope>Public</scope><references>issuer@example.gov.sy,base-001,2026-11-04T05:00:00Z issuer@example.gov.sy,base-001,2026-11-04T05:00:00Z</references>"
        )

        listOf(missingSender, missingStatus, duplicateReferences).forEach { xml ->
            try {
                CapXmlParser.parse(xml)
                throw AssertionError("Invalid required CAP envelope fields must be rejected")
            } catch (_: IllegalArgumentException) {
                // Expected: reject incomplete or ambiguous CAP envelope data.
            }
        }
    }

    @Test
    fun rejectsUnknownCapEnvelopeEnumerationValues() {
        val unknownStatus = cap().replace("<status>Actual</status>", "<status>Urgent</status>")
        val unknownMessageType = cap().replace("<msgType>Alert</msgType>", "<msgType>Broadcast</msgType>")
        val unknownScope = cap().replace("<scope>Public</scope>", "<scope>Everyone</scope>")

        listOf(unknownStatus, unknownMessageType, unknownScope).forEach { xml ->
            try {
                CapXmlParser.parse(xml)
                throw AssertionError("Unknown CAP enumeration values must be rejected")
            } catch (_: IllegalArgumentException) {
                // Expected: CAP 1.2 envelope enumerations are closed sets.
            }
        }
    }

    @Test
    fun rejectsDoctypeAndExternalEntityPayloads() {
        val unsafe = """
            <!DOCTYPE alert [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
            <alert xmlns="urn:oasis:names:tc:emergency:cap:1.2">
              <identifier>&xxe;</identifier>
            </alert>
        """.trimIndent()
        try {
            CapXmlParser.parse(unsafe)
            throw AssertionError("Unsafe XML must be rejected")
        } catch (_: IllegalArgumentException) {
            // Expected: parser fails closed for DTD/entity declarations.
        }
    }

    @Test
    fun rejectsMissingExpiryAndInvalidXml() {
        val noExpiry = cap().replace("<expires>2026-11-04T12:00:00+03:00</expires>", "")
        assertFalse(CapXmlParser.parse(noExpiry).isPubliclyDisplayable(
            Instant.parse("2026-11-04T06:30:00Z")
        ))
        try {
            CapXmlParser.parse("<not-cap>")
            throw AssertionError("Malformed XML must be rejected")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }
    }
    @Test
    fun rejectsFutureSentTimestampAndExpiryBeforeSentTimestamp() {
        assertFalse(CapXmlParser.parse(
            cap(sent = "2026-11-04T10:00:00+03:00")
        ).isPubliclyDisplayable(Instant.parse("2026-11-04T06:30:00Z")))

        assertFalse(CapXmlParser.parse(
            cap(expires = "2026-11-04T05:00:00+03:00")
        ).isPubliclyDisplayable(Instant.parse("2026-11-04T01:30:00Z")))
    }

    @Test
    fun rejectsMissingOrInvalidSentTimestamp() {
        val missingSent = cap().replace("<sent>2026-11-04T06:00:00+03:00</sent>", "")
        try {
            CapXmlParser.parse(missingSent)
            throw AssertionError("Missing sent timestamp must be rejected")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }

        try {
            CapXmlParser.parse(cap(sent = "not-a-timestamp"))
            throw AssertionError("Invalid sent timestamp must be rejected")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }
    }

    @Test
    fun rejectsIncorrectCapNamespaceOnRoot() {
        val wrongNamespace = cap().replace(
            "urn:oasis:names:tc:emergency:cap:1.2",
            "urn:attacker:lookalike-cap"
        )
        try {
            CapXmlParser.parse(wrongNamespace)
            throw AssertionError("A non-CAP namespace root must be rejected")
        } catch (_: IllegalArgumentException) {
            // Expected: only the CAP 1.2 namespace is accepted.
        }
    }

    @Test
    fun ignoresForeignNamespaceIdentifierAndSentElements() {
        val foreignIdentifier = cap().replace(
            "<identifier>sy-test-001</identifier>",
            "<fake:identifier xmlns:fake=\"urn:attacker:spoof\">sy-test-001</fake:identifier>"
        )
        val foreignSent = cap().replace(
            "<sent>2026-11-04T06:00:00+03:00</sent>",
            "<fake:sent xmlns:fake=\"urn:attacker:spoof\">2026-11-04T06:00:00+03:00</fake:sent>"
        )

        listOf(foreignIdentifier, foreignSent).forEach { xml ->
            try {
                CapXmlParser.parse(xml)
                throw AssertionError("Foreign namespace CAP fields must not be accepted")
            } catch (_: IllegalArgumentException) {
                // Expected: standard CAP fields must use the CAP 1.2 namespace.
            }
        }
    }

}
