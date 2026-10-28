package com.github.premnirmal.ticker.network.data

import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.text.Html

/**
 * Android implementation backed by [android.text.Html], preserving the exact sanitization behaviour
 * the app previously used in `NewsArticle.descriptionSanitized()` / `titleSanitized()`.
 */
actual fun sanitizeHtml(html: String): String {
    return if (VERSION.SDK_INT >= VERSION_CODES.N) {
        Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT).toString()
    } else {
        @Suppress("DEPRECATION")
        Html.fromHtml(html).toString()
    }
}
