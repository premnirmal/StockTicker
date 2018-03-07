package com.github.premnirmal.ticker.ui

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import com.github.premnirmal.tickerwidget.R
import kotlinx.android.synthetic.main.stock_field_view.view.fieldname
import kotlinx.android.synthetic.main.stock_field_view.view.fieldvalue

/**
 * Created by premnirmal on 2/27/16.
 */
class StockFieldView @JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {

  init {
    LayoutInflater.from(context).inflate(R.layout.stock_field_view, this, true)
    attrs?.let {
      val array = context.obtainStyledAttributes(it, R.styleable.StockFieldView)
      val orientation = array.getInt(R.styleable.StockFieldView_or, 0)
      if (orientation == 0) {
        setOrientation(LinearLayout.HORIZONTAL)
        fieldname.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT,
            0.5f)
        fieldvalue.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT,
            0.5f)
        fieldvalue.gravity = Gravity.END
      } else {
        setOrientation(LinearLayout.VERTICAL)
        fieldname.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 0,
            0.5f)
        fieldvalue.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 0,
            0.5f)
        fieldvalue.gravity = Gravity.START
      }
      weightSum = 1f
      val name = getStringValue(context, array, R.styleable.StockFieldView_name)
      fieldname.text = name
      val textSize = array.getDimensionPixelSize(R.styleable.StockFieldView_size, 20).toFloat()
      fieldname.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
      fieldvalue.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize * 0.9f)
      val centerText = array.getBoolean(R.styleable.StockFieldView_center_text, false)
      if (centerText) {
        fieldname.gravity = Gravity.CENTER
        fieldvalue.gravity = Gravity.CENTER
      } else {
        fieldname.gravity = Gravity.LEFT
        fieldvalue.gravity = Gravity.RIGHT
      }
      array.recycle()
    }
  }

  constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : this(context, attrs)

  constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : this(
      context, attrs)

  fun setLabel(text: CharSequence?) {
    fieldname.text = text
  }

  fun setText(text: CharSequence?) {
    fieldvalue.text = text
  }

  fun setTextColor(color: Int) {
    fieldvalue.setTextColor(color)
  }

  private fun getStringValue(context: Context, array: TypedArray, stylelable: Int): String {
    var name = array.getString(stylelable)
    if (name == null) {
      val stringId = array.getResourceId(stylelable, -1)
      name = if (stringId > 0) {
        context.getString(stringId)
      } else {
        ""
      }
    }
    return name
  }

}