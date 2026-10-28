package com.github.premnirmal.ticker.review

import com.github.premnirmal.ticker.UserPreferences
import com.github.premnirmal.ticker.components.AppLogger
import platform.StoreKit.SKStoreReviewController
import platform.UIKit.UIApplication
import platform.UIKit.UIWindowScene

/**
 * iOS in-app review prompt, the counterpart of Android's `AppReviewManager`/`IAppReviewManager`
 * (which launches the Play in-app review flow from `HomeActivity` when the user opens a quote detail,
 * gated on [UserPreferences.shouldPromptRate]).
 *
 * On iOS the equivalent is StoreKit's `SKStoreReviewController`: the system decides whether to
 * actually show the rating sheet (and rate-limits it to a few prompts per year), so all the app does
 * is *request* a review at a natural moment. We mirror Android's trigger — navigating into a quote
 * detail — and apply the same shared [UserPreferences.shouldPromptRate] gate plus a once-per-session
 * guard so the request is only made occasionally.
 */
class AppReviewPrompter(private val prefs: UserPreferences) {

    /** Ensures we ask StoreKit at most once per app session, like Android's `rateDialogShown`. */
    private var requestedThisSession = false

    /**
     * Requests an App Store review prompt if appropriate for this session. Mirrors Android's
     * `HomeActivity`, which checks `shouldPromptRate()` before launching the review flow and then
     * sets a session flag so it is not shown again.
     */
    fun maybeRequestReview() {
        if (requestedThisSession) return
        if (!prefs.shouldPromptRate()) return
        requestedThisSession = true
        requestReview()
    }

    private fun requestReview() {
        val scene = UIApplication.sharedApplication.connectedScenes
            .firstOrNull { it is UIWindowScene } as? UIWindowScene
        if (scene != null) {
            SKStoreReviewController.requestReviewInScene(scene)
        } else {
            AppLogger.w("No active UIWindowScene; skipping App Store review request")
        }
    }
}
