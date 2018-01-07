package com.github.premnirmal.ticker.news

import android.os.Bundle
import android.os.PersistableBundle
import com.github.premnirmal.ticker.base.BaseActivity
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.network.NewsApi
import com.github.premnirmal.tickerwidget.R
import javax.inject.Inject

class NewsFeedActivity : BaseActivity() {

  companion object {
    const val TICKER = "TICKER"
  }

  @Inject lateinit internal var stocksProvider: IStocksProvider
  @Inject lateinit var newsApi: NewsApi
  private lateinit var ticker: String

  override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
    super.onCreate(savedInstanceState, persistentState)
    setContentView(R.layout.activity_news_feed)
    Injector.appComponent.inject(this)
    if (intent.hasExtra(TICKER) && intent.getStringExtra(TICKER) != null) {
      ticker = intent.getStringExtra(TICKER)
    } else {
      ticker = ""
      InAppMessage.showToast(this, R.string.error_symbol)
      finish()
      return
    }
    val quote = stocksProvider.getStock(ticker)

  }
}