package com.github.premnirmal.ticker.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import com.github.premnirmal.tickerwidget.R
import kotlinx.android.synthetic.main.layout_widget_setting.view.setting_edit_text
import kotlinx.android.synthetic.main.layout_widget_setting.view.setting_subtitle
import kotlinx.android.synthetic.main.layout_widget_setting.view.setting_title
import kotlinx.android.synthetic.main.layout_widget_setting.view.text_flipper

class SettingsTextView : LinearLayout {

  var editable: Boolean = false

  constructor(context: Context) : this(context, null)

  constructor(
    context: Context,
    attrs: AttributeSet?
  ) : this(context, attrs, 0)

  constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int
  ) : super(
      context, attrs,
      defStyleAttr
  ) {
    orientation = VERTICAL
    val inflater = LayoutInflater.from(context)
    inflater.inflate(R.layout.layout_widget_setting, this, true)
    val pad = resources.getDimensionPixelSize(R.dimen.setting_padding)
    setPadding(pad, pad, pad, pad)
    attrs?.let {
      val array = context.obtainStyledAttributes(it, R.styleable.SettingsTextView)
      val title = array.getString(R.styleable.SettingsTextView_title_text)
      setTitle(title)
      val subtitle = array.getString(R.styleable.SettingsTextView_subtitle_text)
      setSubtitle(subtitle)
      array.recycle()
    }
  }

  constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
    defStyleRes: Int
  ) : this(
      context, attrs, defStyleAttr
  )

  fun setTitle(text: CharSequence?) {
    setting_title.text = text
  }

  fun setSubtitle(text: CharSequence?) {
    setting_subtitle.text = text
  }

  fun setIsEditable(
    isEditable: Boolean,
    callback: (s: String) -> Unit = {}
  ) {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    if (isEditable != editable) {
      editable = isEditable
      if (editable) {
        text_flipper.displayedChild = 1
        setting_edit_text.setText(setting_subtitle.text)
        setting_edit_text.setSelection(setting_edit_text.text.length)
        setting_edit_text.setOnEditorActionListener { v, actionId, _ ->
          if (actionId == EditorInfo.IME_ACTION_DONE) {
            callback.invoke(v.text.toString())
            true
          } else {
            false
          }
        }
        setting_edit_text.requestFocus()
        imm.showSoftInput(setting_edit_text, 0)
      } else {
        setting_edit_text.setOnEditorActionListener(null)
        imm.hideSoftInputFromWindow(setting_edit_text.windowToken, 0)
        setting_edit_text.clearFocus()
        text_flipper.displayedChild = 2
      }
    }
  }

  fun setTextColor(color: Int) {
    setting_title.setTextColor(color)
    setting_subtitle.setTextColor(color)
    setting_edit_text.setTextColor(color)
  }
}