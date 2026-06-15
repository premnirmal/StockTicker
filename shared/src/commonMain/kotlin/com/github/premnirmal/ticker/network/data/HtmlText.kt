package com.github.premnirmal.ticker.network.data

/**
 * Portable, dependency-free "HTML fragment → plain text" conversion used by the non-Android
 * [sanitizeHtml] actuals. It strips tags and decodes the HTML entities that commonly appear in RSS
 * `<title>`/`<description>` values. It is intentionally simple (RSS snippets, not full documents);
 * Android uses the richer `android.text.Html` instead.
 */
internal fun stripHtmlToText(html: String): String {
    // Remove tags first, then decode entities so that decoded '<'/'>' are not treated as tags.
    val withoutTags = html.replace(Regex("<[^>]*>"), "")
    return decodeHtmlEntities(withoutTags)
}

private val NAMED_ENTITIES = mapOf(
    "amp" to "&",
    "lt" to "<",
    "gt" to ">",
    "quot" to "\"",
    "apos" to "'",
    "nbsp" to "\u00A0",
    "#39" to "'"
)

/**
 * Decodes named entities from [NAMED_ENTITIES] plus numeric (`&#123;`) and hex (`&#x1F;`) character
 * references. Unknown entities are left untouched.
 */
private fun decodeHtmlEntities(text: String): String {
    if (text.indexOf('&') < 0) return text
    val entityRegex = Regex("&(#x?[0-9a-fA-F]+|[a-zA-Z][a-zA-Z0-9]*);")
    return entityRegex.replace(text) { match ->
        val body = match.groupValues[1]
        when {
            body.startsWith("#x") || body.startsWith("#X") -> {
                body.substring(2).toIntOrNull(16)?.let { codePointToString(it) } ?: match.value
            }
            body.startsWith("#") -> {
                body.substring(1).toIntOrNull()?.let { codePointToString(it) } ?: match.value
            }
            else -> NAMED_ENTITIES[body] ?: match.value
        }
    }
}

private fun codePointToString(codePoint: Int): String =
    if (codePoint in 0..0x10FFFF) {
        StringBuilder().appendCodePointCompat(codePoint).toString()
    } else {
        ""
    }

/**
 * Multiplatform-safe replacement for `StringBuilder.appendCodePoint` (which is JVM-only): appends the
 * UTF-16 representation of [codePoint], using a surrogate pair for supplementary code points.
 */
private fun StringBuilder.appendCodePointCompat(codePoint: Int): StringBuilder {
    if (codePoint <= 0xFFFF) {
        append(codePoint.toChar())
    } else {
        val offset = codePoint - 0x10000
        append((0xD800 + (offset shr 10)).toChar())
        append((0xDC00 + (offset and 0x3FF)).toChar())
    }
    return this
}
