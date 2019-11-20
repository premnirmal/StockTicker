package com.github.premnirmal.ticker.news

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.LineChart
import com.github.premnirmal.ticker.BrowserFallback
import com.github.premnirmal.ticker.analytics.ClickEvent
import com.github.premnirmal.ticker.analytics.GeneralEvent
import com.github.premnirmal.ticker.base.BaseGraphActivity
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.components.isNetworkOnline
import com.github.premnirmal.ticker.model.IHistoryProvider
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.network.NewsProvider
import com.github.premnirmal.ticker.network.data.NewsArticle
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.portfolio.AddPositionActivity
import com.github.premnirmal.ticker.toBitmap
import com.github.premnirmal.tickerwidget.R
import kotlinx.android.synthetic.main.activity_news_feed.average_price
import kotlinx.android.synthetic.main.activity_news_feed.change
import kotlinx.android.synthetic.main.activity_news_feed.day_change
import kotlinx.android.synthetic.main.activity_news_feed.description
import kotlinx.android.synthetic.main.activity_news_feed.edit_positions
import kotlinx.android.synthetic.main.activity_news_feed.equityValue
import kotlinx.android.synthetic.main.activity_news_feed.exchange
import kotlinx.android.synthetic.main.activity_news_feed.graphView
import kotlinx.android.synthetic.main.activity_news_feed.graph_container
import kotlinx.android.synthetic.main.activity_news_feed.lastTradePrice
import kotlinx.android.synthetic.main.activity_news_feed.news_container
import kotlinx.android.synthetic.main.activity_news_feed.numShares
import kotlinx.android.synthetic.main.activity_news_feed.progress
import kotlinx.android.synthetic.main.activity_news_feed.tickerName
import kotlinx.android.synthetic.main.activity_news_feed.toolbar
import kotlinx.android.synthetic.main.activity_news_feed.total_gain_loss
import kotlinx.coroutines.launch
import saschpe.android.customtabs.CustomTabsHelper
import javax.inject.Inject

class NewsFeedActivity : BaseGraphActivity() {

  companion object {
    const val TICKER = "TICKER"
    private const val DATA_POINTS = "DATA_POINTS"
  }

  @Inject internal lateinit var stocksProvider: IStocksProvider
  @Inject internal lateinit var newsProvider: NewsProvider
  @Inject internal lateinit var historyProvider: IHistoryProvider
  private lateinit var ticker: String
  override val simpleName: String = "NewsFeedActivity"

