package com.github.premnirmal.ticker.home

/**
 * Platform-neutral seam supplying the localized strings shown in the home screen's tutorial and
 * "what's new" bottom sheets. The Android implementation resolves the string resources in `:app`,
 * keeping the resource lookup out of the shared [HomeViewModel]; iOS provides its own
 * implementation later.
 */
interface HomeStrings {

    /** Title of the tutorial bottom sheet (`R.string.how_to_title`). */
    fun tutorialTitle(): String

    /** Body of the tutorial bottom sheet (`R.string.how_to`). */
    fun tutorialMessage(): String

    /** Title of the "what's new" bottom sheet for [versionName] (`R.string.whats_new_in`). */
    fun whatsNewTitle(versionName: String): String

    /** Body shown when the "what's new" fetch fails, composed around [errorMessage]. */
    fun whatsNewError(errorMessage: String): String
}
