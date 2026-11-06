package com.example.ui.components

import java.net.URI

/**
 * Validation rules for the public RainViewer historical radar manifest.
 * The same constants are embedded into the WebView script and exercised by JVM tests.
 */
internal object RainViewerFramePolicy {
    const val PATH_PATTERN = "^/v2/radar/[A-Za-z0-9_-]+$"
    const val HOST_SUFFIX = "rainviewer.com"

    fun isValidRadarPath(path: String?): Boolean =
        path != null && Regex(PATH_PATTERN).matches(path)

    fun isTrustedTileHost(host: String?): Boolean {
        if (host.isNullOrBlank()) return false
        val uri = runCatching { URI(host) }.getOrNull() ?: return false
        val hostname = uri.host?.lowercase() ?: return false
        val trustedDomain = hostname == HOST_SUFFIX ||
            hostname.endsWith(".$HOST_SUFFIX")
        return uri.scheme.equals("https", ignoreCase = true) &&
            trustedDomain &&
            uri.userInfo == null &&
            (uri.port == -1 || uri.port == 443) &&
            uri.path.isNullOrEmpty() &&
            uri.query == null &&
            uri.fragment == null
    }
}
