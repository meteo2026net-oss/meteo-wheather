package com.example.data.cap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class CapMessageReconciliationTest {
    private val now = Instant.parse("2026-11-04T06:30:00Z")

    private fun message(
        id: String,
        type: String = "Alert",
        sent: String = "2026-11-04T06:00:00Z",
        refs: String = ""
    ): CapFeedMessage {
        val referenceXml = if (refs.isBlank()) "" else "<references>$refs</references>"
        return CapXmlParser.parse("""
            <alert xmlns="urn:oasis:names:tc:emergency:cap:1.2">
              <identifier>$id</identifier><sender>issuer@example.gov.sy</sender>
              <sent>$sent</sent><status>Actual</status><msgType>$type</msgType>
              <scope>Public</scope>$referenceXml
              <info><language>ar-SY</language><event>اختبار</event>
                <severity>Severe</severity><expires>2026-11-04T08:00:00Z</expires>
              </info>
            </alert>
        """.trimIndent())
    }

    private val base = "issuer@example.gov.sy,base-1,2026-11-04T06:00:00Z"

    @Test fun acceptsAlertAndKeepsItsOriginalIdentity() {
        val state = CapReconciliationState().apply(message("base-1"), now)
        val key = CapReference("issuer@example.gov.sy", "base-1", Instant.parse("2026-11-04T06:00:00Z"))
        assertTrue(state.alerts.containsKey(key))
        assertEquals(key, state.alerts[key]?.originalReference)
    }

    @Test fun rejectsUpdateWithoutKnownReferenceOrWithPartialReferenceSet() {
        val initial = CapReconciliationState().apply(message("base-1"), now)
        val unknown = message("upd-1", "Update", "2026-11-04T06:10:00Z",
            "issuer@example.gov.sy,unknown,2026-11-04T05:00:00Z")
        val partial = message("upd-2", "Update", "2026-11-04T06:11:00Z",
            "$base issuer@example.gov.sy,unknown,2026-11-04T05:00:00Z")
        assertEquals(initial, initial.apply(unknown, now))
        assertEquals(initial, initial.apply(partial, now))
    }

    @Test fun appliesOrderedUpdateAndAllowsNextUpdateToReferenceEarlierChainEntry() {
        val initial = CapReconciliationState().apply(message("base-1"), now)
        val update = message("upd-1", "Update", "2026-11-04T06:10:00Z", base)
        val afterUpdate = initial.apply(update, now)
        assertEquals(1, afterUpdate.alerts.size)
        val record = afterUpdate.alerts.values.single()
        assertEquals("upd-1", record.currentMessage.identifier)
        assertEquals(2, record.messageHistory.size)

        val chained = message("upd-2", "Update", "2026-11-04T06:20:00Z",
            "issuer@example.gov.sy,upd-1,2026-11-04T06:10:00Z")
        val afterChain = afterUpdate.apply(chained, now)
        assertEquals("upd-2", afterChain.alerts.values.single().currentMessage.identifier)
    }

    @Test fun rejectsStaleUpdateAndUpdateAfterExpiry() {
        val initial = CapReconciliationState().apply(message("base-1"), now)
        val stale = message("upd-old", "Update", "2026-11-04T05:59:00Z", base)
        assertEquals(initial, initial.apply(stale, now))

        val expiredNow = Instant.parse("2026-11-04T09:00:00Z")
        val late = message("upd-late", "Update", "2026-11-04T08:30:00Z", base)
        assertEquals(initial, initial.apply(late, expiredNow))
    }

    @Test fun cancellationMarksOnlyItsReferencedAlertInactive() {
        val initial = CapReconciliationState().apply(message("base-1"), now)
        val cancel = message("cancel-1", "Cancel", "2026-11-04T06:15:00Z", base)
        val result = initial.apply(cancel, now)
        assertTrue(result.alerts.values.single().cancelled)
        assertEquals("base-1", result.alerts.values.single().originalReference.identifier)

        val laterUpdate = message("upd-after-cancel", "Update", "2026-11-04T06:20:00Z", base)
        assertEquals(result, result.apply(laterUpdate, now))
    }

    @Test fun acceptsValidCancelWithoutOptionalInfoOrExpiry() {
        val initial = CapReconciliationState().apply(message("base-1"), now)
        val cancel = CapXmlParser.parse("""
            <alert xmlns="urn:oasis:names:tc:emergency:cap:1.2">
              <identifier>cancel-no-info</identifier>
              <sender>issuer@example.gov.sy</sender>
              <sent>2026-11-04T06:15:00Z</sent>
              <status>Actual</status><msgType>Cancel</msgType><scope>Public</scope>
              <references>$base</references>
            </alert>
        """.trimIndent())
        assertEquals(null, cancel.expiresAt)
        val result = initial.apply(cancel, now)
        assertTrue(result.alerts.values.single().cancelled)
    }

    @Test fun rejectsReusedMessageReferenceInUpdateChain() {
        val initial = CapReconciliationState().apply(message("base-1"), now)
        val update = message("upd-1", "Update", "2026-11-04T06:10:00Z", base)
        val afterUpdate = initial.apply(update, now)
        val reused = message("base-1", "Update", "2026-11-04T06:20:00Z",
            "issuer@example.gov.sy,upd-1,2026-11-04T06:10:00Z")
        assertEquals(afterUpdate, afterUpdate.apply(reused, now))
    }

    @Test fun rejectsCancelWithUnknownReferenceAndDoesNotCreateAlertFromCancel() {
        val empty = CapReconciliationState()
        val orphanCancel = message("cancel-orphan", "Cancel", "2026-11-04T06:15:00Z", base)
        assertEquals(empty, empty.apply(orphanCancel, now))
        assertFalse(empty.apply(orphanCancel, now).alerts.values.any { it.cancelled })
    }
}
