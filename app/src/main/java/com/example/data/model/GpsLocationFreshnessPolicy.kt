package com.example.data.model

/**
 * Accepts a cached Android location only when it is recent enough to represent
 * the device's current area for weather lookup.
 */
internal object GpsLocationFreshnessPolicy {
    const val MAX_AGE_MILLIS: Long = 10 * 60 * 1000L

    fun isFresh(
        locationTimeMillis: Long,
        nowMillis: Long,
        maxAgeMillis: Long = MAX_AGE_MILLIS
    ): Boolean {
        if (locationTimeMillis <= 0L || nowMillis < locationTimeMillis || maxAgeMillis < 0L) {
            return false
        }
        return nowMillis - locationTimeMillis <= maxAgeMillis
    }
}
