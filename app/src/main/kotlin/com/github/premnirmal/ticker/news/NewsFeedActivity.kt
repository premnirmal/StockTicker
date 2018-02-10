package com.github.premnirmal.ticker.news

import android.os.Bundle
import com.github.premnirmal.ticker.base.BaseActivity
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.network.NewsProvider
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.tickerwidget.R
import com.github.premnirmal.tickerwidget.R.string
import kotlinx.android.synthetic.main.activity_news_feed.change
import kotlinx.android.synthetic.main.activity_news_feed.description
import kotlinx.android.synthetic.main.activity_news_feed.equityValue
import kotlinx.android.synthetic.main.activity_news_feed.exchange
import kotlinx.android.synthetic.main.activity_news_feed.lastTradePrice
import kotlinx.android.synthetic.main.activity_news_feed.numShares
import kotlinx.android.synthetic.main.activity_news_feed.tickerName
import kotlinx.android.synthetic.main.activity_news_feed.toolbar
import javax.inject.Inject

class NewsFeedActivity : BaseActivity() {

  companion object {
    const val TICKER = "TICKER"
  }

  @Inject lateinit internal var stocksProvider: IStocksProvider
  @Inject lateinit var newsProvider: NewsProvider
  private lateinit var ticker: String

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_news_feed)
    Injector.appComponent.inject(this)
    val quote: Quote?
    if (intent.hasExtra(TICKER) && intent.getStringExtra(TICKER) != null) {
      ticker = intent.getStringExtra(TICKER)
      quote = stocksProvider.getStock(ticker)
      if (quote == null) {
        showErrorAndFinish()
      }
    } else {
      ticker = ""
      showErrorAndFinish()
      return
    }
    toolbar.title = ticker
    tickerName.text = quote!!.name
    lastTradePrice.text = quote.priceString()
    change.text = quote.changeString() + " (" + quote.changePercentString() + ")"
    exchange.text = quote.stockExchange
    numShares.text = quote.numSharesString()
    equityValue.text = quote.holdingsString()
    description.text = quote.description
  }

  private fun showErrorAndFinish() {
    InAppMessage.showToast(this, string.error_symbol)
    finish()
  }
}