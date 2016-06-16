package com.github.premnirmal.ticker.portfolio

import android.os.Bundle
import com.github.premnirmal.ticker.BaseActivity
import com.github.premnirmal.ticker.Injector
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.tickerwidget.R
import kotlinx.android.synthetic.main.activity_positions.*
import javax.inject.Inject

/**
 * Created by premnirmal on 2/25/16.
 */
open class AddPositionActivity : BaseActivity() {

  companion object {
    const val TICKER = "TICKER"
  }

  @Inject
  lateinit internal var stocksProvider: IStocksProvider
  protected var ticker: String? = null

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Injector.getAppComponent().inject(this)
    setContentView(R.layout.activity_positions)
    updateToolbar(toolbar)
    toolbar.setNavigationOnClickListener {
      finish()
    }

    ticker = intent.getStringExtra(TICKER)
    val name = tickerName
    name.text = ticker

    doneButton.setOnClickListener { onDoneClicked() }

    skipButton.setOnClickListener { skip() }
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