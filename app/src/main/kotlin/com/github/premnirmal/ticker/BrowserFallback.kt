package com.github.premnirmal.ticker

import android.content.Context
import android.content.Intent
import android.net.Uri
import saschpe.android.customtabs.CustomTabsHelper.CustomTabFallback

class BrowserFallback : CustomTabFallback {

  override fun openUri(context: Context, uri: Uri) {
    val browserActivityIntent = Intent(Intent.ACTION_VIEW, uri)
    context.startActivity(browserActivityIntent)
  }
}