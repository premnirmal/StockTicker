package com.github.premnirmal.ticker.news

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.support.customtabs.CustomTabsIntent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.github.mikephil.charting.charts.LineChart
import com.github.premnirmal.ticker.base.BaseGraphActivity
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.components.isNetworkOnline
import com.github.premnirmal.ticker.model.IHistoryProvider
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.network.NewsProvider
import com.github.premnirmal.ticker.network.SimpleSubscriber
import com.github.premnirmal.ticker.network.data.DataPoint
import com.github.premnirmal.ticker.network.data.NewsArticle
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.portfolio.EditPositionActivity
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
import saschpe.android.customtabs.CustomTabsHelper
import saschpe.android.customtabs.WebViewFallback
import javax.inject.Inject

class NewsFeedActivity : BaseGraphActivity() {

  companion object {
    const val TICKER = "TICKER"
    private const val DATA_POINTS = "DATA_POINTS"
  }

  @Inject
  internal lateinit var stocksProvider: IStocksProvider
  @Inject
  internal lateinit var newsProvider: NewsProvider
  @Inject
  internal lateinit var historyProvider: IHistoryProvider
  private lateinit var ticker: String

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_news_feed)
    Injector.appComponent.inject(this)
    updateToolbar(toolbar)
    toolbar.setNavigationOnClickListener {
      finish()
    }
    graph_container.layoutParams.height = (resources.displayMetrics.widthPixels * 0.5625f).toInt()
    graph_container.requestLayout()
    setupGraphView(graphView)
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
    change.text = "${quote.changeStringWithSign()} ( ${quote.changePercentStringWithSign()})"
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
      val intent = Intent(this, EditPositionActivity::class.java)
      intent.putExtra(EditPositionActivity.TICKER, quote.symbol)
      startActivity(intent)
    }
    savedInstanceState?.let {
      dataPoints = it.getParcelableArrayList(DATA_POINTS)
    }
  }

  private fun fetchData() {
    if (isNetworkOnline()) {
      bind(historyProvider.getHistoricalDataShort(quote.symbol)).subscribe(
          object : SimpleSubscriber<List<DataPoint>>() {
            override fun onNext(result: List<DataPoint>) {
              dataPoints = result
              loadGraph(graphView)
            }

            override fun onError(e: Throwable) {
              progress.visibility = View.GONE
              graphView.setNoDataText(getString(R.string.graph_fetch_failed))
              InAppMessage.showMessage(this@NewsFeedActivity, getString(R.string.graph_fetch_failed))
            }
          })
    } else {
      progress.visibility = View.GONE
      graphView.setNoDataText(getString(R.string.graph_fetch_failed))
      InAppMessage.showMessage(this, getString(R.string.no_network_message))
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
        newsArticle.getSourceName()?.let { source ->
          sourceView.text = source
        }
        titleView.text = newsArticle.title
        subTitleView.text = newsArticle.description
        dateView.text = newsArticle.dateString()
        val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT)
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
          CustomTabsHelper.addKeepAliveExtra(this, customTabsIntent.intent)
          CustomTabsHelper.openCustomTab(this, customTabsIntent,
              Uri.parse(article.url), WebViewFallback())
        }
      }
    }
  }

  override fun onStart() {
    super.onStart()
    if (news_container.childCount <= 1) {
      fetchNews()
    }
    if (dataPoints == null) {
      fetchData()
    } else {
      loadGraph(graphView)
    }
  }

  override fun onResume() {
    super.onResume()
    numShares.text = quote.numSharesString()
    equityValue.text = quote.holdingsString()
    if (quote.isPosition) {
      total_gain_loss.visibility = View.VISIBLE
      total_gain_loss.setText("${quote.gainLossString()} (${quote.gainLossPercentString()})")
      if (quote.gainLoss() >= 0) {
        total_gain_loss.setTextColor(resources.getColor(R.color.positive_green))
      } else {
        total_gain_loss.setTextColor(resources.getColor(R.color.negative_red))
      }
      average_price.visibility = View.VISIBLE
      average_price.setText(quote.positionPriceString())
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

  private fun fetchNews() {
    if (isNetworkOnline()) {
      bind(newsProvider.getNews(quote.newsQuery())).subscribe(
          object : SimpleSubscriber<List<NewsArticle>>() {
            override fun onNext(result: List<NewsArticle>) {
              setUpArticles(result)
            }

            override fun onError(e: Throwable) {
              news_container.visibility = View.GONE
              InAppMessage.showMessage(this@NewsFeedActivity, getString(R.string.news_fetch_failed))
            }
          })
    } else {
      news_container.visibility = View.GONE
      InAppMessage.showMessage(this, getString(R.string.no_network_message))
    }
  }

  override fun onGraphDataAdded(graphView: LineChart) {
    progress.visibility = View.GONE
  }

  override fun onNoGraphData(graphView: LineChart) {
    progress.visibility = View.GONE
  }

  /**
   * Called via xml
   */
  fun openGraph(v: View) {
    val intent = Intent(this, GraphActivity::class.java)
    intent.putExtra(GraphActivity.TICKER, ticker)
    startActivity(intent)
  }

  private fun Drawable.toBitmap(): Bitmap {
    if (this is BitmapDrawable) {
      return bitmap
    }

    val width = if (bounds.isEmpty) intrinsicWidth else bounds.width()
    val height = if (bounds.isEmpty) intrinsicHeight else bounds.height()

    return Bitmap.createBitmap(width.nonZero(), height.nonZero(), Bitmap.Config.ARGB_8888).also {
      val canvas = Canvas(it)
      setBounds(0, 0, canvas.width, canvas.height)
      draw(canvas)
    }
  }

  private fun Int.nonZero() = if (this <= 0) 1 else this
}