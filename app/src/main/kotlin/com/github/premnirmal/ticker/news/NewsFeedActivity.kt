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
import com.github.premnirmal.ticker.base.BaseActivity
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.network.NewsProvider
import com.github.premnirmal.ticker.network.SimpleSubscriber
import com.github.premnirmal.ticker.network.data.NewsArticle
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
  }

  @Inject
  lateinit internal var stocksProvider: IStocksProvider
  @Inject
  lateinit var newsProvider: NewsProvider
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
    change.text = quote.changeString() + " (" + quote.changePercentString() + ")"
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
  }

  private fun setUpArticles(articles: List<NewsArticle>) {
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
      news_container.setOnClickListener {
        val customTabsIntent = CustomTabsIntent.Builder()
            .addDefaultShareMenuItem()
            .setToolbarColor(this.resources.getColor(R.color.colorPrimary))
            .setShowTitle(true)
            .setCloseButtonIcon(resources.getDrawable(R.drawable.ic_close).toBitmap())
            .build()
        CustomTabsHelper.addKeepAliveExtra(this, customTabsIntent.intent)
        CustomTabsHelper.openCustomTab(this, customTabsIntent,
            Uri.parse(newsArticle.url), WebViewFallback())
      }
    }
  }

  override fun onResume() {
    super.onResume()
    numShares.text = quote.numSharesString()
    equityValue.text = quote.holdingsString()
    if (quote.isPosition) {
      total_gain_loss.visibility = View.VISIBLE
      total_gain_loss.setText(quote.gainLossString())
      if (quote.gainLoss() >= 0) {
        total_gain_loss.setTextColor(resources.getColor(R.color.positive_green))
      } else {
        total_gain_loss.setTextColor(resources.getColor(R.color.negative_red))
      }
    } else {
      total_gain_loss.visibility = View.GONE
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