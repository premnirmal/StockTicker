package com.github.premnirmal.ticker.news

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
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
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.premnirmal.ticker.base.BaseActivity
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.model.DataPoint
import com.github.premnirmal.ticker.model.IHistoryProvider
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.network.NewsProvider
import com.github.premnirmal.ticker.network.SimpleSubscriber
import com.github.premnirmal.ticker.network.data.NewsArticle
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.portfolio.EditPositionActivity
import com.github.premnirmal.ticker.ui.TextMarkerView
import com.github.premnirmal.tickerwidget.R
import com.github.premnirmal.tickerwidget.R.string
import kotlinx.android.synthetic.main.activity_news_feed.average_price
import kotlinx.android.synthetic.main.activity_news_feed.change
import kotlinx.android.synthetic.main.activity_news_feed.description
import kotlinx.android.synthetic.main.activity_news_feed.edit_positions
import kotlinx.android.synthetic.main.activity_news_feed.equityValue
import kotlinx.android.synthetic.main.activity_news_feed.exchange
import kotlinx.android.synthetic.main.activity_news_feed.graphView
import kotlinx.android.synthetic.main.activity_news_feed.lastTradePrice
import kotlinx.android.synthetic.main.activity_news_feed.news_container
import kotlinx.android.synthetic.main.activity_news_feed.numShares
import kotlinx.android.synthetic.main.activity_news_feed.tickerName
import kotlinx.android.synthetic.main.activity_news_feed.toolbar
import kotlinx.android.synthetic.main.activity_news_feed.total_gain_loss
import saschpe.android.customtabs.CustomTabsHelper
import saschpe.android.customtabs.WebViewFallback
import timber.log.Timber
import javax.inject.Inject


class NewsFeedActivity : BaseActivity() {

  companion object {
    const val TICKER = "TICKER"
    const val DATA_POINTS = "DATA_POINTS"
    const val DURATION = 2000
  }

  @Inject
  internal lateinit var stocksProvider: IStocksProvider
  @Inject
  internal lateinit var newsProvider: NewsProvider
  @Inject
  internal lateinit var historyProvider: IHistoryProvider
  private var dataPoints: List<DataPoint>? = null
  private lateinit var ticker: String
  private lateinit var quote: Quote

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_news_feed)
    Injector.appComponent.inject(this)
    updateToolbar(toolbar)
    toolbar.setNavigationOnClickListener {
      finish()
    }
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
    change.text = "${quote.changeStringWithSign()} ( ${quote.changePercentStringWithSign()})"
    if (quote.changeInPercent >= 0) {
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

    if (news_container.childCount <= 1) {
      bind(newsProvider.getNews(quote.newsQuery())).subscribe(
          object : SimpleSubscriber<List<NewsArticle>>() {
            override fun onNext(result: List<NewsArticle>) {
              setUpArticles(result)
            }

            override fun onError(e: Throwable) {
              Timber.w(e)
            }
          })
    }
    graphView.isDoubleTapToZoomEnabled = false
    graphView.axisLeft.setDrawGridLines(false)
    graphView.axisLeft.setDrawAxisLine(false)
    graphView.axisLeft.isEnabled = false
    graphView.axisRight.setDrawGridLines(false)
    graphView.axisRight.setDrawAxisLine(true)
    graphView.axisRight.isEnabled = true
    graphView.xAxis.setDrawGridLines(false)
    graphView.legend.isEnabled = false
    graphView.marker = TextMarkerView(this, graphView)
    savedInstanceState?.let {
      dataPoints = it.getParcelableArrayList(DATA_POINTS)
      setupGraphData()
    }
    if (dataPoints == null) {
      bind(historyProvider.getHistoricalData(quote.symbol)).subscribe(
          object: SimpleSubscriber<List<DataPoint>>() {
            override fun onNext(result: List<DataPoint>) {
              dataPoints = result
              setupGraphData()
            }

            override fun onError(e: Throwable) {
              Timber.w(e)
            }
          })
    }
  }

  private fun setupGraphData() {
    if (dataPoints == null || dataPoints!!.isEmpty()) {
      return
    }
    graphView.lineData?.clearValues()
    graphView.invalidate()
//    desc.text = ticker.name
    val series = LineDataSet(dataPoints!!, "data")
    series.setDrawHorizontalHighlightIndicator(false)
    series.setDrawValues(false)
    val colorAccent = resources.getColor(R.color.color_accent)
    series.setDrawFilled(true)
    series.color = colorAccent
    series.fillColor = colorAccent
    series.fillAlpha = 150
    series.setDrawCircles(true)
    series.mode = LineDataSet.Mode.CUBIC_BEZIER
    series.cubicIntensity = 0.07f
    series.lineWidth = 2f
    series.setDrawCircles(false)
    series.highLightColor = Color.GRAY
    val dataSets: MutableList<ILineDataSet> = ArrayList()
    dataSets.add(series)
    val lineData = LineData(dataSets)
    graphView.data = lineData
    val xAxis: XAxis = graphView.xAxis
    val yAxis: YAxis = graphView.axisRight
    xAxis.position = XAxis.XAxisPosition.BOTTOM
    xAxis.textSize = 10f
    yAxis.textSize = 10f
    xAxis.textColor = Color.GRAY
    yAxis.textColor = Color.GRAY
    xAxis.setLabelCount(5, true)
    yAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
    xAxis.setDrawAxisLine(true)
    yAxis.setDrawAxisLine(true)
    xAxis.setDrawGridLines(false)
    yAxis.setDrawGridLines(false)
//    graph_holder.visibility = View.VISIBLE
//    progress.visibility = View.GONE
    graphView.animateX(DURATION, Easing.EasingOption.EaseInOutQuad)
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
    } else {
      total_gain_loss.visibility = View.GONE
      average_price.visibility = View.GONE
    }
  }

  private fun showErrorAndFinish() {
    InAppMessage.showToast(this, string.error_symbol)
    finish()
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