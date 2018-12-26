package com.github.premnirmal.ticker.settings

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.browser.customtabs.CustomTabsIntent
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.github.premnirmal.ticker.toBitmap
import com.github.premnirmal.tickerwidget.BuildConfig
import com.github.premnirmal.tickerwidget.R
import saschpe.android.customtabs.CustomTabsHelper
import saschpe.android.customtabs.WebViewFallback

class FooterPreference(context: Context, attrs: AttributeSet) : Preference(context, attrs) {

  override fun onBindViewHolder(holder: PreferenceViewHolder) {
    super.onBindViewHolder(holder)
    val view = holder.itemView
    val versionView = view.findViewById<TextView>(R.id.version)
    val vName = "v${BuildConfig.VERSION_NAME}"
    versionView.text = vName
    val githubLink = view.findViewById<View>(R.id.github_link)
    githubLink.setOnClickListener {
      val customTabsIntent = CustomTabsIntent.Builder().addDefaultShareMenuItem()
          .setToolbarColor(view.resources.getColor(R.color.colorPrimary)).setShowTitle(true)
          .setCloseButtonIcon(view.resources.getDrawable(R.drawable.ic_close).toBitmap()).build()
      CustomTabsHelper.addKeepAliveExtra(context, customTabsIntent.intent)
      CustomTabsHelper.openCustomTab(context, customTabsIntent,
          Uri.parse(view.resources.getString(R.string.checkout_open_source)), WebViewFallback())
    }
  }
}