package com.example.data.model

/**
 * Resolves the independent actions that may be waiting for the Android
 * notification permission prompt.
 *
 * A test-notification request is transient and must never change the user's
 * persistent severe-alert preference.
 */
data class NotificationPermissionResolution(
    val testAlertToSend: SevereWeatherAlert?,
    /** Null means the permission result must not change the saved preference. */
    val alertsEnabled: Boolean?
)

object NotificationPermissionDecision {
    fun resolve(
        permissionGranted: Boolean,
        pendingTestAlert: SevereWeatherAlert?,
        pendingEnableAlerts: Boolean
    ): NotificationPermissionResolution = NotificationPermissionResolution(
        testAlertToSend = pendingTestAlert.takeIf { permissionGranted },
        alertsEnabled = if (pendingEnableAlerts) permissionGranted else null
    )
}
