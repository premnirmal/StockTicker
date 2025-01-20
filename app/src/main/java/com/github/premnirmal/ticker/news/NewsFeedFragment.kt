package com.github.premnirmal.ticker.news

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.premnirmal.ticker.CustomTabs
import com.github.premnirmal.ticker.analytics.ClickEvent
import com.github.premnirmal.ticker.base.BaseFragment
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.home.ChildFragment
import com.github.premnirmal.ticker.network.data.NewsArticle
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.ui.SpacingDecoration
import com.github.premnirmal.ticker.viewBinding
import com.github.premnirmal.tickerwidget.R
import com.github.premnirmal.tickerwidget.R.dimen
import com.github.premnirmal.tickerwidget.databinding.FragmentNewsFeedBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NewsFeedFragment : BaseFragment<FragmentNewsFeedBinding>(), ChildFragment, TrendingAdapter.TrendingListener {

  companion object {
    private const val INDEX_PROGRESS = 0
    private const val INDEX_ERROR = 1
    private const val INDEX_EMPTY = 2
    private const val INDEX_DATA = 3
  }

  override val binding: (FragmentNewsFeedBinding) by viewBinding(FragmentNewsFeedBinding::inflate)
  private lateinit var adapter: TrendingAdapter
  private val viewModel: NewsFeedViewModel by viewModels()
  override val simpleName: String
    get() = "NewsFeedFragment"

  override fun onViewCreated(
      view: View,
      savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
      binding.toolbar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
        this.topMargin = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
      }
      insets
    }
    adapter = TrendingAdapter(this)
    binding.recyclerView.layoutManager = LinearLayoutManager(activity)
    binding.recyclerView.addItemDecoration(
        SpacingDecoration(requireContext().resources.getDimensionPixelSize(dimen.list_spacing_double))
    )
    binding.recyclerView.adapter = adapter
    binding.swipeContainer.setOnRefreshListener { refreshNews() }
    viewModel.newsFeed.observe(viewLifecycleOwner) {
      if (it.wasSuccessful) {
        if (it.data.isEmpty()) {
          binding.viewFlipper.displayedChild = INDEX_EMPTY
        } else {
          adapter.setData(it.data)
          binding.viewFlipper.displayedChild = INDEX_DATA
        }
      } else {
        InAppMessage.showMessage(requireActivity(), R.string.news_fetch_failed, error = true)
        if (adapter.itemCount == 0) {
          binding.viewFlipper.displayedChild = INDEX_ERROR
        } else {
          binding.viewFlipper.displayedChild = INDEX_DATA
        }
      }
      binding.swipeContainer.isRefreshing = false
    }
  }

  override fun onStart() {
    super.onStart()
    binding.viewFlipper.displayedChild = INDEX_PROGRESS
    viewModel.fetchNews()
  }

  private fun refreshNews() {
    viewModel.fetchNews(forceRefresh = true)
  }

  // TrendingAdapter.TrendingListener

  override fun onClickNewsArticle(article: NewsArticle) {
    CustomTabs.openTab(requireContext(), article.url)
  }

  override fun onClickQuote(quote: Quote) {
    analytics.trackClickEvent(ClickEvent("InstrumentClick"))
    val intent = Intent(requireContext(), QuoteDetailActivity::class.java)
    intent.putExtra(QuoteDetailActivity.TICKER, quote.symbol)
    startActivity(intent)
  }

  // Child Fragment

  override fun scrollToTop() {
    binding.recyclerView.smoothScrollToPosition(0)
  }
}