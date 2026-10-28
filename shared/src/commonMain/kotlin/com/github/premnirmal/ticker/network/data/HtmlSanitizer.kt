package com.github.premnirmal.ticker.network.data

/**
 * Converts an HTML fragment (as found in RSS `<title>`/`<description>` values) into display text:
 * decodes HTML entities and strips markup. This previously used Android's `android.text.Html` in the
 * `:app` module; it is now an `expect`/`actual` so the shared news models can sanitize on every
 * platform (Android keeps the exact `android.text.Html` behaviour; other platforms use a portable
 * Kotlin implementation).
 */
expect fun sanitizeHtml(html: String): String
