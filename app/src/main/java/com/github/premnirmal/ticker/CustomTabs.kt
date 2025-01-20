package com.github.premnirmal.ticker

import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsIntent.SHARE_STATE_ON
import com.github.premnirmal.ticker.settings.WebViewActivity
import com.google.android.material.elevation.SurfaceColors
import timber.log.Timber

object CustomTabs {

  fun openTab(
    context: Context,
    url: String
  ) {
    try {
      val color = SurfaceColors.SURFACE_2.getColor(context)
      val customTabsIntent = CustomTabsIntent.Builder()
        .setShareState(SHARE_STATE_ON)
        .setShowTitle(true)
        .setDefaultColorSchemeParams(
          CustomTabColorSchemeParams.Builder().setToolbarColor(color).build()
        )
        .setExitAnimations(context, android.R.anim.fade_in, android.R.anim.fade_out)
        .build()
      customTabsIntent.launchUrl(context, Uri.parse(url))
    } catch (e: ActivityNotFoundException) {
      Timber.w(e)
      context.startActivity(WebViewActivity.newIntent(context, url))
    }
  }
}