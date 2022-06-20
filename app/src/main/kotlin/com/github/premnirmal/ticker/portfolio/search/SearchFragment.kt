package com.github.premnirmal.ticker.portfolio.search

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.premnirmal.ticker.base.BaseFragment
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.dismissKeyboard
import com.github.premnirmal.ticker.getStatusBarHeight
import com.github.premnirmal.ticker.home.ChildFragment
import com.github.premnirmal.ticker.isNetworkOnline
import com.github.premnirmal.ticker.network.data.Suggestion
import com.github.premnirmal.ticker.news.QuoteDetailActivity
import com.github.premnirmal.ticker.portfolio.search.SuggestionsAdapter.SuggestionClickListener
import com.github.premnirmal.ticker.showDialog
import com.github.premnirmal.ticker.showKeyboard
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import com.github.premnirmal.tickerwidget.R
import com.github.premnirmal.tickerwidget.databinding.FragmentSearchBinding

class SearchFragment : BaseFragment<FragmentSearchBinding>(), ChildFragment, SuggestionClickListener, TextWatcher {

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

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    if (arguments?.getBoolean(ARG_SHOW_NAV_ICON) == true) {
      binding.toolbar.setNavigationIcon(R.drawable.ic_back)
      binding.toolbar.navigationIcon?.setTint(ContextCompat.getColor(requireContext(), R.color.icon_tint))
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
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
      binding.fakeStatusBar.updateLayoutParams<ViewGroup.LayoutParams> {
        height = requireContext().getStatusBarHeight()
      }
    }
    adapter = SuggestionsAdapter(this)
    binding.recyclerView.layoutManager = LinearLayoutManager(activity)
    binding.recyclerView.addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))
    binding.recyclerView.adapter = adapter
    binding.searchView.addTextChangedListener(this)

    savedInstanceState?.let { selectedWidgetId = it.getInt(ARG_WIDGET_ID, -1) }
    if (viewModel.searchResult.value?.wasSuccessful == true) {
      adapter.setData(viewModel.searchResult.value!!.data)
    }
    viewModel.searchResult.observe(viewLifecycleOwner, Observer {
      if (it.wasSuccessful) {
        adapter.setData(it.data)
      } else {
        adapter.setData(listOf(Suggestion(binding.searchView.text?.toString().orEmpty())))
        InAppMessage.showToast(requireActivity(), R.string.error_fetching_suggestions)
      }
    })
  }

  override fun onHiddenChanged(hidden: Boolean) {
    super.onHiddenChanged(hidden)
    if (hidden) dismissKeyboard() else {
      binding.searchView.showKeyboard()
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
      if (requireActivity().isNetworkOnline()) {
        viewModel.fetchResults(query)
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
        if (viewModel.hasWidget()) {
          val widgetDatas = viewModel.getWidgetDatas()
          if (widgetDatas.size > 1) {
            val widgetNames = widgetDatas.map { it.widgetName() }
                .toTypedArray()
            AlertDialog.Builder(requireActivity())
                .setTitle(R.string.select_widget)
                .setItems(widgetNames) { dialog, which ->
                  val id = widgetDatas[which].widgetId
                  addTickerToWidget(ticker, id)
                  suggestion.exists = viewModel.doesSuggestionExist(suggestion)
                  adapter.notifyDataSetChanged()
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
          addTickerToWidget(ticker, WidgetDataProvider.INVALID_WIDGET_ID)
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
    binding.recyclerView.smoothScrollToPosition(0)
  }
}