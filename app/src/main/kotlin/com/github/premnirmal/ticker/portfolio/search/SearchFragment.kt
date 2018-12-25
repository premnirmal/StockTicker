package com.github.premnirmal.ticker.portfolio.search

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.premnirmal.ticker.base.BaseFragment
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.components.isNetworkOnline
import com.github.premnirmal.ticker.hideKeyboard
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.network.SimpleSubscriber
import com.github.premnirmal.ticker.network.SuggestionApi
import com.github.premnirmal.ticker.network.data.Suggestions.Suggestion
import com.github.premnirmal.ticker.portfolio.search.SuggestionsAdapter.SuggestionClickListener
import com.github.premnirmal.ticker.showDialog
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import com.github.premnirmal.tickerwidget.R
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_search.recycler_view
import kotlinx.android.synthetic.main.fragment_search.search_view
import timber.log.Timber
import javax.inject.Inject

class SearchFragment : BaseFragment(), SuggestionClickListener, TextWatcher {

  @Inject internal lateinit var suggestionApi: SuggestionApi
  @Inject internal lateinit var widgetDataProvider: WidgetDataProvider
  @Inject internal lateinit var stocksProvider: IStocksProvider
  private var disposable: Disposable? = null
  private lateinit var adapter: SuggestionsAdapter

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Injector.appComponent.inject(this)
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_search, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    adapter = SuggestionsAdapter(this)
    recycler_view.layoutManager = LinearLayoutManager(activity)
    recycler_view.addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))
    recycler_view.adapter = adapter
    search_view.addTextChangedListener(this)
  }

  override fun onPause() {
    hideKeyboard(search_view)
    super.onPause()
  }

  private fun addTickerToWidget(ticker: String, widgetId: Int) {
    val widgetData = widgetDataProvider.dataForWidgetId(widgetId)
    if (!widgetData.hasTicker(ticker)) {
      widgetData.addTicker(ticker)
      widgetDataProvider.broadcastUpdateWidget(widgetId)
      InAppMessage.showMessage(activity, getString(R.string.added_to_list, ticker))
    } else {
      activity!!.showDialog(getString(R.string.already_in_portfolio, ticker))
    }
  }

  override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
    // Do nothing.
  }

  override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
    // Do nothing.
  }

  override fun afterTextChanged(s: Editable) {
    val query = s.toString().trim { it <= ' ' }.replace(" ".toRegex(), "")
    if (!query.isEmpty()) {
      disposable?.dispose()

      if (activity!!.isNetworkOnline()) {
        val observable = suggestionApi.getSuggestions(query)
        disposable =
            bind(observable).map { (resultSet) -> resultSet?.result!! }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : SimpleSubscriber<List<Suggestion>?>() {
                  override fun onError(e: Throwable) {
                    Timber.w(e)
                    InAppMessage.showMessage(activity, R.string.error_fetching_suggestions)
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
        InAppMessage.showMessage(activity, R.string.no_network_message)
      }
    }
  }

  override fun onSuggestionClick(suggestion: Suggestion) {
    val ticker = suggestion.symbol
    if (widgetDataProvider.hasWidget()) {
      val widgetIds = widgetDataProvider.getAppWidgetIds()
      if (widgetIds.size > 1) {
        val widgets =
          widgetIds.map { widgetDataProvider.dataForWidgetId(it).widgetName() }.toTypedArray()
        AlertDialog.Builder(context!!).setTitle(R.string.select_widget)
            .setItems(widgets) { dialog, which ->
              val id = widgetIds[which]
              addTickerToWidget(ticker, id)
              dialog.dismiss()
            }.create().show()
      } else {
        addTickerToWidget(ticker, widgetIds.first())
      }
    } else {
      addTickerToWidget(ticker, WidgetDataProvider.INVALID_WIDGET_ID)
    }
  }
}