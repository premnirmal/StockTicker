package com.github.premnirmal.ticker.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import com.github.premnirmal.tickerwidget.R
import com.github.premnirmal.tickerwidget.databinding.LayoutWidgetSettingBinding

class SettingsTextView : LinearLayout {

  private val binding: LayoutWidgetSettingBinding
  private var editable: Boolean = false

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
    binding = LayoutWidgetSettingBinding.bind(this)
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
    binding.settingTitle.text = text
  }

  fun setSubtitle(text: CharSequence?) {
    binding.settingSubtitle.text = text
  }

  fun setIsEditable(
    isEditable: Boolean,
    callback: (s: String) -> Unit = {}
  ) {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    if (isEditable != editable) {
      editable = isEditable
      if (editable) {
        binding.textFlipper.displayedChild = 1
        binding.settingEditText.setText(binding.settingSubtitle.text)
        binding.settingEditText.setSelection(binding.settingEditText.text.length)
        binding.settingEditText.setOnEditorActionListener { v, actionId, _ ->
          if (actionId == EditorInfo.IME_ACTION_DONE) {
            callback.invoke(v.text.toString())
            true
          } else {
            false
          }
        }
        binding.settingEditText.requestFocus()
        imm.showSoftInput(binding.settingEditText, 0)
      } else {
        binding.settingEditText.setOnEditorActionListener(null)
        imm.hideSoftInputFromWindow(binding.settingEditText.windowToken, 0)
        binding.settingEditText.clearFocus()
        binding.textFlipper.displayedChild = 2
      }
    }
  }

  fun setTextColor(color: Int) {
    binding.settingTitle.setTextColor(color)
    binding.settingSubtitle.setTextColor(color)
    binding.settingEditText.setTextColor(color)
  }
}