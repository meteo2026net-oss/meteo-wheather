package com.example.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SevereAlertNotificationPolicyTest {

    private fun alert(
        id: String,
        severity: AlertSeverity = AlertSeverity.WARNING,
        isActive: Boolean = true
    ) = SevereWeatherAlert(
        id = id,
        titleAr = "تنبيه اختبار",
        severity = severity,
        descriptionAr = "وصف اختبار",
        safetyInstructionsAr = "تعليمات اختبار",
        source = "اختبار",
        validUntil = "غير محدد",
        isActive = isActive
    )

    @Test
    fun allNewSevereAlertsAreSelectedRatherThanOnlyTheFirstOne() {
        val alerts = listOf(
            alert("storm", AlertSeverity.EMERGENCY),
            alert("rain", AlertSeverity.WARNING),
            alert("wind", AlertSeverity.ADVISORY)
        )

        assertEquals(
            listOf("storm", "rain"),
            SevereAlertNotificationPolicy.alertsRequiringNotification(alerts, emptySet()).map { it.id }
        )
    }

    @Test
    fun previouslyNotifiedActiveAlertsAreNotRepeated() {
        val alerts = listOf(alert("storm"), alert("rain"))

        assertEquals(
            listOf("rain"),
            SevereAlertNotificationPolicy.alertsRequiringNotification(alerts, setOf("storm")).map { it.id }
        )
    }

    @Test
    fun inactiveAndNonSevereAlertsAreNeverSelected() {
        val alerts = listOf(
            alert("inactive", isActive = false),
            alert("watch", AlertSeverity.WATCH),
            alert("advisory", AlertSeverity.ADVISORY)
        )

        assertEquals(
            emptyList<SevereWeatherAlert>(),
            SevereAlertNotificationPolicy.alertsRequiringNotification(alerts, emptySet())
        )
    }

    @Test
    fun disappearedAlertsAreRemovedFromThePersistedActiveSet() {
        val currentAlerts = listOf(alert("storm"), alert("rain"))
        val updated = SevereAlertNotificationPolicy.updatedNotifiedIds(
            alerts = currentAlerts,
            previouslyNotifiedIds = setOf("storm", "old-alert"),
            successfullyPostedIds = setOf("rain", "unrelated")
        )

        assertEquals(setOf("storm", "rain"), updated)
    }

    @Test
    fun failedNotificationIsNotMarkedAsNotifiedAndCanBeRetried() {
        val alerts = listOf(alert("storm"))
        val updated = SevereAlertNotificationPolicy.updatedNotifiedIds(
            alerts = alerts,
            previouslyNotifiedIds = emptySet(),
            successfullyPostedIds = emptySet()
        )

        assertEquals(emptySet<String>(), updated)
        assertEquals(
            listOf("storm"),
            SevereAlertNotificationPolicy.alertsRequiringNotification(alerts, updated).map { it.id }
        )
    }
}
