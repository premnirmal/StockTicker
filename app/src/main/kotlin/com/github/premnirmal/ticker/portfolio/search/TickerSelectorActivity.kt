package com.github.premnirmal.ticker.portfolio.search

import android.os.Bundle
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.text.Editable
import android.text.TextWatcher
import com.github.premnirmal.ticker.Tools
import com.github.premnirmal.ticker.base.BaseActivity
import com.github.premnirmal.ticker.components.CrashLogger
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.components.SimpleSubscriber
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.network.SuggestionApi
import com.github.premnirmal.ticker.network.data.Suggestions.Suggestion
import com.github.premnirmal.ticker.portfolio.search.SuggestionsAdapter.Callback
import com.github.premnirmal.tickerwidget.R
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_ticker_selector.recycler_view
import kotlinx.android.synthetic.main.activity_ticker_selector.search_view
import kotlinx.android.synthetic.main.activity_ticker_selector.toolbar
import javax.inject.Inject

/**
 * Created by premnirmal on 2/25/16.
 */
class TickerSelectorActivity : BaseActivity(), Callback, TextWatcher {

  @Inject
  lateinit internal var suggestionApi: SuggestionApi

  @Inject
  lateinit internal var stocksProvider: IStocksProvider

  internal var disposable: Disposable? = null

  private val adapter = SuggestionsAdapter(this@TickerSelectorActivity)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Injector.inject(this)
    setContentView(R.layout.activity_ticker_selector)
    toolbar.setNavigationOnClickListener {
      finish()
    }
    recycler_view.layoutManager = LinearLayoutManager(this@TickerSelectorActivity)
    recycler_view.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
    recycler_view.adapter = adapter

    search_view.addTextChangedListener(this@TickerSelectorActivity)
  }

  override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

  }

  override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

  }

  override fun afterTextChanged(s: Editable) {
    val query = s.toString().trim { it <= ' ' }.replace(" ".toRegex(), "")
    if (!query.isEmpty()) {
      disposable?.dispose()

      if (Tools.isNetworkOnline(applicationContext)) {
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
                if (result != null) {
                  val suggestionList = ArrayList(result)
                  suggestionList.add(0, Suggestion(query))
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
    if (!stocksProvider.getTickers().contains(ticker)) {
      stocksProvider.addStock(ticker)
      InAppMessage.showMessage(this@TickerSelectorActivity,
          getString(R.string.added_to_list, ticker))
    } else {
      showDialog(getString(R.string.already_in_portfolio, ticker))
    }
  }

}