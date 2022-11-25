package com.github.premnirmal.ticker.home

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import com.github.premnirmal.ticker.base.BaseFragment
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.isNetworkOnline
import com.github.premnirmal.ticker.portfolio.PortfolioFragment
import com.github.premnirmal.ticker.viewBinding
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import com.github.premnirmal.tickerwidget.R
import com.github.premnirmal.tickerwidget.databinding.FragmentHomeBinding
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.internal.ViewUtils
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding>(), ChildFragment, PortfolioFragment.Parent {
	override val binding: (FragmentHomeBinding) by viewBinding(FragmentHomeBinding::inflate)
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
      }

  @SuppressLint("RestrictedApi")
  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    ViewUtils.doOnApplyWindowInsets(view) { _, insets, _ ->
      val statusBarSize = insets.systemWindowInsetTop
      (binding.toolbar.layoutParams as MarginLayoutParams).topMargin = statusBarSize
      (binding.subtitle.layoutParams as MarginLayoutParams).topMargin = statusBarSize
      insets
    }
    binding.swipeContainer.setOnRefreshListener { fetch() }
    adapter = HomePagerAdapter(childFragmentManager, lifecycle)
    binding.viewPager.adapter = adapter
    TabLayoutMediator(binding.tabs, binding.viewPager) { tab, position ->
      tab.text = widgetDataProvider.refreshWidgetDataList()[position].widgetName()
    }.attach()
    binding.appBarLayout.addOnOffsetChangedListener(offsetChangedListener)
    binding.subtitle.text = subtitleText
    viewModel.fetchState.observe(viewLifecycleOwner) {
      updateHeader()
    }
    binding.toolbar.setOnMenuItemClickListener {
      showTotalHoldingsPopup()
      true
    }
    binding.toolbar.menu.findItem(R.id.total_holdings).apply {
      isVisible = viewModel.hasHoldings
      isEnabled = viewModel.hasHoldings
    }
  }

  override fun onDestroyView() {
    binding.appBarLayout.removeOnOffsetChangedListener(offsetChangedListener)
    super.onDestroyView()
  }

  override fun onHiddenChanged(hidden: Boolean) {
    super.onHiddenChanged(hidden)
    if (!hidden) updateHeader()
  }

  private fun updateHeader() {
    binding.tabs.visibility = if (widgetDataProvider.hasWidget()) View.VISIBLE else View.INVISIBLE
    adapter.setData(widgetDataProvider.refreshWidgetDataList())
    binding.subtitle.text = subtitleText
    binding.toolbar.menu.findItem(R.id.total_holdings).apply {
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
      popupWindow.showAtLocation(binding.toolbar, Gravity.TOP, binding.toolbar.width / 2, binding.toolbar.height)
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
            binding.swipeContainer.isRefreshing = false
            if (success) {
              update()
            }
          }
        } else {
          attemptingFetch = false
          InAppMessage.showMessage(requireActivity(), R.string.refresh_failed, error = true)
          binding.swipeContainer.isRefreshing = false
        }
      } else {
        attemptingFetch = false
        InAppMessage.showMessage(requireActivity(), R.string.no_network_message, error = true)
        binding.swipeContainer.isRefreshing = false
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
    binding.swipeContainer.isEnabled = false
  }

  override fun onDragEnded() {
    binding.swipeContainer.isEnabled = true
  }

  private val offsetChangedListener = object : AppBarLayout.OnOffsetChangedListener {
    private var isTitleShowing = true

    override fun onOffsetChanged(appBarLayout: AppBarLayout?, verticalOffset: Int) {
      val show = verticalOffset > -binding.tabs.height / 2
      if (show && !isTitleShowing) {
        binding.subtitle.animate().alpha(1f).start()
        binding.tabs.animate().alpha(1f).start()
        isTitleShowing = true
      } else if (!show && isTitleShowing) {
        binding.subtitle.animate().alpha(0f).start()
        binding.tabs.animate().alpha(0f).start()
        isTitleShowing = false
      }
    }
  }

  // ChildFragment

  override fun scrollToTop() {
    val fragment = childFragmentManager.findFragmentByTag("f${binding.viewPager.currentItem}")
    (fragment  as? ChildFragment)?.scrollToTop()
    binding.appBarLayout.setExpanded(true, true)
  }
}