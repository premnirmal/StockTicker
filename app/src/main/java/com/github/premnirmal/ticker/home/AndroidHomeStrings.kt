package com.github.premnirmal.ticker.home

import android.content.Context
import com.github.premnirmal.tickerwidget.R

/**
 * Android implementation of [HomeStrings] that resolves the home screen's tutorial and
 * "what's new" copy from string resources, keeping the resource lookup out of the shared
 * [HomeViewModel].
 */
class AndroidHomeStrings(private val context: Context) : HomeStrings {

    override fun tutorialTitle(): String = context.getString(R.string.how_to_title)

    override fun tutorialMessage(): String = context.getString(R.string.how_to)

    override fun whatsNewTitle(versionName: String): String =
        context.getString(R.string.whats_new_in, versionName)

    override fun whatsNewError(errorMessage: String): String =
        "${context.getString(R.string.error_fetching_whats_new)}\n\n :( $errorMessage"
}
