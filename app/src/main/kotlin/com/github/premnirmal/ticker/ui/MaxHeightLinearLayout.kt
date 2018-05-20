package com.github.premnirmal.ticker.ui

import android.content.Context
import android.content.res.Resources
import android.util.AttributeSet
import android.widget.LinearLayout
import com.github.premnirmal.tickerwidget.R

class MaxHeightLinearLayout : LinearLayout {

  private var maxHeightDp: Int = 0

  constructor(context: Context) : super(context)

  constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
    val attributeSet = context.theme.obtainStyledAttributes(attrs,
        R.styleable.MaxHeightLinearLayout, 0, 0)
    try {
      maxHeightDp = attributeSet.getInteger(R.styleable.MaxHeightLinearLayout_maxHeightDp, 0)
    } finally {
      attributeSet.recycle()
    }
  }

  constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : this(context, attrs)

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val maxHeightPx = maxHeightDp.px
    val heightMeasure = MeasureSpec.makeMeasureSpec(maxHeightPx, MeasureSpec.AT_MOST)
    setMeasuredDimension(measuredWidth, maxHeightPx)
    super.onMeasure(widthMeasureSpec, heightMeasure)
  }

  private val Int.px: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()
}