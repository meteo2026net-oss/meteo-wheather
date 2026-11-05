package com.example.data.model

/**
 * Pure policy for deduplicating notifications while a severe weather alert remains active.
 * The caller persists only IDs for alerts whose notification requests were accepted.
 */
object SevereAlertNotificationPolicy {

    private fun eligibleAlerts(alerts: List<SevereWeatherAlert>): List<SevereWeatherAlert> =
        alerts.filter {
            it.isActive &&
                (it.severity == AlertSeverity.WARNING || it.severity == AlertSeverity.EMERGENCY)
        }

    fun alertsRequiringNotification(
        alerts: List<SevereWeatherAlert>,
        previouslyNotifiedIds: Set<String>
    ): List<SevereWeatherAlert> =
        eligibleAlerts(alerts).filterNot { it.id in previouslyNotifiedIds }

    fun updatedNotifiedIds(
        alerts: List<SevereWeatherAlert>,
        previouslyNotifiedIds: Set<String>,
        successfullyPostedIds: Set<String>
    ): Set<String> {
        val activeIds = eligibleAlerts(alerts).map { it.id }.toSet()
        return (previouslyNotifiedIds intersect activeIds) +
            (successfullyPostedIds intersect activeIds)
    }
}
