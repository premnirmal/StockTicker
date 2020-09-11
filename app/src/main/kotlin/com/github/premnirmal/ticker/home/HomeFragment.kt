package com.github.premnirmal.ticker.home

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.PopupWindow
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.github.premnirmal.ticker.base.BaseFragment
import com.github.premnirmal.ticker.components.AsyncBus
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.events.RefreshEvent
import com.github.premnirmal.ticker.getStatusBarHeight
import com.github.premnirmal.ticker.isNetworkOnline
import com.github.premnirmal.ticker.portfolio.PortfolioFragment
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import com.github.premnirmal.tickerwidget.R
import com.github.premnirmal.tickerwidget.R.layout
import kotlinx.android.synthetic.main.fragment_home.subtitle
import kotlinx.android.synthetic.main.fragment_home.swipe_container
import kotlinx.android.synthetic.main.fragment_home.tabs
import kotlinx.android.synthetic.main.fragment_home.toolbar
import kotlinx.android.synthetic.main.fragment_home.view_pager
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

class HomeFragment : BaseFragment(), ChildFragment, PortfolioFragment.Parent {

  companion object {
    private const val MAX_FETCH_COUNT = 3
  }

  interface Parent {
    fun showWhatsNew()
    fun showTutorial()
  }

  @Inject internal lateinit var widgetDataProvider: WidgetDataProvider
  @Inject internal lateinit var bus: AsyncBus
  override val simpleName: String = "HomeFragment"

  private var attemptingFetch = false
  private var fetchCount = 0
  private lateinit var adapter: HomePagerAdapter
  private lateinit var viewModel: HomeViewModel

  private val subtitleText: String
    get() = getString(
        R.string.last_and_next_fetch, viewModel.lastFetched(), viewModel.nextFetch()
    )

  private val totalHoldingsText: String
    get() {
      return if (viewModel.hasHoldings) {
        val (totalHolding, totalQuotesWithPosition) = viewModel.getTotalHoldings()
        getString(R.string.total_holdings, totalHolding, totalQuotesWithPosition)
      } else ""
    }

  private val totalGainLossText: Pair<String, String>
    get() = viewModel.getTotalGainLoss()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Injector.appComponent.inject(this)

    // Set up the ViewModel for the total holdings.
    viewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    return inflater.inflate(R.layout.fragment_home, container, false)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    (toolbar.layoutParams as MarginLayoutParams).topMargin = requireContext().getStatusBarHeight()
    swipe_container.setColorSchemeResources(
        R.color.color_primary_dark, R.color.spicy_salmon,
        R.color.sea
    )
    swipe_container.setOnRefreshListener { fetch() }
    adapter = HomePagerAdapter(childFragmentManager)
    view_pager.adapter = adapter
    tabs.setupWithViewPager(view_pager)
    subtitle.text = subtitleText
    toolbar.setOnMenuItemClickListener {
      showTotalHoldingsPopup()
      true
    }
    toolbar.menu.findItem(R.id.total_holdings).apply {
      isVisible = viewModel.hasHoldings
      isEnabled = viewModel.hasHoldings
    }
  }

  private fun showTotalHoldingsPopup() {
    val popupWindow = PopupWindow(requireContext(), null)
    val popupView = LayoutInflater.from(requireContext())
        .inflate(layout.layout_holdings_popup, null)
    popupWindow.contentView = popupView
    popupWindow.isOutsideTouchable = true
    popupWindow.setBackgroundDrawable(resources.getDrawable(R.drawable.card_bg))
    popupView.findViewById<TextView>(R.id.totalHoldings).text = totalHoldingsText
    val (totalGainStr, totalLossStr) = totalGainLossText
    popupView.findViewById<TextView>(R.id.totalGain).text = totalGainStr
    popupView.findViewById<TextView>(R.id.totalLoss).text = totalLossStr
    popupWindow.showAtLocation(toolbar, Gravity.TOP, toolbar.width / 2, toolbar.height)
  }

  override fun onHiddenChanged(hidden: Boolean) {
    super.onHiddenChanged(hidden)
    if (!hidden) updateHeader()
  }

  override fun onResume() {
    super.onResume()
    update()
    lifecycleScope.launch {
      val flow = bus.receive<RefreshEvent>()
      flow.collect {
        if (isResumed) {
          updateHeader()
        }
      }
    }
  }

  private fun updateHeader() {
    tabs.visibility = if (widgetDataProvider.hasWidget()) View.VISIBLE else View.INVISIBLE
    adapter.setData(widgetDataProvider.widgetDataList())
    subtitle.text = subtitleText
    toolbar.menu.findItem(R.id.total_holdings).apply {
      isVisible = viewModel.hasHoldings
      isEnabled = viewModel.hasHoldings
    }
  }

  private fun fetch() {
    if (!attemptingFetch) {
      if (requireActivity().isNetworkOnline()) {
        fetchCount++
        // Don't attempt to make many requests in a row if the stocks don't fetch.
        if (fetchCount <= MAX_FETCH_COUNT) {
          attemptingFetch = true
          viewModel.fetch().observe(viewLifecycleOwner, Observer { success ->
            attemptingFetch = false
            swipe_container?.isRefreshing = false
            if (success) {
              update()
            }
          })
        } else {
          attemptingFetch = false
          InAppMessage.showMessage(requireActivity(), R.string.refresh_failed, error = true)
          swipe_container?.isRefreshing = false
        }
      } else {
        attemptingFetch = false
        InAppMessage.showMessage(requireActivity(), R.string.no_network_message, error = true)
        swipe_container?.isRefreshing = false
      }
    }
  }

  private fun update() {
    adapter.notifyDataSetChanged()
    updateHeader()
    fetchCount = 0
  }

  // PortfolioFragment.Parent

  override fun onDragStarted() {
    swipe_container.isEnabled = false
  }

  override fun onDragEnded() {
    swipe_container.isEnabled = true
  }

  // ChildFragment

  override fun setData(bundle: Bundle) {

  }
}