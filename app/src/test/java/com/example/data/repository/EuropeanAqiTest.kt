package com.example.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class EuropeanAqiTest {
    @Test
    fun mapsAllEuropeanAqiBandsAtTheirBoundaries() {
        assertEquals("جيد", EuropeanAqi.describe(0).first)
        assertEquals("جيد", EuropeanAqi.describe(20).first)
        assertEquals("مقبول", EuropeanAqi.describe(21).first)
        assertEquals("مقبول", EuropeanAqi.describe(40).first)
        assertEquals("متوسط", EuropeanAqi.describe(41).first)
        assertEquals("متوسط", EuropeanAqi.describe(60).first)
        assertEquals("رديء", EuropeanAqi.describe(61).first)
        assertEquals("رديء", EuropeanAqi.describe(80).first)
        assertEquals("رديء جداً", EuropeanAqi.describe(81).first)
        assertEquals("رديء جداً", EuropeanAqi.describe(100).first)
        assertEquals("شديد السوء", EuropeanAqi.describe(101).first)
    }

    @Test
    fun negativeValueIsNotReportedAsGoodAir() {
        assertEquals("غير متاح", EuropeanAqi.describe(-1).first)
    }
}
