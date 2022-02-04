package com.github.premnirmal.ticker.home

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import com.github.premnirmal.ticker.base.BaseFragment
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.isNetworkOnline
import com.github.premnirmal.ticker.portfolio.PortfolioFragment
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import com.github.premnirmal.tickerwidget.R
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.internal.ViewUtils
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.fragment_home.app_bar_layout
import kotlinx.android.synthetic.main.fragment_home.subtitle
import kotlinx.android.synthetic.main.fragment_home.swipe_container
import kotlinx.android.synthetic.main.fragment_home.tabs
import kotlinx.android.synthetic.main.fragment_home.toolbar
import kotlinx.android.synthetic.main.fragment_home.view_pager
import javax.inject.Inject

class HomeFragment : BaseFragment(), ChildFragment, PortfolioFragment.Parent {

  companion object {
    private const val MAX_FETCH_COUNT = 3
  }

  @Inject internal lateinit var widgetDataProvider: WidgetDataProvider
  override val simpleName: String = "HomeFragment"

  private var attemptingFetch = false
  private var fetchCount = 0
  private lateinit var adapter: HomePagerAdapter
  private val viewModel: HomeViewModel by viewModels()

  private val subtitleText: String
    get() = getString(
        R.string.last_and_next_fetch, viewModel.lastFetched(), viewModel.nextFetch()
    )

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Injector.appComponent.inject(this)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    return inflater.inflate(R.layout.fragment_home, container, false)
  }

  @SuppressLint("RestrictedApi")
  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    ViewUtils.doOnApplyWindowInsets(view) { _, insets, _ ->
      val statusBarSize = insets.systemWindowInsetTop
      (toolbar.layoutParams as MarginLayoutParams).topMargin = statusBarSize
      (subtitle.layoutParams as MarginLayoutParams).topMargin = statusBarSize
      insets
    }
    swipe_container.setOnRefreshListener { fetch() }
    adapter = HomePagerAdapter(childFragmentManager, lifecycle)
    view_pager.adapter = adapter
    TabLayoutMediator(tabs, view_pager) { tab, position ->
      tab.text = widgetDataProvider.widgetDataList()[position].widgetName()
    }.attach()
    app_bar_layout.addOnOffsetChangedListener(offsetChangedListener)
    subtitle.text = subtitleText
    viewModel.fetchState.observe(viewLifecycleOwner) {
      updateHeader()
    }
    toolbar.setOnMenuItemClickListener {
      showTotalHoldingsPopup()
      true
    }
    toolbar.menu.findItem(R.id.total_holdings).apply {
      isVisible = viewModel.hasHoldings
      isEnabled = viewModel.hasHoldings
    }
  }

  override fun onDestroyView() {
    app_bar_layout.removeOnOffsetChangedListener(offsetChangedListener)
    super.onDestroyView()
  }

  override fun onHiddenChanged(hidden: Boolean) {
    super.onHiddenChanged(hidden)
    if (!hidden) updateHeader()
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

  private fun showTotalHoldingsPopup() {
    val popupWindow = PopupWindow(requireContext(), null)
    val popupView = LayoutInflater.from(requireContext()).inflate(R.layout.layout_holdings_popup, null)
    popupWindow.contentView = popupView
    popupWindow.isOutsideTouchable = true
    popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.card_bg))
    viewModel.getTotalGainLoss().observe(viewLifecycleOwner) {
      val totalHoldingsText = getString(R.string.total_holdings, it.holdings)
      popupView.findViewById<TextView>(R.id.totalHoldings).text = totalHoldingsText
      popupView.findViewById<TextView>(R.id.totalGain).text = it.gain
      popupView.findViewById<TextView>(R.id.totalLoss).text = it.loss
      popupWindow.showAtLocation(toolbar, Gravity.TOP, toolbar.width / 2, toolbar.height)
    }
  }

  private fun fetch() {
    if (!attemptingFetch) {
      if (requireActivity().isNetworkOnline()) {
        fetchCount++
        // Don't attempt to make many requests in a row if the stocks don't fetch.
        if (fetchCount <= MAX_FETCH_COUNT) {
          attemptingFetch = true
          viewModel.fetch().observe(viewLifecycleOwner) { success ->
            attemptingFetch = false
            swipe_container?.isRefreshing = false
            if (success) {
              update()
            }
          }
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

  private val offsetChangedListener = object : AppBarLayout.OnOffsetChangedListener {
    private var isTitleShowing = true

    override fun onOffsetChanged(appBarLayout: AppBarLayout?, verticalOffset: Int) {
      val show = verticalOffset > -tabs.height / 2
      if (show && !isTitleShowing) {
        subtitle.animate().alpha(1f).start()
        tabs.animate().alpha(1f).start()
        isTitleShowing = true
      } else if (!show && isTitleShowing) {
        subtitle.animate().alpha(0f).start()
        tabs.animate().alpha(0f).start()
        isTitleShowing = false
      }
    }
  }

  // ChildFragment

  override fun scrollToTop() {
    val fragment = childFragmentManager.findFragmentByTag("f${view_pager.currentItem}")
    (fragment  as? ChildFragment)?.scrollToTop()
    app_bar_layout.setExpanded(true, true)
  }
}