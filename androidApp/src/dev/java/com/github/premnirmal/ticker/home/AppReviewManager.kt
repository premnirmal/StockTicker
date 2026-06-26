package com.github.premnirmal.ticker.home

import android.app.Activity
import android.content.Context
import com.github.premnirmal.ticker.showDialog
import timber.log.Timber

class AppReviewManager(context: Context) : IAppReviewManager {
    override fun launchReviewFlow(activity: Activity) {
        activity.showDialog("Launching app review flow triggered on dev build :)")
    }
}