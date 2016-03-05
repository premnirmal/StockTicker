package com.github.premnirmal.ticker.ui

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.github.premnirmal.tickerwidget.R

/**
 * Created by premnirmal on 2/27/16.
 */
class StockFieldView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {

    private val fieldName: TextView
    private val fieldValue: TextView

    init {
        LayoutInflater.from(context).inflate(R.layout.stock_field_view, this, true)
        fieldName = findViewById(R.id.fieldname) as TextView
        fieldValue = findViewById(R.id.fieldvalue) as TextView
        if (attrs != null) {
            val array = getContext().obtainStyledAttributes(attrs, R.styleable.StockFieldView)
            val orientation = array.getInt(R.styleable.StockFieldView_or, 0)
            if (orientation == 0) {
                setOrientation(LinearLayout.HORIZONTAL)
                fieldName.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.5f)
                fieldValue.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.5f)
                fieldValue.gravity = Gravity.RIGHT
            } else {
                setOrientation(LinearLayout.VERTICAL)
                fieldName.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0.5f)
                fieldValue.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0.5f)
                fieldValue.gravity = Gravity.LEFT
            }
            weightSum = 1f
            val name = getStringValue(context, array, R.styleable.StockFieldView_name)
            fieldName.text = name
            val textSize = array.getDimensionPixelSize(R.styleable.StockFieldView_size, 20).toFloat()
            fieldName.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
            fieldValue.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize * 0.8f)
            array.recycle()
        }
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : this(context, attrs) {
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : this(context, attrs) {
    }

    fun setLabel(text: CharSequence?) {
        fieldName.text = text
    }

    fun setText(text: CharSequence?) {
        fieldValue.text = text
    }

    fun setTextColor(color: Int) {
        fieldValue.setTextColor(color)
    }

    private fun getStringValue(context: Context, array: TypedArray, stylelable: Int): String {
        var name = array.getString(stylelable)
        if (name == null) {
            val stringId = array.getResourceId(stylelable, -1)
            if (stringId > 0) {
                name = context.getString(stringId)
            } else {
                name = ""
            }
        }
        return name
    }

}