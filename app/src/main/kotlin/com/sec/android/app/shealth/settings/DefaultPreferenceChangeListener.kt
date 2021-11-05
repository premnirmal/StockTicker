package com.sec.android.app.shealth.settings

/**
 * Created by android on 3/22/17.
 */
internal open class DefaultPreferenceChangeListener :
    android.preference.Preference.OnPreferenceChangeListener,
    androidx.preference.Preference.OnPreferenceChangeListener {

  override fun onPreferenceChange(
    preference: android.preference.Preference,
    newValue: Any
  ): Boolean = false

  override fun onPreferenceChange(
    preference: androidx.preference.Preference,
    newValue: Any
  ): Boolean = false
}