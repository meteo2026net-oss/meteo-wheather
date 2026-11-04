package com.example.data.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CityFavoriteMatchingTest {

    @Test
    fun matchesCityNameAndCountryWithoutCaseSensitivity() {
        assertTrue(CityFavoriteMatching.matches("Paris", "France", "paris", "FRANCE"))
    }

    @Test
    fun doesNotMatchSameCityNameInDifferentCountries() {
        assertFalse(CityFavoriteMatching.matches("Springfield", "United States", "Springfield", "United Kingdom"))
    }

    @Test
    fun trimsNamesAndCountryCodesBeforeMatching() {
        assertTrue(CityFavoriteMatching.matches("  دمشق ", " سوريا ", "دمشق", "سوريا"))
    }

    @Test
    fun legacyFavoriteWithoutCountryStillMatchesByName() {
        assertTrue(CityFavoriteMatching.matches("Damascus", "", "damascus", "Syria"))
    }

    @Test
    fun blankQueryCountryPreservesNameOnlyCompatibility() {
        assertTrue(CityFavoriteMatching.matches("Damascus", "Syria", "Damascus", ""))
    }

    @Test
    fun differentCityNamesNeverMatchEvenWhenCountryIsBlank() {
        assertFalse(CityFavoriteMatching.matches("Damascus", "", "Aleppo", ""))
    }
}
