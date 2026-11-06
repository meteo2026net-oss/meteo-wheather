package com.example.data.cap

import java.time.Instant

/**
 * Immutable, fail-closed CAP message lifecycle reducer.
 *
 * It operates only on already parsed messages. It does not establish issuer trust,
 * download feeds, persist state, or make messages eligible for public display by itself.
 */
data class CapReconciledAlert(
    val originalReference: CapReference,
    val latestReference: CapReference,
    val currentMessage: CapFeedMessage,
    val messageHistory: Set<CapReference>,
    val cancelled: Boolean = false
)

data class CapReconciliationState(
    val alerts: Map<CapReference, CapReconciledAlert> = emptyMap()
) {
    fun apply(message: CapFeedMessage, now: Instant): CapReconciliationState {
        val reference = message.toReference()
            ?: return this

        return when (message.messageType) {
            "Alert" -> acceptAlert(message, reference, now)
            "Update" -> acceptUpdate(message, reference, now)
            "Cancel" -> acceptCancel(message, reference, now)
            else -> this
        }
    }

    private fun acceptAlert(
        message: CapFeedMessage,
        reference: CapReference,
        now: Instant
    ): CapReconciliationState {
        if (!message.isPubliclyDisplayable(now) || hasUsedIdentifier(reference)) return this
        val alert = CapReconciledAlert(
            originalReference = reference,
            latestReference = reference,
            currentMessage = message,
            messageHistory = setOf(reference)
        )
        return copy(alerts = alerts + (reference to alert))
    }

    private fun acceptUpdate(
        message: CapFeedMessage,
        reference: CapReference,
        now: Instant
    ): CapReconciliationState {
        val target = resolveSingleTarget(message) ?: return this
        if (target.cancelled || !target.currentMessage.expiresAt.isActiveAt(now)) return this
        if (hasUsedIdentifier(reference)) return this
        if (!isStrictlyNewer(message, target.latestReference)) return this
        if (!message.isPubliclyDisplayable(now, target.messageHistory)) return this

        val updated = target.copy(
            latestReference = reference,
            currentMessage = message,
            messageHistory = target.messageHistory + reference
        )
        return replaceAlert(target.originalReference, updated)
    }

    private fun acceptCancel(
        message: CapFeedMessage,
        reference: CapReference,
        now: Instant
    ): CapReconciliationState {
        val target = resolveSingleTarget(message) ?: return this
        if (target.cancelled || !target.currentMessage.expiresAt.isActiveAt(now)) return this
        if (hasUsedIdentifier(reference)) return this
        if (reference in target.messageHistory || alerts.values.any { reference in it.messageHistory }) return this
        if (!isStrictlyNewer(message, target.latestReference)) return this
        if (!isValidCancelEnvelope(message, now)) return this

        val cancelled = target.copy(
            latestReference = reference,
            messageHistory = target.messageHistory + reference,
            cancelled = true
        )
        return replaceAlert(target.originalReference, cancelled)
    }

    /** CAP identifier is unique per sender; sent time is part of a reference triple,
     * but must not make a reused message identifier look like a new message.
     */
    private fun hasUsedIdentifier(reference: CapReference): Boolean =
        alerts.values.any { alert ->
            alert.messageHistory.any {
                it.sender == reference.sender && it.identifier == reference.identifier
            }
        }

    private fun resolveSingleTarget(message: CapFeedMessage): CapReconciledAlert? {
        if (message.parsedReferences.isEmpty()) return null
        val matches = alerts.values.filter { alert ->
            !alert.cancelled && message.parsedReferences.all { it in alert.messageHistory }
        }
        return matches.singleOrNull()
    }

    private fun replaceAlert(
        key: CapReference,
        value: CapReconciledAlert
    ): CapReconciliationState = copy(alerts = alerts + (key to value))

    private fun isStrictlyNewer(
        message: CapFeedMessage,
        previous: CapReference
    ): Boolean = message.sentAt?.isAfter(previous.sentAt) == true

    private fun isValidCancelEnvelope(
        message: CapFeedMessage,
        now: Instant
    ): Boolean =
        message.status == "Actual" &&
            message.scope == "Public" &&
            message.messageType == "Cancel" &&
            message.sentAt?.let { !it.isAfter(now) } == true

    private fun CapFeedMessage.toReference(): CapReference? {
        val senderValue = sender?.takeIf(String::isNotBlank) ?: return null
        val sentValue = sentAt ?: return null
        if (identifier.isBlank()) return null
        return CapReference(senderValue, identifier, sentValue)
    }

    private fun Instant?.isActiveAt(now: Instant): Boolean = this?.isAfter(now) == true
}
