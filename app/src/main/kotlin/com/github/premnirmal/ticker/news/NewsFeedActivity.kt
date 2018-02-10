package com.github.premnirmal.ticker.news

import android.content.Intent
import android.os.Bundle
import com.github.premnirmal.ticker.base.BaseActivity
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.network.NewsProvider
import com.github.premnirmal.ticker.network.SimpleSubscriber
import com.github.premnirmal.ticker.network.data.NewsFeed
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.portfolio.EditPositionActivity
import com.github.premnirmal.tickerwidget.R
import com.github.premnirmal.tickerwidget.R.string
import kotlinx.android.synthetic.main.activity_news_feed.change
import kotlinx.android.synthetic.main.activity_news_feed.description
import kotlinx.android.synthetic.main.activity_news_feed.edit_positions
import kotlinx.android.synthetic.main.activity_news_feed.equityValue
import kotlinx.android.synthetic.main.activity_news_feed.exchange
import kotlinx.android.synthetic.main.activity_news_feed.lastTradePrice
import kotlinx.android.synthetic.main.activity_news_feed.numShares
import kotlinx.android.synthetic.main.activity_news_feed.recycler_view
import kotlinx.android.synthetic.main.activity_news_feed.tickerName
import kotlinx.android.synthetic.main.activity_news_feed.toolbar
import timber.log.Timber
import javax.inject.Inject

class NewsFeedActivity : BaseActivity() {

  companion object {
    const val TICKER = "TICKER"
  }

  @Inject lateinit internal var stocksProvider: IStocksProvider
  @Inject lateinit var newsProvider: NewsProvider
  private lateinit var ticker: String
  private lateinit var quote: Quote
  private val adapter = NewsAdapter()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_news_feed)
    Injector.appComponent.inject(this)
    val q: Quote?
    if (intent.hasExtra(TICKER) && intent.getStringExtra(TICKER) != null) {
      ticker = intent.getStringExtra(TICKER)
      q = stocksProvider.getStock(ticker)
      if (q == null) {
        showErrorAndFinish()
      }
    } else {
      ticker = ""
      showErrorAndFinish()
      return
    }
    quote = q!!
    toolbar.title = ticker
    tickerName.text = quote.name
    lastTradePrice.text = quote.priceString()
    change.text = quote.changeString() + " (" + quote.changePercentString() + ")"
    exchange.text = quote.stockExchange
    numShares.text = quote.numSharesString()
    equityValue.text = quote.holdingsString()
    description.text = quote.description
    edit_positions.setOnClickListener {
      val intent = Intent(this, EditPositionActivity::class.java)
      intent.putExtra(EditPositionActivity.TICKER, quote.symbol)
      startActivity(intent)
    }
    recycler_view.adapter = adapter
    if (adapter.itemCount == 0) {
      bind(newsProvider.getNews(ticker)).subscribe(object : SimpleSubscriber<NewsFeed>() {
        override fun onNext(result: NewsFeed) {
          result.articles?.let { articles ->
            adapter.setItems(articles)
          }
        }

        override fun onError(e: Throwable) {
          Timber.w(e)
        }
      })
    }
  }

  override fun onResume() {
    super.onResume()
    numShares.text = quote.numSharesString()
    equityValue.text = quote.holdingsString()
  }

  private fun showErrorAndFinish() {
    InAppMessage.showToast(this, string.error_symbol)
    finish()
  }
}