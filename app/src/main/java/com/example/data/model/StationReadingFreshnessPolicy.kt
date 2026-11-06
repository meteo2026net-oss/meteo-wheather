package com.example.data.model

/**
 * Prevents a verified station's old observation from being presented as current.
 * A reading needs a provider timestamp within the freshness window.
 */
object StationReadingFreshnessPolicy {
    const val DEFAULT_MAX_AGE_MILLIS: Long = 90L * 60L * 1000L
    const val MAX_FUTURE_SKEW_MILLIS: Long = 5L * 60L * 1000L

    fun isFresh(
        observedAtEpochMillis: Long?,
        nowEpochMillis: Long,
        maxAgeMillis: Long = DEFAULT_MAX_AGE_MILLIS
    ): Boolean {
        if (observedAtEpochMillis == null || maxAgeMillis < 0L) return false
        val ageMillis = try {
            Math.subtractExact(nowEpochMillis, observedAtEpochMillis)
        } catch (_: ArithmeticException) {
            return false
        }
        return ageMillis >= -MAX_FUTURE_SKEW_MILLIS && ageMillis <= maxAgeMillis
    }
}
