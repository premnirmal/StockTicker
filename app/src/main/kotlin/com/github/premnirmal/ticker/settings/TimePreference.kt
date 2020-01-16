package com.github.premnirmal.ticker.settings

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import androidx.preference.DialogPreference

/**
 * Created by premnirmal on 2/27/16.
 */
class TimePreference(
  context: Context,
  attrs: AttributeSet
) : DialogPreference(context, attrs) {

  var lastHour = 0
  var lastMinute = 0

  override fun onGetDefaultValue(
    a: TypedArray,
    index: Int
  ): Any? = a.getString(index)

  override fun onSetInitialValue(
    restoreValue: Boolean,
    defaultValue: Any?
  ) {
    super.onSetInitialValue(restoreValue, defaultValue)
    val time: String = if (restoreValue) {
      if (defaultValue == null) {
        getPersistedString("00:00")
      } else {
        getPersistedString(defaultValue.toString())
      }
    } else {
      defaultValue.toString()
    }
    lastHour = getHour(time)
    lastMinute = getMinute(time)
  }

  companion object {

    fun getHour(time: String): Int {
      val pieces = time.split(":".toRegex())
          .dropLastWhile { it.isEmpty() }
          .toTypedArray()
      return Integer.parseInt(pieces[0])
    }

    fun getMinute(time: String): Int {
      val pieces = time.split(":".toRegex())
          .dropLastWhile { it.isEmpty() }
          .toTypedArray()
      return Integer.parseInt(pieces[1])
    }
  }

}