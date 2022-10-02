package com.github.premnirmal.ticker.home

import android.app.Activity
import android.content.Context
import com.github.premnirmal.ticker.AppPreferences
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber

class AppReviewManager(@ApplicationContext private val context: Context, private val appPreferences: AppPreferences) : IAppReviewManager {

  private val manager: ReviewManager by lazy {
    ReviewManagerFactory.create(context)
  }

  override fun launchReviewFlow(activity: Activity) {
    manager.requestReviewFlow().addOnCompleteListener { task ->
      if (task.isSuccessful) {
        val reviewInfo = task.result
        manager.launchReviewFlow(activity, reviewInfo).addOnCompleteListener {
          appPreferences.userDidRate()
          Timber.i("Review left")
        }
      } else {
        Timber.w(task.exception, "Failed to request review")
      }
    }
  }
}