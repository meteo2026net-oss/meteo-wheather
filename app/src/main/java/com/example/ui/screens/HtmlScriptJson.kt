package com.example.ui.screens

/**
 * Encodes JSON for safe embedding inside an HTML script element.
 * JSON string escaping alone does not prevent a value containing </script>
 * from terminating the surrounding HTML script element.
 */
internal fun encodeJsonForHtmlScript(json: String): String = json
    .replace("&", "\\u0026")
    .replace("<", "\\u003c")
    .replace(">", "\\u003e")
    .replace("\u2028", "\\u2028")
    .replace("\u2029", "\\u2029")
