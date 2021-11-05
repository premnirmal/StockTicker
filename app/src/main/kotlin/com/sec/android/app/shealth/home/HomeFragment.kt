package com.sec.android.app.shealth.home

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.sec.android.app.shealth.base.BaseFragment
import com.sec.android.app.shealth.components.AsyncBus
import com.sec.android.app.shealth.components.InAppMessage
import com.sec.android.app.shealth.components.Injector
import com.sec.android.app.shealth.events.RefreshEvent
import com.sec.android.app.shealth.getStatusBarHeight
import com.sec.android.app.shealth.isNetworkOnline
import com.sec.android.app.shealth.portfolio.PortfolioFragment
import com.sec.android.app.shealth.widget.WidgetDataProvider
import com.sec.android.app.shealth.R
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.fragment_home.*
import javax.inject.Inject
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

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
        val totalHoldings = viewModel.getTotalHoldings()
        getString(R.string.total_holdings, totalHoldings)
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
    swipe_container.setOnRefreshListener { fetch() }
    adapter = HomePagerAdapter(childFragmentManager, lifecycle)
    view_pager.adapter = adapter
    TabLayoutMediator(tabs, view_pager) { tab, position ->
      tab.text = widgetDataProvider.widgetDataList()[position].widgetName()
    }.attach()
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
        .inflate(R.layout.layout_holdings_popup, null)
    popupWindow.contentView = popupView
    popupWindow.isOutsideTouchable = true
    popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.card_bg))
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
          viewModel.fetch().observe(viewLifecycleOwner, { success ->
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

  override fun scrollToTop() {
    val fragment = childFragmentManager.findFragmentByTag("f${view_pager.currentItem}")
    (fragment  as? ChildFragment)?.scrollToTop()
    app_bar_layout.setExpanded(true, true)
  }
}