package com.github.premnirmal.ticker.settings

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.github.premnirmal.ticker.CustomTabs
import com.github.premnirmal.tickerwidget.BuildConfig
import com.github.premnirmal.tickerwidget.R

class FooterPreference(
  context: Context,
  attrs: AttributeSet
) : Preference(context, attrs) {

  override fun onBindViewHolder(holder: PreferenceViewHolder) {
    super.onBindViewHolder(holder)
    val view = holder.itemView
    val versionView = view.findViewById<TextView>(R.id.version)
    val vName = "v${BuildConfig.VERSION_NAME}"
    versionView.text = vName
    val githubLink = view.findViewById<View>(R.id.github_link)
    githubLink.setOnClickListener {
      CustomTabs.openTab(
          context, view.resources.getString(R.string.checkout_open_source)
      )
    }
  }
}