package com.github.premnirmal.ticker

import com.github.premnirmal.shared.CommonParcelable
import com.github.premnirmal.shared.CommonParcelize

/**
 * A wall-clock time of day (24h) used by the configured update window.
 *
 * This is the platform-neutral replacement for the former Android-only `AppPreferences.Time`: it
 * carries no `java.time`/JVM types, so the update-window settings ([UserPreferences.startTime] /
 * [UserPreferences.endTime]) can live in `commonMain` and be shared by Android and iOS. It keeps a
 * `Parcelable` implementation on Android via [CommonParcelable]/[CommonParcelize] (the Android
 * settings UI state that holds it is parceled), and is a plain value type on iOS.
 */
@CommonParcelize
data class Time(
    val hour: Int,
    val minute: Int
) : CommonParcelable {

    /** The `"HH:mm"` representation persisted in the key/value store. */
    fun toPrefString(): String {
        val h = hour.toString().padStart(2, '0')
        val m = minute.toString().padStart(2, '0')
        return "$h:$m"
    }

    companion object {
        /** Parses an `"HH:mm"` string (e.g. `"09:30"`), defaulting either component to `0`. */
        fun parse(time: String): Time {
            val split = time.split(":")
            val hour = split.getOrNull(0)?.trim()?.toIntOrNull() ?: 0
            val minute = split.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
            return Time(hour, minute)
        }
    }
}
