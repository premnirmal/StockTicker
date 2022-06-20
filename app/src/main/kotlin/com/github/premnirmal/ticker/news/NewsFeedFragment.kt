package com.github.premnirmal.ticker.news

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.premnirmal.ticker.CustomTabs
import com.github.premnirmal.ticker.base.BaseFragment
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.getStatusBarHeight
import com.github.premnirmal.ticker.home.ChildFragment
import com.github.premnirmal.ticker.network.data.NewsArticle
import com.github.premnirmal.ticker.ui.SpacingDecoration
import com.github.premnirmal.tickerwidget.R
import com.github.premnirmal.tickerwidget.R.dimen
import com.github.premnirmal.tickerwidget.databinding.FragmentNewsFeedBinding

class NewsFeedFragment : BaseFragment<FragmentNewsFeedBinding>(), ChildFragment, NewsFeedAdapter.NewsClickListener {

  companion object {
    private const val INDEX_PROGRESS = 0
    private const val INDEX_ERROR = 1
    private const val INDEX_EMPTY = 2
    private const val INDEX_DATA = 3
  }

  private lateinit var adapter: NewsFeedAdapter
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
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
      binding.fakeStatusBar.updateLayoutParams<ViewGroup.LayoutParams> {
        height = requireContext().getStatusBarHeight()
      }
    }
    adapter = NewsFeedAdapter(this)
    binding.recyclerView.layoutManager = LinearLayoutManager(activity)
    binding.recyclerView.addItemDecoration(
        SpacingDecoration(requireContext().resources.getDimensionPixelSize(dimen.list_spacing_double))
    )
    binding.recyclerView.adapter = adapter
    binding.swipeContainer.setColorSchemeResources(R.color.color_primary_dark, R.color.spicy_salmon,
        R.color.sea)
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

  // NewsFeedAdapter.NewsClickListener

  override fun onClickNewsArticle(article: NewsArticle) {
    CustomTabs.openTab(requireContext(), article.url)
  }

  // Child Fragment

  override fun scrollToTop() {
    binding.recyclerView.smoothScrollToPosition(0)
  }
}