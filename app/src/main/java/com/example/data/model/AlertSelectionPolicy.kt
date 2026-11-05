package com.example.data.model

/** Keeps a remembered alert selection valid when a refreshed list changes size. */
object AlertSelectionPolicy {
    fun safeIndex(requestedIndex: Int, itemCount: Int): Int? =
        if (itemCount <= 0) null else requestedIndex.coerceIn(0, itemCount - 1)
}
