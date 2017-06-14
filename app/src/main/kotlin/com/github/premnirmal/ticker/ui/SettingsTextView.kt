package com.github.premnirmal.ticker.ui

import android.animation.LayoutTransition
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.github.premnirmal.tickerwidget.R
import kotlinx.android.synthetic.main.layout_widget_setting.view.setting_subtitle
import kotlinx.android.synthetic.main.layout_widget_setting.view.setting_title

class SettingsTextView : LinearLayout {

  var editable: Boolean = false

  val editText: EditText
  val subtitleTextView: TextView

  constructor(context: Context) : this(context, null)

  constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(context, attrs,
      defStyleAttr, 0)

  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(
      context, attrs, defStyleAttr, defStyleRes) {
    orientation = VERTICAL
    val inflater = LayoutInflater.from(context)
    inflater.inflate(R.layout.layout_widget_setting, this, true)
    subtitleTextView = setting_subtitle
    editText = inflater.inflate(R.layout.edit_text, null) as EditText
    val pad = resources.getDimensionPixelSize(R.dimen.setting_padding)
    setPadding(pad, pad, pad, pad)
    layoutTransition = LayoutTransition()
    layoutTransition.setDuration(200L)
    attrs?.let {
      val array = context.obtainStyledAttributes(it, R.styleable.SettingsTextView)
      val title = array.getString(R.styleable.SettingsTextView_title_text)
      setTitle(title)
      val subtitle = array.getString(R.styleable.SettingsTextView_subtitle_text)
      setSubtitle(subtitle)
      array.recycle()
    }
  }

  fun setTitle(text: CharSequence?) {
    setting_title.text = text
  }

  fun setSubtitle(text: CharSequence?) {
    subtitleTextView.text = text
  }

  fun setIsEditable(isEditable: Boolean, callback: (s: String) -> Unit = {}) {
    if (isEditable != editable) {
      editable = isEditable
      if (editable) {
        removeView(subtitleTextView)
        editText.setText(subtitleTextView.text)
        addView(editText, 1)
        editText.setOnEditorActionListener { v, actionId, _ ->
          if (actionId == EditorInfo.IME_ACTION_DONE) {
            callback.invoke(v.text.toString())
            true
          } else {
            false
          }
        }
        editText.requestFocus()
      } else {
        editText.setOnEditorActionListener(null)
        editText.clearFocus()
        removeView(editText)
        addView(subtitleTextView, 1)
      }
    }
  }

  fun setTextColor(color: Int) {
    setting_title.setTextColor(color)
    subtitleTextView.setTextColor(color)
    editText.setTextColor(color)
  }
}