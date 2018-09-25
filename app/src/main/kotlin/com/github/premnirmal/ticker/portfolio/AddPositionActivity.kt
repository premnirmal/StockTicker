package com.github.premnirmal.ticker.portfolio

import android.os.Bundle
import android.text.InputFilter
import android.text.Spanned
import android.widget.TextView
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.base.BaseActivity
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.network.data.Holding
import com.github.premnirmal.tickerwidget.R
import kotlinx.android.synthetic.main.activity_positions.addButton
import kotlinx.android.synthetic.main.activity_positions.positionsHolder
import kotlinx.android.synthetic.main.activity_positions.price
import kotlinx.android.synthetic.main.activity_positions.priceInputLayout
import kotlinx.android.synthetic.main.activity_positions.shares
import kotlinx.android.synthetic.main.activity_positions.sharesInputLayout
import kotlinx.android.synthetic.main.activity_positions.tickerName
import kotlinx.android.synthetic.main.activity_positions.toolbar
import java.util.regex.Pattern
import javax.inject.Inject

/**
 * Created by premnirmal on 2/25/16.
 */
open class AddPositionActivity : BaseActivity() {

  companion object {
    const val TICKER = "TICKER"
    private val PATTERN: Pattern = Pattern.compile(
        "[0-9]{0," + (5) + "}+((\\.[0-9]{0," + (1) + "})?)||(\\.)?")

    private class DecimalDigitsInputFilter() : InputFilter {

      override fun filter(source: CharSequence, start: Int, end: Int, dest: Spanned, dStart: Int,
          dEnd: Int): CharSequence? {
        val matcher = PATTERN.matcher(dest)
        return if (!matcher.matches()) "" else null
      }
    }
  }

  @Inject internal lateinit var stocksProvider: IStocksProvider
  internal lateinit var ticker: String

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Injector.appComponent.inject(this)
    setContentView(R.layout.activity_positions)
    toolbar.setTitle(R.string.add_position)
    updateToolbar(toolbar)
    toolbar.setNavigationOnClickListener {
      finish()
    }
    if (intent.hasExtra(TICKER) && intent.getStringExtra(TICKER) != null) {
      ticker = intent.getStringExtra(TICKER)
    } else {
      ticker = ""
      InAppMessage.showToast(this, R.string.error_symbol)
      finish()
      return
    }
    val name = tickerName
    name.text = ticker

    addButton.setOnClickListener { onAddClicked() }

    price.filters = arrayOf<InputFilter>(DecimalDigitsInputFilter())
    shares.filters = arrayOf<InputFilter>(DecimalDigitsInputFilter())

    positionsHolder.removeAllViews()
    val position = stocksProvider.getPosition(ticker)
    position?.let {
      for (holding in position.holdings) {
        addPositionView(holding)
      }
    }
  }

  private fun onAddClicked() {
    val sharesView = shares
    val priceView = price
    val priceText = priceView.text.toString()
    val sharesText = sharesView.text.toString()
    if (!priceText.isEmpty() && !sharesText.isEmpty()) {
      var price = 0f
      var shares = 0f
      var success = true
      try {
        price = priceText.toFloat()
      } catch (e: NumberFormatException) {
        priceInputLayout.error = getString(R.string.invalid_number)
        success = false
      }
      try {
        shares = sharesText.toFloat()
      } catch (e: NumberFormatException) {
        sharesInputLayout.error = getString(R.string.invalid_number)
        success = false
      }
      if (success) {
        priceInputLayout.error = null
        sharesInputLayout.error = null
        val holding = stocksProvider.addPosition(ticker, shares, price)
        priceView.setText("")
        sharesView.setText("")
        addPositionView(holding)
      }
    }
  }

  private fun addPositionView(holding: Holding) {
    val view = layoutInflater.inflate(R.layout.layout_position_holding, null)
    val positionText = view.findViewById<TextView>(R.id.position_text)
    positionText.text = holding.holdingsText()
    positionsHolder.addView(view)
    view.tag = holding
    view.setOnClickListener {
      stocksProvider.removePosition(ticker, holding)
      positionsHolder.removeView(view)
    }
  }
}