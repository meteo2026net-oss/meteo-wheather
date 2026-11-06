package com.example.data.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GpsLocationFreshnessPolicyTest {
    @Test
    fun acceptsLocationWithinTenMinuteWindow() {
        val now = 1_000_000L
        assertTrue(GpsLocationFreshnessPolicy.isFresh(now - 10 * 60 * 1000L, now))
        assertTrue(GpsLocationFreshnessPolicy.isFresh(now, now))
    }

    @Test
    fun rejectsOldOrMissingLocation() {
        val now = 1_000_000L
        assertFalse(GpsLocationFreshnessPolicy.isFresh(now - 10 * 60 * 1000L - 1L, now))
        assertFalse(GpsLocationFreshnessPolicy.isFresh(0L, now))
    }

    @Test
    fun rejectsFutureTimestampAndNegativeAgeWindow() {
        val now = 1_000_000L
        assertFalse(GpsLocationFreshnessPolicy.isFresh(now + 1L, now))
        assertFalse(GpsLocationFreshnessPolicy.isFresh(now, now, maxAgeMillis = -1L))
    }
}
