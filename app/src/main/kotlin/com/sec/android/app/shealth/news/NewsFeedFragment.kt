package com.sec.android.app.shealth.news

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.sec.android.app.shealth.CustomTabs
import com.sec.android.app.shealth.base.BaseFragment
import com.sec.android.app.shealth.components.InAppMessage
import com.sec.android.app.shealth.getStatusBarHeight
import com.sec.android.app.shealth.home.ChildFragment
import com.sec.android.app.shealth.network.data.NewsArticle
import com.sec.android.app.shealth.ui.SpacingDecoration
import com.sec.android.app.shealth.R
import com.sec.android.app.shealth.R.dimen
import kotlinx.android.synthetic.main.fragment_news_feed.*

class NewsFeedFragment : BaseFragment(), ChildFragment, NewsFeedAdapter.NewsClickListener {

  companion object {
    private const val INDEX_PROGRESS = 0
    private const val INDEX_ERROR = 1
    private const val INDEX_EMPTY = 2
    private const val INDEX_DATA = 3
  }

  private lateinit var adapter: NewsFeedAdapter
  private lateinit var viewModel: NewsFeedViewModel
  override val simpleName: String
    get() = "NewsFeedFragment"

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    viewModel =
      ViewModelProvider(this).get(NewsFeedViewModel::class.java)
  }

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
    fake_status_bar.visibility = View.VISIBLE
    fake_status_bar.layoutParams.height = requireContext().getStatusBarHeight()
    fake_status_bar.requestLayout()
    adapter = NewsFeedAdapter(this)
    recycler_view.layoutManager = LinearLayoutManager(activity)
    recycler_view.addItemDecoration(
        SpacingDecoration(requireContext().resources.getDimensionPixelSize(dimen.list_spacing_double))
    )
    recycler_view.adapter = adapter
    swipe_container.setColorSchemeResources(R.color.color_primary_dark, R.color.spicy_salmon,
        R.color.sea)
    swipe_container.setOnRefreshListener { refreshNews() }
    viewModel.newsFeed.observe(viewLifecycleOwner, Observer {
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
    })
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