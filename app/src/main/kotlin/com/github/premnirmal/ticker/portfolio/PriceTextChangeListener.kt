package com.github.premnirmal.ticker.portfolio

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

internal class PriceTextChangeListener(private val editText: EditText) : TextWatcher {

  override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

  }

  override fun afterTextChanged(s: Editable?) {

  }

  override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
    if (!s.toString().matches("^\\$(\\d{1,3}(,\\d{3})*|(\\d+))(\\.\\d{2})?$".toRegex())) {
      val userInput = "" + s.toString().replace("[^\\d]".toRegex(), "")
      val amountBuilder = StringBuilder(userInput)

      while (amountBuilder.length > 3 && amountBuilder[0] == '0') {
        amountBuilder.deleteCharAt(0)
      }
      while (amountBuilder.length < 3) {
        amountBuilder.insert(0, '0')
      }
      amountBuilder.insert(amountBuilder.length - 2, '.')
      amountBuilder.insert(0, '$')

      editText.setText(amountBuilder.toString())
    }
  }
}