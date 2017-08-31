package com.github.premnirmal.ticker.settings

import android.preference.Preference

/**
 * Created by premnirmal on 3/22/17.
 */
internal open class DefaultPreferenceChangeListener : Preference.OnPreferenceChangeListener {

  override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean = false
}