package com.github.premnirmal.ticker.ui

import android.content.Context
import android.os.Build.VERSION_CODES
import android.support.annotation.RequiresApi
import android.util.AttributeSet
import android.widget.LinearLayout
import com.github.premnirmal.tickerwidget.R

class MaxHeightLinearLayout(context: Context, attrs: AttributeSet? = null)
  : LinearLayout(context, attrs) {

  var maxHeight: Int = -1

  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(context, attrs)

  @RequiresApi(VERSION_CODES.LOLLIPOP)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int)
      : this(context, attrs)

  init {
    attrs?.let {
      val array = context.obtainStyledAttributes(it, R.styleable.MaxHeightLinearLayout)
      this.maxHeight = array.getDimensionPixelSize(R.styleable.MaxHeightLinearLayout_maxheight, -1)
      array.recycle()
    }
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, maxHeight)
  }
}