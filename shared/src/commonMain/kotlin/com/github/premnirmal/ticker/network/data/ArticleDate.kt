package com.github.premnirmal.ticker.network.data

/**
 * Parsed representation of an RSS `pubDate` value. Multiplatform replacement for the previous
 * `java.time`-based parsing in `NewsArticle` (which used `DateTimeFormatter.RFC_1123_DATE_TIME` with
 * an ISO-8601 `Instant` fallback): it exposes a [sortKey] for ordering (newest first) and the
 * [month]/[day] used to render the short "MMM d" label.
 *
 * @param sortKey epoch seconds derived from the parsed fields (UTC), used only for ordering. Mirrors
 * the previous behaviour of treating the written wall-clock time as the sortable value.
 * @param month 1-based month (1 = January), or `0` when the date could not be parsed.
 * @param day day of month, or `0` when the date could not be parsed.
 */
internal data class ArticleDate(
    val sortKey: Long,
    val month: Int,
    val day: Int
) {
    val isValid: Boolean get() = month in 1..12 && day in 1..31

    companion object {
        private const val SECONDS_PER_DAY = 86_400L

        val INVALID = ArticleDate(sortKey = Long.MIN_VALUE, month = 0, day = 0)

        private val MONTH_ABBREVIATIONS = listOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        )

        /**
         * Parses [publishedAt], trying RFC-1123 ("Tue, 3 Jun 2008 11:05:30 GMT") first and falling
         * back to an ISO-8601 instant ("2008-06-03T11:05:30Z"). Returns [INVALID] when neither
         * format matches, so callers can treat it as "unknown date" instead of throwing.
         */
        fun parse(publishedAt: String?): ArticleDate {
            if (publishedAt.isNullOrBlank()) return INVALID
            return parseRfc1123(publishedAt.trim()) ?: parseIso(publishedAt.trim()) ?: INVALID
        }

        fun monthAbbreviation(month: Int): String =
            if (month in 1..12) MONTH_ABBREVIATIONS[month - 1] else ""

        private fun parseRfc1123(value: String): ArticleDate? {
            // Drop the optional leading weekday ("Tue, ").
            val withoutWeekday = value.substringAfter(',', value).trim()
            val tokens = withoutWeekday.split(Regex("\\s+"))
            if (tokens.size < 4) return null
            val day = tokens[0].toIntOrNull() ?: return null
            val month = MONTH_ABBREVIATIONS.indexOfFirst { it.equals(tokens[1], ignoreCase = true) }
                .let { if (it < 0) return null else it + 1 }
            val year = tokens[2].toIntOrNull() ?: return null
            val time = tokens[3].split(':')
            if (time.size < 2) return null
            val hour = time[0].toIntOrNull() ?: return null
            val minute = time[1].toIntOrNull() ?: return null
            val second = time.getOrNull(2)?.toIntOrNull() ?: 0
            val sortKey = epochSeconds(year, month, day, hour, minute, second)
            return ArticleDate(sortKey = sortKey, month = month, day = day)
        }

        private fun parseIso(value: String): ArticleDate? {
            val instant = try {
                kotlin.time.Instant.parse(value)
            } catch (e: IllegalArgumentException) {
                return null
            }
            val epochSeconds = instant.epochSeconds
            val days = floorDiv(epochSeconds, SECONDS_PER_DAY)
            val (year, month, day) = civilFromDays(days)
            return ArticleDate(sortKey = epochSeconds, month = month, day = day)
        }

        /** Multiplatform floored integer division (matches `Math.floorDiv`, which is JVM-only). */
        private fun floorDiv(x: Long, y: Long): Long {
            var q = x / y
            if ((x xor y) < 0 && q * y != x) q--
            return q
        }

        // Day-count algorithms from Howard Hinnant's "chrono-Compatible Low-Level Date Algorithms".
        private fun epochSeconds(
            year: Int,
            month: Int,
            day: Int,
            hour: Int,
            minute: Int,
            second: Int
        ): Long {
            val days = daysFromCivil(year, month, day)
            return days * SECONDS_PER_DAY + hour * 3600L + minute * 60L + second
        }

        private fun daysFromCivil(year: Int, month: Int, day: Int): Long {
            val y = (if (month <= 2) year - 1 else year).toLong()
            val era = (if (y >= 0) y else y - 399) / 400
            val yoe = y - era * 400
            val mp = if (month > 2) month - 3 else month + 9
            val doy = (153 * mp + 2) / 5 + day - 1
            val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
            return era * 146097 + doe - 719468
        }

        private fun civilFromDays(z0: Long): Triple<Int, Int, Int> {
            val z = z0 + 719468
            val era = (if (z >= 0) z else z - 146096) / 146097
            val doe = z - era * 146097
            val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
            val y = yoe + era * 400
            val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
            val mp = (5 * doy + 2) / 153
            val day = doy - (153 * mp + 2) / 5 + 1
            val month = if (mp < 10) mp + 3 else mp - 9
            val year = if (month <= 2) y + 1 else y
            return Triple(year.toInt(), month.toInt(), day.toInt())
        }
    }
}
