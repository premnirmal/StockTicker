package com.github.premnirmal.ticker.portfolio.search

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.animation.AnimatorListenerAdapter
import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewTreeObserver
import com.github.premnirmal.ticker.base.BaseActivity
import com.github.premnirmal.ticker.components.CrashLogger
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.components.SimpleSubscriber
import com.github.premnirmal.ticker.network.SuggestionApi
import com.github.premnirmal.ticker.network.data.Suggestions.Suggestion
import com.github.premnirmal.ticker.portfolio.search.SuggestionsAdapter.Callback
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import com.github.premnirmal.tickerwidget.R
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_ticker_selector.activity_root
import kotlinx.android.synthetic.main.activity_ticker_selector.recycler_view
import kotlinx.android.synthetic.main.activity_ticker_selector.search_view
import kotlinx.android.synthetic.main.activity_ticker_selector.toolbar
import javax.inject.Inject

/**
 * Created by premnirmal on 2/25/16.
 */
class TickerSelectorActivity : BaseActivity(), Callback, TextWatcher {

  companion object {
    val ARG_WIDGET_ID = "WIDGET_ID"

    fun launchIntent(context: Context, widgetId: Int): Intent {
      val intent = Intent(context, TickerSelectorActivity::class.java)
      intent.putExtra(ARG_WIDGET_ID, widgetId)
      return intent
    }
  }

  @Inject lateinit internal var suggestionApi: SuggestionApi

  @Inject lateinit internal var widgetDataProvider: WidgetDataProvider

  internal var disposable: Disposable? = null

  private var widgetId = 0

  private val adapter = SuggestionsAdapter(this@TickerSelectorActivity)

  override fun onCreate(savedInstanceState: Bundle?) {
    overridePendingTransition(0, 0)
    widgetId = intent.getIntExtra(ARG_WIDGET_ID, 0)
    super.onCreate(savedInstanceState)
    Injector.appComponent.inject(this)
    setContentView(R.layout.activity_ticker_selector)
    toolbar.setNavigationOnClickListener {
      onBackPressed()
    }
    recycler_view.layoutManager = LinearLayoutManager(this@TickerSelectorActivity)
    recycler_view.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
    recycler_view.adapter = adapter

    search_view.addTextChangedListener(this@TickerSelectorActivity)

    if (savedInstanceState == null && VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
      activity_root.visibility = View.INVISIBLE

      if (activity_root.viewTreeObserver.isAlive) {
        activity_root.viewTreeObserver
            .addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
              @TargetApi(VERSION_CODES.LOLLIPOP)
              @RequiresApi(VERSION_CODES.LOLLIPOP)
              override fun onGlobalLayout() {
                if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {
                  activity_root.viewTreeObserver.removeOnGlobalLayoutListener(this)
                } else {
                  activity_root.viewTreeObserver.removeGlobalOnLayoutListener(this)
                }
                doCircularReveal()
              }
            })
      }
    }
  }

  @TargetApi(VERSION_CODES.LOLLIPOP)
  @RequiresApi(VERSION_CODES.LOLLIPOP)
  fun doCircularReveal(reverse: Boolean = false, listener: AnimatorListener? = null) {
    val cx = intent.getIntExtra(EXTRA_CENTER_X, resources.displayMetrics.widthPixels)
    val cy = intent.getIntExtra(EXTRA_CENTER_Y, resources.displayMetrics.heightPixels)
    val finalRadius = Math.max(activity_root.width, activity_root.height)
    val circularRevealAnim = ViewAnimationUtils
        .createCircularReveal(activity_root, cx, cy,
            if (reverse) finalRadius.toFloat() else 0.toFloat(),
            if (reverse) 0.toFloat() else finalRadius.toFloat())
    circularRevealAnim.duration = resources.getInteger(
        android.R.integer.config_mediumAnimTime).toLong()
    activity_root.visibility = View.VISIBLE
    listener?.let { circularRevealAnim.addListener(it) }
    circularRevealAnim.start()
  }

  override fun finishAfterTransition() {
    if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
      doCircularReveal(true, object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator?) {
          activity_root.visibility = View.INVISIBLE
          finish()
          overridePendingTransition(0, 0)
        }
      })
    } else {
      super.finishAfterTransition()
    }
  }

  override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

  }

  override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

  }

  override fun afterTextChanged(s: Editable) {
    val query = s.toString().trim { it <= ' ' }.replace(" ".toRegex(), "")
    if (!query.isEmpty()) {
      disposable?.dispose()

      if (isNetworkOnline()) {
        val observable = suggestionApi.getSuggestions(query)
        disposable = bind(observable)
            .map { (resultSet) -> resultSet?.result }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(object : SimpleSubscriber<List<Suggestion>?>() {
              override fun onError(e: Throwable) {
                CrashLogger.logException(e)
                InAppMessage.showMessage(this@TickerSelectorActivity,
                    R.string.error_fetching_suggestions)
              }

              override fun onNext(result: List<Suggestion>?) {
                result?.let {
                  val suggestionList = ArrayList(it)
                  if (it.isEmpty()) {
                    suggestionList.add(0, Suggestion(query))
                  }
                  adapter.setData(suggestionList)
                }
              }
            })
      } else {
        InAppMessage.showMessage(this@TickerSelectorActivity, R.string.no_network_message)
      }
    }
  }

  override fun onSuggestionClick(suggestion: Suggestion) {
    val ticker = suggestion.symbol
    val widgetData = widgetDataProvider.dataForWidgetId(widgetId)
    if (!widgetData.getTickers().contains(ticker)) {
      widgetData.addTicker(ticker)
      widgetDataProvider.broadcastUpdateWidget(widgetId)
      InAppMessage.showMessage(this, getString(R.string.added_to_list, ticker))
    } else {
      showDialog(getString(R.string.already_in_portfolio, ticker))
    }
  }
}