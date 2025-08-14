package com.github.premnirmal.ticker.home

import android.app.Activity
import android.content.Context
import com.github.premnirmal.ticker.showDialog
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber

class AppReviewManager(@ApplicationContext context: Context) : IAppReviewManager {
    override fun launchReviewFlow(activity: Activity) {
        activity.showDialog("Launching app review flow triggered on dev build :)")
    }
}