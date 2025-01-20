package com.github.premnirmal.ticker.portfolio.search

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.premnirmal.ticker.analytics.ClickEvent
import com.github.premnirmal.ticker.base.BaseFragment
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.home.ChildFragment
import com.github.premnirmal.ticker.isNetworkOnline
import com.github.premnirmal.ticker.network.data.Suggestion
import com.github.premnirmal.ticker.news.QuoteDetailActivity
import com.github.premnirmal.ticker.portfolio.search.SuggestionsAdapter.SuggestionClickListener
import com.github.premnirmal.ticker.showDialog
import com.github.premnirmal.ticker.ui.SpacingDecoration
import com.github.premnirmal.ticker.viewBinding
import com.github.premnirmal.tickerwidget.R
import com.github.premnirmal.tickerwidget.R.dimen
import com.github.premnirmal.tickerwidget.databinding.FragmentSearchBinding
import com.google.android.material.color.MaterialColors
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchFragment : BaseFragment<FragmentSearchBinding>(), ChildFragment, SuggestionClickListener, TextWatcher {
	override val binding: (FragmentSearchBinding) by viewBinding(FragmentSearchBinding::inflate)
  companion object {
    private const val ARG_WIDGET_ID = AppWidgetManager.EXTRA_APPWIDGET_ID
    private const val ARG_SHOW_NAV_ICON = "SHOW_NAV_ICON"

    fun newInstance(
      widgetId: Int,
      showNavIcon: Boolean = false
    ): SearchFragment {
      val fragment = SearchFragment()
      val args = Bundle()
      args.putInt(ARG_WIDGET_ID, widgetId)
      args.putBoolean(ARG_SHOW_NAV_ICON, showNavIcon)
      fragment.arguments = args
      return fragment
    }
  }

  private val viewModel: SearchViewModel by viewModels()
  private lateinit var suggestionsAdapter: SuggestionsAdapter
  private lateinit var trendingAdapter: TrendingStocksAdapter
  override val simpleName: String = "SearchFragment"

  private var selectedWidgetId: Int = -1

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
        arguments?.let {
      setData(it)
    }
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    if (arguments?.getBoolean(ARG_SHOW_NAV_ICON) == true) {
      binding.toolbar.setNavigationIcon(R.drawable.ic_back)
      val tint = MaterialColors.getColor(binding.toolbar, com.google.android.material.R.attr.colorOnSurfaceVariant)
      binding.toolbar.navigationIcon?.setTint(tint)
      binding.toolbar.navigationIcon?.setTintMode(PorterDuff.Mode.SRC_IN)
      binding.toolbar.setNavigationOnClickListener { requireActivity().finish() }
    } else {
      // Inset toolbar from window top inset
      ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
        binding.toolbar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
          this.topMargin = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
        }
        insets
      }
    }
    trendingAdapter = TrendingStocksAdapter { quote ->
      analytics.trackClickEvent(ClickEvent("InstrumentClick"))
      val intent = Intent(requireContext(), QuoteDetailActivity::class.java)
      intent.putExtra(QuoteDetailActivity.TICKER, quote.symbol)
      startActivity(intent)
    }
    binding.trendingRecyclerView?.layoutManager = GridLayoutManager(activity, 3)
    binding.trendingRecyclerView?.addItemDecoration(
      SpacingDecoration(requireContext().resources.getDimensionPixelSize(dimen.list_spacing_double))
    )
    binding.trendingRecyclerView?.adapter = trendingAdapter

    suggestionsAdapter = SuggestionsAdapter(this)
    binding.searchResultsRecyclerView?.layoutManager = LinearLayoutManager(activity)
    binding.searchResultsRecyclerView?.addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))
    binding.searchResultsRecyclerView?.adapter = suggestionsAdapter

    binding.searchView.addTextChangedListener(this)

    savedInstanceState?.let { selectedWidgetId = it.getInt(ARG_WIDGET_ID, -1) }

    if (viewModel.searchResult.value?.wasSuccessful == true) {
      suggestionsAdapter.setData(viewModel.searchResult.value!!.data)
    }
    viewModel.fetchTrendingStocks().observe(viewLifecycleOwner) { quotes ->
      if (quotes.isNotEmpty()) trendingAdapter.setData(quotes)
    }
    viewModel.searchResult.observe(viewLifecycleOwner) {
      if (it.wasSuccessful) {
        suggestionsAdapter.setData(it.data)
      } else {
        suggestionsAdapter.setData(
            listOf(
                Suggestion(
                    binding.searchView.text?.toString()
                        .orEmpty()
                )
            )
        )
        InAppMessage.showToast(requireActivity(), R.string.error_fetching_suggestions)
      }
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
    if (viewModel.addTickerToWidget(ticker, widgetId)) {
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
      binding.searchResultsRecyclerView?.isVisible = true
      binding.trendingHolder?.isVisible = false
      if (requireActivity().isNetworkOnline()) {
        viewModel.fetchResults(query)
      } else {
        InAppMessage.showToast(requireActivity(), R.string.no_network_message)
      }
    } else {
      binding.searchResultsRecyclerView?.isVisible = false
      binding.trendingHolder?.isVisible = true
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
        val widgetDatas = viewModel.getWidgetDataList()
        if (viewModel.hasWidget()) {
          if (widgetDatas.size > 1) {
            val widgetNames = widgetDatas.map { it.widgetName() }
                .toTypedArray()
            AlertDialog.Builder(requireActivity())
                .setTitle(R.string.select_widget)
                .setItems(widgetNames) { dialog, which ->
                  val id = widgetDatas[which].widgetId
                  addTickerToWidget(ticker, id)
                  suggestion.exists = viewModel.doesSuggestionExist(suggestion)
                  suggestionsAdapter.notifyDataSetChanged()
                  dialog.dismiss()
                }
                .create()
                .show()
            return false
          } else {
            addTickerToWidget(ticker, widgetDatas.first().widgetId)
            return true
          }
        } else {
          addTickerToWidget(ticker, widgetDatas.first().widgetId)
          return true
        }
      }
    } else {
      viewModel.removeStock(ticker, selectedWidgetId)
      return true
    }
  }

  // ChildFragment

  override fun setData(bundle: Bundle) {
    selectedWidgetId = bundle.getInt(ARG_WIDGET_ID, -1)
  }

  override fun scrollToTop() {
    binding.searchResultsRecyclerView?.smoothScrollToPosition(0)
    binding.trendingRecyclerView?.smoothScrollToPosition(0)
  }
}