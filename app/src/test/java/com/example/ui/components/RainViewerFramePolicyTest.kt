package com.example.ui.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RainViewerFramePolicyTest {

    @Test
    fun acceptsCurrentTimestampAndOpaqueRadarFrameIdentifiers() {
        assertTrue(RainViewerFramePolicy.isValidRadarPath("/v2/radar/1712345678"))
        assertTrue(RainViewerFramePolicy.isValidRadarPath("/v2/radar/7ed4e09f-abc_123"))
    }

    @Test
    fun rejectsUnexpectedRadarPathsAndPathTraversal() {
        listOf(
            null,
            "",
            "/v2/satellite/1712345678",
            "/v2/radar/",
            "/v2/radar/../private",
            "/v2/radar/abc?redirect=https://evil.example",
            "https://evil.example/v2/radar/123",
            "/v2/radar/abc/extra"
        ).forEach { path ->
            assertFalse("Unexpected path accepted: $path", RainViewerFramePolicy.isValidRadarPath(path))
        }
    }

    @Test
    fun acceptsOnlyHttpsRainViewerTileHosts() {
        assertTrue(RainViewerFramePolicy.isTrustedTileHost("https://tilecache.rainviewer.com"))
        assertTrue(RainViewerFramePolicy.isTrustedTileHost("https://cdn.rainviewer.com"))
        assertTrue(RainViewerFramePolicy.isTrustedTileHost("https://rainviewer.com"))
    }

    @Test
    fun rejectsUntrustedOrAmbiguousTileHosts() {
        listOf(
            null,
            "",
            "http://tilecache.rainviewer.com",
            "https://rainviewer.com.attacker.example",
            "https://notrainviewer.com",
            "https://rainviewer.com@attacker.example",
            "https://tilecache.rainviewer.com:8443",
            "https://tilecache.rainviewer.com/path",
            "https://tilecache.rainviewer.com?next=attacker"
        ).forEach { host ->
            assertFalse("Unexpected host accepted: $host", RainViewerFramePolicy.isTrustedTileHost(host))
        }
    }
}
