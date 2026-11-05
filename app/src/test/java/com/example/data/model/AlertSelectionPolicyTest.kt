package com.example.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlertSelectionPolicyTest {
    @Test
    fun returnsNullWhenAlertListIsEmpty() {
        assertNull(AlertSelectionPolicy.safeIndex(requestedIndex = 2, itemCount = 0))
    }

    @Test
    fun preservesSelectionWhenItIsWithinTheUpdatedList() {
        assertEquals(1, AlertSelectionPolicy.safeIndex(requestedIndex = 1, itemCount = 3))
    }

    @Test
    fun clampsSelectionToLastItemWhenListShrinks() {
        assertEquals(1, AlertSelectionPolicy.safeIndex(requestedIndex = 4, itemCount = 2))
    }

    @Test
    fun clampsNegativeSelectionToFirstItem() {
        assertEquals(0, AlertSelectionPolicy.safeIndex(requestedIndex = -2, itemCount = 3))
    }
}
