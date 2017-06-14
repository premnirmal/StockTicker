package com.github.premnirmal.ticker.portfolio

import android.os.Bundle
import com.github.premnirmal.ticker.base.BaseActivity
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.tickerwidget.R
import kotlinx.android.synthetic.main.activity_positions.doneButton
import kotlinx.android.synthetic.main.activity_positions.price
import kotlinx.android.synthetic.main.activity_positions.shares
import kotlinx.android.synthetic.main.activity_positions.skipButton
import kotlinx.android.synthetic.main.activity_positions.tickerName
import kotlinx.android.synthetic.main.activity_positions.toolbar
import javax.inject.Inject

/**
 * Created by premnirmal on 2/25/16.
 */
open class AddPositionActivity : BaseActivity() {

  companion object {
    const val TICKER = "TICKER"
  }

  @Inject lateinit internal var stocksProvider: IStocksProvider
  lateinit protected var ticker: String

  public override fun onCreate(savedInstanceState: Bundle?) {
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
    }
    val name = tickerName
    name.text = ticker

    doneButton.setOnClickListener { onDoneClicked() }

    skipButton.setOnClickListener { skip() }

    price.addTextChangedListener(PriceTextChangeListener())
  }

  protected open fun skip() {
    finish()
  }

  protected fun onDoneClicked() {
    val sharesView = shares
    val priceView = price
    val priceText = priceView.text.toString()
    val sharesText = sharesView.text.toString()
    if (!priceText.isEmpty() && !sharesText.isEmpty()) {
      val price = java.lang.Float.parseFloat(priceText)
      val shares = java.lang.Integer.parseInt(sharesText)
      stocksProvider.addPosition(ticker, shares, price)
    }
    finish()
  }
}