package com.github.premnirmal.ticker.portfolio.search

import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.premnirmal.ticker.base.BaseFragment
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.isNetworkOnline
import com.github.premnirmal.ticker.getStatusBarHeight
import com.github.premnirmal.ticker.home.ChildFragment
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.network.StocksApi
import com.github.premnirmal.ticker.network.data.Suggestions.Suggestion
import com.github.premnirmal.ticker.news.NewsFeedActivity
import com.github.premnirmal.ticker.portfolio.search.SuggestionsAdapter.SuggestionClickListener
import com.github.premnirmal.ticker.showDialog
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import com.github.premnirmal.tickerwidget.R
import kotlinx.android.synthetic.main.fragment_search.fake_status_bar
import kotlinx.android.synthetic.main.fragment_search.recycler_view
import kotlinx.android.synthetic.main.fragment_search.search_view
import kotlinx.coroutines.launch
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

  @Inject internal lateinit var stocksApi: StocksApi
  @Inject internal lateinit var widgetDataProvider: WidgetDataProvider
  @Inject internal lateinit var stocksProvider: IStocksProvider
  private lateinit var adapter: SuggestionsAdapter
  override val simpleName: String = "SearchFragment"

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
    fake_status_bar.layoutParams.height = context!!.getStatusBarHeight()
    fake_status_bar.requestLayout()
    adapter = SuggestionsAdapter(this)
    recycler_view.layoutManager = LinearLayoutManager(activity)
    recycler_view.addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))
    recycler_view.adapter = adapter
    search_view.addTextChangedListener(this)

    savedInstanceState?.let { selectedWidgetId = it.getInt(ARG_WIDGET_ID, -1) }
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
      InAppMessage.showMessage(requireActivity(), getString(R.string.added_to_list, ticker))
    } else {
      requireActivity().showDialog(getString(R.string.already_in_portfolio, ticker))
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
    if (query.isNotEmpty()) {
      if (requireActivity().isNetworkOnline()) {
        lifecycleScope.launch {
          val suggestions = stocksApi.getSuggestions(query)
          if (suggestions.wasSuccessful) {
            adapter.setData(suggestions.data)
          } else {
            Timber.w(suggestions.error)
            InAppMessage.showMessage(requireActivity(), R.string.error_fetching_suggestions, error = true)
          }
        }
      } else {
        InAppMessage.showMessage(requireActivity(), R.string.no_network_message, error = true)
      }
    }
  }

  override fun onSuggestionClick(suggestion: Suggestion) {
    val ticker = suggestion.symbol
    val intent = Intent(requireContext(), NewsFeedActivity::class.java)
    intent.putExtra(NewsFeedActivity.TICKER, ticker)
    startActivity(intent)
    return
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
        AlertDialog.Builder(requireContext())
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