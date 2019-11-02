package com.github.premnirmal.ticker.home

import android.appwidget.AppWidgetManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.lifecycle.lifecycleScope
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.base.BaseFragment
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.components.RxBus
import com.github.premnirmal.ticker.components.isNetworkOnline
import com.github.premnirmal.ticker.getStatusBarHeight
import com.github.premnirmal.ticker.model.FetchException
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.portfolio.PortfolioFragment
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import com.github.premnirmal.tickerwidget.R
import io.github.inflationx.calligraphy3.TypefaceUtils
import kotlinx.android.synthetic.main.fragment_home.collapsingToolbarLayout
import kotlinx.android.synthetic.main.fragment_home.fab_settings
import kotlinx.android.synthetic.main.fragment_home.subtitle
import kotlinx.android.synthetic.main.fragment_home.swipe_container
import kotlinx.android.synthetic.main.fragment_home.tabs
import kotlinx.android.synthetic.main.fragment_home.toolbar
import kotlinx.android.synthetic.main.fragment_home.view_pager
import kotlinx.coroutines.launch
import javax.inject.Inject

class HomeFragment : BaseFragment(), ChildFragment, PortfolioFragment.Parent {

  companion object {
    private const val MAX_FETCH_COUNT = 3
  }

  interface Parent {
    fun showWhatsNew()
    fun showTutorial()
    fun openSearch(widgetId: Int)
  }

  @Inject internal lateinit var stocksProvider: IStocksProvider
  @Inject internal lateinit var widgetDataProvider: WidgetDataProvider
  @Inject internal lateinit var appPreferences: AppPreferences
  @Inject internal lateinit var bus: RxBus
  override val simpleName: String = "HomeFragment"

  private val parent: Parent
    get() = activity as Parent
  private var attemptingFetch = false
  private var fetchCount = 0
  private lateinit var adapter: HomePagerAdapter

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Injector.appComponent.inject(this)
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                            savedInstanceState: Bundle?): View {
    return inflater.inflate(R.layout.fragment_home, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    (toolbar.layoutParams as MarginLayoutParams).topMargin = context!!.getStatusBarHeight()
    val boldTypeface = TypefaceUtils.load(activity!!.assets, "fonts/Ubuntu-Bold.ttf")
    collapsingToolbarLayout.setCollapsedTitleTypeface(boldTypeface)
    collapsingToolbarLayout.setExpandedTitleTypeface(boldTypeface)
    swipe_container.setColorSchemeResources(R.color.color_primary_dark, R.color.spicy_salmon,
        R.color.sea)
    swipe_container.setOnRefreshListener { fetch() }
    adapter = HomePagerAdapter(childFragmentManager)
    view_pager.adapter = adapter
    tabs.setupWithViewPager(view_pager)
    subtitle?.text = getString(R.string.last_fetch, stocksProvider.lastFetched())
    fab_settings.setOnClickListener {
      if (!widgetDataProvider.hasWidget()) {
        val widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
        parent.openSearch(widgetId)
      }
    }
  }

  override fun onResume() {
    super.onResume()
    update()
//    bind(bus.forEventType(RefreshEvent::class.java))
//        .observeOn(AndroidSchedulers.mainThread())
//        .subscribe { update() }
  }

  private fun updateHeader() {
    tabs.visibility = if (widgetDataProvider.hasWidget()) View.VISIBLE else View.INVISIBLE
    subtitle?.text = getString(R.string.last_fetch, stocksProvider.lastFetched())
    if (!widgetDataProvider.hasWidget()) {
      fab_settings.show()
      fab_settings.setImageResource(R.drawable.ic_add)
    } else {
      fab_settings.hide()
    }
  }

  private fun fetch() {
    if (!attemptingFetch) {
      if (activity!!.isNetworkOnline()) {
        fetchCount++
        // Don't attempt to make many requests in a row if the stocks don't fetch.
        if (fetchCount <= MAX_FETCH_COUNT) {
          attemptingFetch = true
          lifecycleScope.launch {
            try {
              stocksProvider.fetch()
              update()
            } catch (ex: FetchException) {
              InAppMessage.showMessage(activity, getString(R.string.refresh_failed))
            }
            attemptingFetch = false
            swipe_container?.isRefreshing = false
          }
        } else {
          attemptingFetch = false
          InAppMessage.showMessage(activity, getString(R.string.refresh_failed))
          swipe_container?.isRefreshing = false
        }
      } else {
        attemptingFetch = false
        InAppMessage.showMessage(activity, getString(R.string.no_network_message))
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