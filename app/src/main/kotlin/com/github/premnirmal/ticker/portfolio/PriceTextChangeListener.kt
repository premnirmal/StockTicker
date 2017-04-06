package com.github.premnirmal.ticker.portfolio

import android.text.Editable
import android.text.Selection
import android.text.TextWatcher


internal class PriceTextChangeListener : TextWatcher {

  /**
   * Indicates the change was caused by ourselves.
   */
  private var wasSelfChange = false

  /**
   * Indicates the formatting has been stopped.
   */
  private var formattingStopped: Boolean = false

  override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
    if (wasSelfChange || formattingStopped) {
      return
    }
    // If the user manually deleted any non-dialable characters, stop formatting
    if (count > 0 && hasSeparator(s, start, count)) {
      formattingStopped = true
    }
  }

  override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
    if (wasSelfChange || formattingStopped) {
      return
    }
    // If the user inserted any non-dialable characters, stop formatting
    if (count > 0 && hasSeparator(s, start, count)) {
      formattingStopped = true
    }
  }

  override fun afterTextChanged(s: Editable) {
    if (formattingStopped) {
      // Restart the formatting when all texts were clear.
      formattingStopped = s.isNotEmpty()
      return
    }
    if (wasSelfChange) {
      // Ignore the change caused by s.replace().
      return
    }
    val formatted = reformat(s, Selection.getSelectionEnd(s))
    wasSelfChange = true
    s.replace(0, s.length, formatted, 0, formatted.length)
    // The text could be changed by other TextWatcher after we changed it. If we found the
    // text is not the one we were expecting, just give up calling setSelection().
    if (formatted == s.toString()) {
      Selection.setSelection(s, s.length)
    }
    wasSelfChange = false
  }

  private fun reformat(s: CharSequence, cursor: Int): String {

    // first strip out all non digit characters
    val nextStr = StringBuilder(s.toString().replace(".", ""))

    // remove all prepending 0's
    while (nextStr.isNotEmpty() && nextStr[0] == '0') {
      nextStr.replace(0, 1, "")
    }

    while (nextStr.length < 4) {
      nextStr.insert(0, "0")
    }

    // insert a decimal at 3'rd to last position
    val decimalIndex = nextStr.length - 2
    nextStr.insert(decimalIndex, ".")

    return nextStr.toString()
  }

  private fun hasSeparator(s: CharSequence, start: Int, count: Int): Boolean {
    return (start..start + count - 1)
        .map { s[it] }
        .none { isNonSeparator(it) }
  }

  private fun isNonSeparator(c: Char): Boolean {
    return c in '0'..'9' || c == '.'
  }
}