  override fun onCreate(savedInstanceState: Bundle?) {
    Injector.appComponent.inject(this)
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_news_feed)
    toolbar.setNavigationOnClickListener {
      finish()
    }
    if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
      graph_container.layoutParams.height = (resources.displayMetrics.widthPixels * 0.5625f).toInt()
      graph_container.requestLayout()
    }
    setupGraphView()
    val q: Quote?
    if (intent.hasExtra(TICKER) && intent.getStringExtra(TICKER) != null) {
      ticker = intent.getStringExtra(TICKER)
      q = stocksProvider.getStock(ticker)
      if (q == null) {
        showErrorAndFinish()
        return
      }
    } else {
      ticker = ""
      showErrorAndFinish()
      return
    }
    quote = q
    toolbar.title = ticker
    tickerName.text = quote.name
    lastTradePrice.text = quote.priceString()
    val changeText = "${quote.changeStringWithSign()} ( ${quote.changePercentStringWithSign()})"
    change.text = changeText
    if (quote.change > 0 || quote.changeInPercent >= 0) {
      change.setTextColor(resources.getColor(R.color.positive_green))
      lastTradePrice.setTextColor(resources.getColor(R.color.positive_green))
    } else {
      change.setTextColor(resources.getColor(R.color.negative_red))
      lastTradePrice.setTextColor(resources.getColor(R.color.negative_red))
    }
    exchange.text = quote.stockExchange
    numShares.text = quote.numSharesString()
    equityValue.text = quote.holdingsString()
    description.text = quote.description
    edit_positions.setOnClickListener {
      analytics.trackClickEvent(ClickEvent("EditPositionClick")
          .addProperty("Instrument", ticker))
      val intent = Intent(this, AddPositionActivity::class.java)
      intent.putExtra(AddPositionActivity.TICKER, quote.symbol)
      startActivity(intent)
    }
    savedInstanceState?.let {
      dataPoints = it.getParcelableArrayList(DATA_POINTS)
    }
  }

  private fun fetchData() {
    if (isNetworkOnline()) {
      lifecycleScope.launch {
        val result = historyProvider.getHistoricalDataShort(quote.symbol)
        if (result.wasSuccessful) {
          dataPoints = result.data
          loadGraph()
        } else {
          progress.visibility = View.GONE
          graphView.setNoDataText(getString(R.string.graph_fetch_failed))
          InAppMessage.showMessage(this@NewsFeedActivity, R.string.graph_fetch_failed, error = true)
        }
      }
    } else {
      progress.visibility = View.GONE
      graphView.setNoDataText(getString(R.string.no_network_message))
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    dataPoints?.let {
      outState.putParcelableArrayList(DATA_POINTS, ArrayList(it))
    }
  }

  private fun setUpArticles(articles: List<NewsArticle>) {
    if (articles.isEmpty()) {
      news_container.visibility = View.GONE
    } else {
      news_container.visibility = View.VISIBLE
      for (newsArticle in articles) {
        val layout = LayoutInflater.from(this)
            .inflate(R.layout.item_news, news_container, false)
        val sourceView: TextView = layout.findViewById(R.id.news_source)
        val titleView: TextView = layout.findViewById(R.id.news_title)
        val subTitleView: TextView = layout.findViewById(R.id.news_subtitle)
        val dateView: TextView = layout.findViewById(R.id.published_at)
        titleView.text = newsArticle.title
        subTitleView.text = newsArticle.description
        dateView.text = newsArticle.dateString()
        sourceView.text = newsArticle.sourceName()
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.bottomMargin = resources.getDimensionPixelSize(R.dimen.activity_vertical_margin)
        news_container.addView(layout, params)
        layout.tag = newsArticle
        layout.setOnClickListener {
          val article = it.tag as NewsArticle
          val customTabsIntent = CustomTabsIntent.Builder()
              .addDefaultShareMenuItem()
              .setToolbarColor(this.resources.getColor(R.color.colorPrimary))
              .setShowTitle(true)
              .setCloseButtonIcon(resources.getDrawable(R.drawable.ic_close).toBitmap())
              .build()
          analytics.trackClickEvent(ClickEvent("ArticleClick")
              .addProperty("Instrument", ticker)
              .addProperty("ArticleTitle", newsArticle.title.orEmpty())
              .addProperty("ArticleSource", newsArticle.sourceName())
              .addProperty("ArticleUrl", newsArticle.url.orEmpty()))
          CustomTabsHelper.addKeepAliveExtra(this, customTabsIntent.intent)
          CustomTabsHelper.openCustomTab(
              this, customTabsIntent, Uri.parse(article.url.orEmpty()),
              BrowserFallback()
          )
        }
      }
    }
  }

  override fun onStart() {
    super.onStart()
    fetch()
  }

  override fun onResume() {
    super.onResume()
    numShares.text = quote.numSharesString()
    equityValue.text = quote.holdingsString()
    if (quote.hasPositions()) {
      total_gain_loss.visibility = View.VISIBLE
      total_gain_loss.setText("${quote.gainLossString()} (${quote.gainLossPercentString()})")
      if (quote.gainLoss() >= 0) {
        total_gain_loss.setTextColor(resources.getColor(R.color.positive_green))
      } else {
        total_gain_loss.setTextColor(resources.getColor(R.color.negative_red))
      }
      average_price.visibility = View.VISIBLE
      average_price.setText(quote.averagePositionPrice())
      day_change.visibility = View.VISIBLE
      day_change.setText(quote.dayChangeString())
      if (quote.change > 0 || quote.changeInPercent >= 0) {
        day_change.setTextColor(resources.getColor(R.color.positive_green))
      } else {
        day_change.setTextColor(resources.getColor(R.color.negative_red))
      }
    } else {
      total_gain_loss.visibility = View.GONE
      day_change.visibility = View.GONE
      average_price.visibility = View.GONE
    }
  }

  private fun fetch() {
    if (!isNetworkOnline()) {
      InAppMessage.showMessage(this, R.string.no_network_message, error = true)
    }
    if (news_container.childCount <= 1) {
      fetchNews()
    }
    if (dataPoints == null) {
      fetchData()
    } else {
      loadGraph()
    }
  }

  private fun fetchNews() {
    if (isNetworkOnline()) {
      lifecycleScope.launch {
        val result = newsProvider.getNews(quote.newsQuery())
        if (result.wasSuccessful) {
          val articles = result.data
          analytics.trackGeneralEvent(GeneralEvent("FetchNews")
              .addProperty("Instrument", ticker)
              .addProperty("Success", "True"))
          setUpArticles(articles)
        } else {
          news_container.visibility = View.GONE
          InAppMessage.showMessage(this@NewsFeedActivity, R.string.news_fetch_failed, error = true)
          analytics.trackGeneralEvent(GeneralEvent("FetchNews")
              .addProperty("Instrument", ticker)
              .addProperty("Success", "False"))
        }
      }
    } else {
      news_container.visibility = View.GONE
    }
  }

  override fun onGraphDataAdded(graphView: LineChart) {
    progress.visibility = View.GONE
    analytics.trackGeneralEvent(GeneralEvent("GraphLoaded"))
  }

  override fun onNoGraphData(graphView: LineChart) {
    progress.visibility = View.GONE
    analytics.trackGeneralEvent(GeneralEvent("NoGraphData"))
  }

  /**
   * Called via xml
   */
  fun openGraph(v: View) {
    analytics.trackClickEvent(ClickEvent("GraphClick")
        .addProperty("Instrument", ticker))
    val intent = Intent(this, GraphActivity::class.java)
    intent.putExtra(GraphActivity.TICKER, ticker)
    startActivity(intent)
  }
}