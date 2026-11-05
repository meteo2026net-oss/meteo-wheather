package com.example.data.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StationReadingFreshnessPolicyTest {
    private val now = 1_800_000_000_000L

    @Test
    fun recentObservationIsFresh() {
        assertTrue(StationReadingFreshnessPolicy.isFresh(now - 30L * 60L * 1000L, now))
    }

    @Test
    fun observationOlderThanNinetyMinutesIsStale() {
        assertFalse(StationReadingFreshnessPolicy.isFresh(now - 91L * 60L * 1000L, now))
    }

    @Test
    fun missingTimestampIsNeverFresh() {
        assertFalse(StationReadingFreshnessPolicy.isFresh(null, now))
    }

    @Test
    fun smallClockSkewIntoFutureIsAccepted() {
        assertTrue(StationReadingFreshnessPolicy.isFresh(now + 2L * 60L * 1000L, now))
    }

    @Test
    fun implausiblyFutureTimestampIsRejected() {
        assertFalse(StationReadingFreshnessPolicy.isFresh(now + 6L * 60L * 1000L, now))
    }

    @Test
    fun customFreshnessWindowIsRespected() {
        assertFalse(StationReadingFreshnessPolicy.isFresh(now - 11_000L, now, maxAgeMillis = 10_000L))
    }
}
