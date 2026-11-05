package com.example.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HtmlScriptJsonEncodingTest {
    @Test
    fun escapesHtmlScriptDelimitersInsideJsonStrings() {
        val encoded = encodeJsonForHtmlScript("{\"name\":\"</script><script>alert(1)</script>\"}")

        assertFalse(encoded.contains("<"))
        assertFalse(encoded.contains(">"))
        assertTrue(encoded.contains("\\u003c/script\\u003e"))
    }

    @Test
    fun escapesAmpersandsAndJavaScriptLineSeparators() {
        val encoded = encodeJsonForHtmlScript("{\"value\":\"A&B\u2028C\u2029\"}")

        assertTrue(encoded.contains("\\u0026"))
        assertTrue(encoded.contains("\\u2028"))
        assertTrue(encoded.contains("\\u2029"))
    }

    @Test
    fun leavesOrdinaryJsonContentReadable() {
        assertTrue(encodeJsonForHtmlScript("{\"city\":\"Damascus\"}") == "{\"city\":\"Damascus\"}")
    }
}
