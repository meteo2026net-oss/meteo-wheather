package com.example.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationPermissionDecisionTest {

    private fun testAlert() = SevereWeatherAlert(
        id = "permission-test",
        titleAr = "تنبيه اختبار",
        severity = AlertSeverity.WARNING,
        descriptionAr = "وصف اختبار",
        safetyInstructionsAr = "تعليمات اختبار",
        source = "اختبار",
        validUntil = "غير محدد"
    )

    @Test
    fun grantedTestNotificationSendsAlertWithoutChangingSavedPreference() {
        val alert = testAlert()
        val result = NotificationPermissionDecision.resolve(true, alert, false)
        assertEquals(alert, result.testAlertToSend)
        assertEquals(null, result.alertsEnabled)
    }

    @Test
    fun deniedTestNotificationDoesNotChangeSavedPreference() {
        val result = NotificationPermissionDecision.resolve(false, testAlert(), false)
        assertEquals(null, result.testAlertToSend)
        assertEquals(null, result.alertsEnabled)
    }

    @Test
    fun grantedEnableRequestEnablesSavedPreference() {
        val result = NotificationPermissionDecision.resolve(true, null, true)
        assertEquals(null, result.testAlertToSend)
        assertEquals(true, result.alertsEnabled)
    }

    @Test
    fun deniedEnableRequestDisablesSavedPreference() {
        val result = NotificationPermissionDecision.resolve(false, null, true)
        assertEquals(null, result.testAlertToSend)
        assertEquals(false, result.alertsEnabled)
    }

    @Test
    fun simultaneousPendingActionsRemainIndependent() {
        val alert = testAlert()
        val result = NotificationPermissionDecision.resolve(true, alert, true)
        assertEquals(alert, result.testAlertToSend)
        assertEquals(true, result.alertsEnabled)
    }
}
