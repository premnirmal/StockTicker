package com.github.premnirmal.ticker.network.data

/**
 * iOS implementation. There is no `android.text.Html` equivalent, so this uses the portable
 * [stripHtmlToText] helper that removes tags and decodes the common HTML entities found in RSS feeds.
 */
actual fun sanitizeHtml(html: String): String = stripHtmlToText(html)
