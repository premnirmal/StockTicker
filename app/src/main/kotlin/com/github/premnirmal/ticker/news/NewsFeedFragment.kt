package com.github.premnirmal.ticker.news

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
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
import kotlinx.android.synthetic.main.fragment_news_feed.fake_status_bar
import kotlinx.android.synthetic.main.fragment_news_feed.recycler_view
import kotlinx.android.synthetic.main.fragment_news_feed.swipe_container
import kotlinx.android.synthetic.main.fragment_news_feed.toolbar
import kotlinx.android.synthetic.main.fragment_news_feed.view_flipper

class NewsFeedFragment : BaseFragment(), ChildFragment, NewsFeedAdapter.NewsClickListener {

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

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_news_feed, container, false)
  }

  override fun onViewCreated(
      view: View,
      savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
      toolbar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
        this.topMargin = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
      }
      insets
    }
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
      fake_status_bar.updateLayoutParams<ViewGroup.LayoutParams> {
        height = requireContext().getStatusBarHeight()
      }
    }
    adapter = NewsFeedAdapter(this)
    recycler_view.layoutManager = LinearLayoutManager(activity)
    recycler_view.addItemDecoration(
        SpacingDecoration(requireContext().resources.getDimensionPixelSize(dimen.list_spacing_double))
    )
    recycler_view.adapter = adapter
    swipe_container.setColorSchemeResources(R.color.color_primary_dark, R.color.spicy_salmon,
        R.color.sea)
    swipe_container.setOnRefreshListener { refreshNews() }
    viewModel.newsFeed.observe(viewLifecycleOwner) {
      if (it.wasSuccessful) {
        if (it.data.isEmpty()) {
          view_flipper.displayedChild = INDEX_EMPTY
        } else {
          adapter.setData(it.data)
          view_flipper.displayedChild = INDEX_DATA
        }
      } else {
        InAppMessage.showMessage(requireActivity(), R.string.news_fetch_failed, error = true)
        if (adapter.itemCount == 0) {
          view_flipper.displayedChild = INDEX_ERROR
        } else {
          view_flipper.displayedChild = INDEX_DATA
        }
      }
      swipe_container.isRefreshing = false
    }
  }

  override fun onStart() {
    super.onStart()
    view_flipper.displayedChild = INDEX_PROGRESS
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
    recycler_view.smoothScrollToPosition(0)
  }
}