package com.github.premnirmal.ticker.home

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.appwidget.AppWidgetManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.base.BaseFragment
import com.github.premnirmal.ticker.base.ParentActivityDelegate
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.components.RxBus
import com.github.premnirmal.ticker.components.isNetworkOnline
import com.github.premnirmal.ticker.events.RefreshEvent
import com.github.premnirmal.ticker.getStatusBarHeight
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.network.SimpleSubscriber
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.portfolio.PortfolioFragment
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import com.github.premnirmal.tickerwidget.R
import io.github.inflationx.calligraphy3.TypefaceUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.fragment_home.add_stocks_container
import kotlinx.android.synthetic.main.fragment_home.collapsingToolbarLayout
import kotlinx.android.synthetic.main.fragment_home.edit_widget_container
import kotlinx.android.synthetic.main.fragment_home.fab_add_stocks
import kotlinx.android.synthetic.main.fragment_home.fab_bg
import kotlinx.android.synthetic.main.fragment_home.fab_edit_widget
import kotlinx.android.synthetic.main.fragment_home.fab_settings
import kotlinx.android.synthetic.main.fragment_home.subtitle
import kotlinx.android.synthetic.main.fragment_home.swipe_container
import kotlinx.android.synthetic.main.fragment_home.tabs
import kotlinx.android.synthetic.main.fragment_home.toolbar
import kotlinx.android.synthetic.main.fragment_home.view_pager
import javax.inject.Inject

class HomeFragment : BaseFragment(), ChildFragment, PortfolioFragment.Parent {

  companion object {
    private const val MAX_FETCH_COUNT = 3
    private const val FAB_ANIMATION_DURATION = 200L
  }

  interface Parent {
    fun showWhatsNew()
    fun showTutorial()
    fun openWidgetSettings(widgetId: Int)
    fun openSearch(widgetId: Int)
  }

  @Inject internal lateinit var stocksProvider: IStocksProvider
  @Inject internal lateinit var widgetDataProvider: WidgetDataProvider
  @Inject internal lateinit var appPreferences: AppPreferences
  @Inject internal lateinit var bus: RxBus

  private val parent: Parent by ParentActivityDelegate(this)
  private var attemptingFetch = false
  private var isFABOpen = false
  private var fetchCount = 0
  private lateinit var adapter: HomePagerAdapter

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

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    (toolbar.layoutParams as MarginLayoutParams).topMargin = context!!.getStatusBarHeight()
    collapsingToolbarLayout.setCollapsedTitleTypeface(
        TypefaceUtils.load(activity!!.assets, "fonts/Ubuntu-Regular.ttf")
    )
    collapsingToolbarLayout.setExpandedTitleTypeface(
        TypefaceUtils.load(activity!!.assets, "fonts/Ubuntu-Bold.ttf")
    )

    swipe_container.setColorSchemeResources(
        R.color.color_primary_dark, R.color.spicy_salmon,
        R.color.sea
    )
    swipe_container.setOnRefreshListener { fetch() }
    adapter = HomePagerAdapter(childFragmentManager)
    view_pager.adapter = adapter
    tabs.setupWithViewPager(view_pager)
    subtitle?.text = getString(R.string.last_fetch, stocksProvider.lastFetched())

    fab_settings.setOnClickListener {
      if (!widgetDataProvider.hasWidget()) {
        val widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
        parent.openSearch(widgetId)
      } else {
        if (isFABOpen) {
          closeFABMenu()
        } else {
          showFABMenu()
        }
      }
    }

    fab_add_stocks.setOnClickListener {
      val appWidgetIds = widgetDataProvider.getAppWidgetIds()
      if (appWidgetIds.isNotEmpty()) {
        val widgetId = appWidgetIds[view_pager!!.currentItem]
        parent.openSearch(widgetId)
      }
    }

    fab_edit_widget.setOnClickListener {
      val appWidgetIds = widgetDataProvider.getAppWidgetIds()
      if (appWidgetIds.isNotEmpty()) {
        val widgetId = appWidgetIds[view_pager!!.currentItem]
        parent.openWidgetSettings(widgetId)
      }
    }

    fab_bg.setOnClickListener {
      closeFABMenu()
    }
  }

  override fun onPause() {
    closeFABMenu()
    super.onPause()
  }

  private fun showFABMenu() {
    if (!isFABOpen) {
      isFABOpen = true
      fab_settings.animate()
          .rotationBy(45f)
          .setDuration(FAB_ANIMATION_DURATION)
          .start()
      add_stocks_container.visibility = View.VISIBLE
      edit_widget_container.visibility = View.VISIBLE
      fab_bg.visibility = View.VISIBLE
      fab_bg.animate()
          .alpha(1f)
          .setDuration(FAB_ANIMATION_DURATION)
          .setListener(null)
          .start()
      add_stocks_container.animate()
          .translationY(-resources.getDimension(R.dimen.first_fab_margin))
          .setDuration(FAB_ANIMATION_DURATION)
          .start()
      edit_widget_container.animate()
          .translationY(-resources.getDimension(R.dimen.second_fab_margin))
          .setDuration(FAB_ANIMATION_DURATION)
          .start()
    }
  }

  private fun closeFABMenu() {
    if (isFABOpen) {
      isFABOpen = false
      fab_settings.animate()
          .rotationBy(-45f)
          .setDuration(FAB_ANIMATION_DURATION)
          .start()
      fab_bg.visibility = View.VISIBLE
      fab_bg.animate()
          .alpha(0f)
          .setDuration(FAB_ANIMATION_DURATION)
          .setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
              if (isVisible) fab_bg.visibility = View.GONE
            }
          })
          .start()
      add_stocks_container.animate()
          .translationY(0f)
          .setDuration(FAB_ANIMATION_DURATION)
          .start()
      edit_widget_container.animate()
          .translationY(0f)
          .setDuration(FAB_ANIMATION_DURATION)
          .setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animator: Animator) {
              if (isVisible && !isFABOpen) {
                add_stocks_container.visibility = View.GONE
                edit_widget_container.visibility = View.GONE
              }

            }
          })
          .start()
    }
  }

  override fun onResume() {
    super.onResume()
    update()
    closeFABMenu()
    bind(bus.forEventType(RefreshEvent::class.java)).observeOn(AndroidSchedulers.mainThread())
        .subscribe {
          update()
        }
  }

  private fun updateHeader() {
    tabs.visibility = if (widgetDataProvider.hasWidget()) View.VISIBLE else View.INVISIBLE
    subtitle?.text = getString(R.string.last_fetch, stocksProvider.lastFetched())
    if (!widgetDataProvider.hasWidget()) {
      fab_settings.setImageResource(R.drawable.ic_add)
    } else {
      fab_settings.setImageResource(R.drawable.ic_settings)
    }
  }

  private fun fetch() {
    if (!attemptingFetch) {
      if (activity!!.isNetworkOnline()) {
        fetchCount++
        // Don't attempt to make many requests in a row if the stocks don't fetch.
        if (fetchCount <= MAX_FETCH_COUNT) {
          attemptingFetch = true
          bind(stocksProvider.fetch()).subscribe(object : SimpleSubscriber<List<Quote>>() {
            override fun onError(e: Throwable) {
              attemptingFetch = false
              swipe_container?.isRefreshing = false
              InAppMessage.showMessage(activity, getString(R.string.refresh_failed))
            }

            override fun onNext(result: List<Quote>) {
              attemptingFetch = false
              swipe_container?.isRefreshing = false
            }
          })
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