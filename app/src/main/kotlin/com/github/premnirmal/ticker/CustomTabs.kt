package com.github.premnirmal.ticker

import android.content.Context
import android.graphics.PorterDuff.Mode.SRC_IN
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsIntent.SHARE_STATE_ON
import androidx.core.content.ContextCompat
import com.github.premnirmal.tickerwidget.R

object CustomTabs {

  fun openTab(
    context: Context,
    url: String
  ) {
    val closeButton = ContextCompat.getDrawable(context, R.drawable.ic_close)!!
    closeButton.setTint(ContextCompat.getColor(context, R.color.icon_tint))
    closeButton.setTintMode(SRC_IN)
    val customTabsIntent = CustomTabsIntent.Builder()
      .setShareState(SHARE_STATE_ON)
        .setShowTitle(true)
        .setCloseButtonIcon(closeButton.toBitmap())
        .setExitAnimations(context, android.R.anim.fade_in, android.R.anim.fade_out)
        .build()
    customTabsIntent.launchUrl(context, Uri.parse(url))
  }
}