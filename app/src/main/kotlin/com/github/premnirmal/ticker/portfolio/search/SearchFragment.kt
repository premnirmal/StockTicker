package com.github.premnirmal.ticker.portfolio.search

import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.premnirmal.ticker.base.BaseFragment
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.components.isNetworkOnline
import com.github.premnirmal.ticker.getStatusBarHeight
import com.github.premnirmal.ticker.hideKeyboard
import com.github.premnirmal.ticker.home.ChildFragment
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
import kotlinx.android.synthetic.main.fragment_search.*
import timber.log.Timber
import javax.inject.Inject

class SearchFragment : BaseFragment(), ChildFragment, SuggestionClickListener, TextWatcher {

  companion object {
    const val ARG_WIDGET_ID = AppWidgetManager.EXTRA_APPWIDGET_ID

    fun newInstance(widgetId: Int): SearchFragment {
      val fragment = SearchFragment()
      val args = Bundle()
      args.putInt(ARG_WIDGET_ID, widgetId)
      fragment.arguments = args
      return fragment
    }
  }

  @Inject internal lateinit var suggestionApi: SuggestionApi
  @Inject internal lateinit var widgetDataProvider: WidgetDataProvider
  @Inject internal lateinit var stocksProvider: IStocksProvider
  private var disposable: Disposable? = null
  private lateinit var adapter: SuggestionsAdapter

  private var selectedWidgetId: Int = -1

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Injector.appComponent.inject(this)
    arguments?.let {
      setData(it)
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_search, container, false)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    (toolbar.layoutParams as MarginLayoutParams).topMargin = context!!.getStatusBarHeight()
    adapter = SuggestionsAdapter(this)
    recycler_view.layoutManager = LinearLayoutManager(activity)
    recycler_view.addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))
    recycler_view.adapter = adapter
    search_view.addTextChangedListener(this)

    savedInstanceState?.let { selectedWidgetId = it.getInt(ARG_WIDGET_ID, -1) }
  }

  override fun onPause() {
    hideKeyboard(search_view)
    super.onPause()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    outState.putInt(ARG_WIDGET_ID, selectedWidgetId)
    super.onSaveInstanceState(outState)
  }

  private fun addTickerToWidget(
    ticker: String,
    widgetId: Int
  ) {
    val widgetData = widgetDataProvider.dataForWidgetId(widgetId)
    if (!widgetData.hasTicker(ticker)) {
      widgetData.addTicker(ticker)
      widgetDataProvider.broadcastUpdateWidget(widgetId)
      InAppMessage.showMessage(activity, getString(R.string.added_to_list, ticker))
    } else {
      activity!!.showDialog(getString(R.string.already_in_portfolio, ticker))
    }
  }

  override fun beforeTextChanged(
    s: CharSequence,
    start: Int,
    count: Int,
    after: Int
  ) {
    // Do nothing.
  }

  override fun onTextChanged(
    s: CharSequence,
    start: Int,
    before: Int,
    count: Int
  ) {
    // Do nothing.
  }

  override fun afterTextChanged(s: Editable) {
    val query = s.toString()
        .trim { it <= ' ' }
        .replace(" ".toRegex(), "")
    if (!query.isEmpty()) {
      disposable?.dispose()

      if (activity!!.isNetworkOnline()) {
        val observable = suggestionApi.getSuggestions(query)
        disposable =
          bind(observable).map { (resultSet) -> resultSet?.result!! }
              .subscribeOn(Schedulers.io())
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
    if (selectedWidgetId > 0) {
      addTickerToWidget(ticker, selectedWidgetId)
      return
    }
    if (widgetDataProvider.hasWidget()) {
      val widgetIds = widgetDataProvider.getAppWidgetIds()
      if (widgetIds.size > 1) {
        val widgets =
          widgetIds.map { widgetDataProvider.dataForWidgetId(it) }
              .sortedBy { it.widgetName() }
        val widgetNames = widgets.map { it.widgetName() }
            .toTypedArray()
        AlertDialog.Builder(context!!)
            .setTitle(R.string.select_widget)
            .setItems(widgetNames) { dialog, which ->
              val id = widgets[which].widgetId
              addTickerToWidget(ticker, id)
              dialog.dismiss()
            }
            .create()
            .show()
      } else {
        addTickerToWidget(ticker, widgetIds.first())
      }
    } else {
      addTickerToWidget(ticker, WidgetDataProvider.INVALID_WIDGET_ID)
    }
  }

  // ChildFragment

  override fun setData(bundle: Bundle) {
    selectedWidgetId = bundle.getInt(ARG_WIDGET_ID, -1)
  }
}