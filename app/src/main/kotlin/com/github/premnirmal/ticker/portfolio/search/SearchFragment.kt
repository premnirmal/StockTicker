package com.github.premnirmal.ticker.portfolio.search

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.premnirmal.ticker.base.BaseFragment
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.getStatusBarHeight
import com.github.premnirmal.ticker.home.ChildFragment
import com.github.premnirmal.ticker.isNetworkOnline
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.network.StocksApi
import com.github.premnirmal.ticker.network.data.Suggestion
import com.github.premnirmal.ticker.news.QuoteDetailActivity
import com.github.premnirmal.ticker.portfolio.search.SuggestionsAdapter.SuggestionClickListener
import com.github.premnirmal.ticker.showDialog
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import com.github.premnirmal.tickerwidget.R
import kotlinx.android.synthetic.main.fragment_search.fake_status_bar
import kotlinx.android.synthetic.main.fragment_search.recycler_view
import kotlinx.android.synthetic.main.fragment_search.search_view
import kotlinx.android.synthetic.main.fragment_search.toolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

class SearchFragment : BaseFragment(), ChildFragment, SuggestionClickListener, TextWatcher {

  companion object {
    private const val ARG_WIDGET_ID = AppWidgetManager.EXTRA_APPWIDGET_ID
    private const val ARG_SHOW_NAV_ICON = "SHOW_NAV_ICON"

    fun newInstance(widgetId: Int, showNavIcon: Boolean = false): SearchFragment {
      val fragment = SearchFragment()
      val args = Bundle()
      args.putInt(ARG_WIDGET_ID, widgetId)
      args.putBoolean(ARG_SHOW_NAV_ICON, showNavIcon)
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
    if (arguments?.getBoolean(ARG_SHOW_NAV_ICON) == true) {
      toolbar.setNavigationIcon(R.drawable.ic_close)
      if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
        toolbar.navigationIcon?.setTint(resources.getColor(R.color.icon_tint))
        toolbar.navigationIcon?.setTintMode(PorterDuff.Mode.SRC_IN)
      }
      toolbar.setNavigationOnClickListener { requireActivity().finish() }
      fake_status_bar.visibility = View.GONE
    } else {
      fake_status_bar.visibility = View.VISIBLE
      fake_status_bar.layoutParams.height = requireContext().getStatusBarHeight()
      fake_status_bar.requestLayout()
    }
    adapter = SuggestionsAdapter(this)
    recycler_view.layoutManager = LinearLayoutManager(activity)
    recycler_view.addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))
    recycler_view.adapter = adapter
    search_view.addTextChangedListener(this)

    savedInstanceState?.let { selectedWidgetId = it.getInt(ARG_WIDGET_ID, -1) }
    lifecycleScope.launch {
      withContext(Dispatchers.Default) {
        adapter.getData()
            .forEach {
              it.exists = stocksProvider.hasTicker(it.symbol)
            }
      }
      adapter.notifyDataSetChanged()
    }
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
      InAppMessage.showToast(requireActivity(), getString(R.string.added_to_list, ticker))
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
            val data = withContext(Dispatchers.Default) {
              suggestions.data.map {
                val sug = Suggestion.fromSuggestionNet(it)
                sug.exists = stocksProvider.hasTicker(sug.symbol)
                sug
              }
            }
            adapter.setData(data)
          } else {
            Timber.w(suggestions.error)
            InAppMessage.showToast(requireActivity(), R.string.error_fetching_suggestions)
          }
        }
      } else {
        InAppMessage.showToast(requireActivity(), R.string.no_network_message)
      }
    }
  }

  override fun onSuggestionClick(suggestion: Suggestion): Boolean {
    val ticker = suggestion.symbol
    if (selectedWidgetId > 0) {
      addTickerToWidget(ticker, selectedWidgetId)
      return true
    }
    val intent = Intent(requireContext(), QuoteDetailActivity::class.java)
    intent.putExtra(QuoteDetailActivity.TICKER, ticker)
    startActivity(intent)
    return false
  }

  override fun onAddRemoveClick(suggestion: Suggestion): Boolean {
    val ticker = suggestion.symbol
    if (!suggestion.exists) {
      if (selectedWidgetId > 0) {
        addTickerToWidget(ticker, selectedWidgetId)
        return true
      } else {
        if (widgetDataProvider.hasWidget()) {
          val widgetIds = widgetDataProvider.getAppWidgetIds()
          if (widgetIds.size > 1) {
            val widgets =
              widgetIds.map { widgetDataProvider.dataForWidgetId(it) }
                  .sortedBy { it.widgetName() }
            val widgetNames = widgets.map { it.widgetName() }
                .toTypedArray()
            AlertDialog.Builder(requireActivity())
                .setTitle(R.string.select_widget)
                .setItems(widgetNames) { dialog, which ->
                  val id = widgets[which].widgetId
                  addTickerToWidget(ticker, id)
                  suggestion.exists = !suggestion.exists
                  adapter.notifyDataSetChanged()
                  dialog.dismiss()
                }
                .create()
                .show()
            return false
          } else {
            addTickerToWidget(ticker, widgetIds.first())
            return true
          }
        } else {
          addTickerToWidget(ticker, WidgetDataProvider.INVALID_WIDGET_ID)
          return true
        }
      }
    } else {
      if (selectedWidgetId > 0) {
        val widgetData = widgetDataProvider.dataForWidgetId(selectedWidgetId)
        widgetData.removeStock(ticker)
      } else {
        val widgetData = widgetDataProvider.widgetDataWithStock(ticker)
        widgetData?.removeStock(ticker)
      }
      return true
    }
  }

  // ChildFragment

  override fun setData(bundle: Bundle) {
    selectedWidgetId = bundle.getInt(ARG_WIDGET_ID, -1)
  }
}