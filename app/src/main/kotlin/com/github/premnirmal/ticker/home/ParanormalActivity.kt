package com.github.premnirmal.ticker.home

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.AlertDialog.Builder
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import android.view.ViewGroup
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.base.BaseActivity
import com.github.premnirmal.ticker.components.Analytics
import com.github.premnirmal.ticker.components.CrashLogger
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.components.SimpleSubscriber
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.portfolio.search.TickerSelectorActivity
import com.github.premnirmal.ticker.settings.SettingsActivity
import com.github.premnirmal.ticker.settings.WidgetSettingsActivity
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import com.github.premnirmal.tickerwidget.BuildConfig
import com.github.premnirmal.tickerwidget.R
import com.github.premnirmal.tickerwidget.R.string
import kotlinx.android.synthetic.main.activity_paranormal.activity_root
import kotlinx.android.synthetic.main.activity_paranormal.collapsingToolbarLayout
import kotlinx.android.synthetic.main.activity_paranormal.fab_add_stocks
import kotlinx.android.synthetic.main.activity_paranormal.fab_bg
import kotlinx.android.synthetic.main.activity_paranormal.fab_edit_widget
import kotlinx.android.synthetic.main.activity_paranormal.fab_settings
import kotlinx.android.synthetic.main.activity_paranormal.subtitle
import kotlinx.android.synthetic.main.activity_paranormal.swipe_container
import kotlinx.android.synthetic.main.activity_paranormal.tabs
import kotlinx.android.synthetic.main.activity_paranormal.toolbar
import kotlinx.android.synthetic.main.activity_paranormal.view_pager
import uk.co.chrisjenx.calligraphy.TypefaceUtils
import javax.inject.Inject


/**
 * Created by premnirmal on 2/25/16.
 */
class ParanormalActivity : BaseActivity() {

  companion object {
    val DIALOG_SHOWN: String = "DIALOG_SHOWN"
    val MAX_FETCH_COUNT = 3
    val FAB_ANIMATION_DURATION = 200L
  }

  @Inject
  lateinit internal var preferences: SharedPreferences
  @Inject
  lateinit internal var stocksProvider: IStocksProvider
  @Inject
  lateinit internal var widgetDataProvider: WidgetDataProvider

  private var dialogShown = false
  private var attemptingFetch = false
  private var isFABOpen = false
  private var fetchCount = 0
  private val adapter: HomePagerAdapter by lazy {
    HomePagerAdapter(supportFragmentManager)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Injector.appComponent.inject(this)
    savedInstanceState?.let { dialogShown = it.getBoolean(DIALOG_SHOWN, false) }
    setContentView(R.layout.activity_paranormal)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      (toolbar.layoutParams as ViewGroup.MarginLayoutParams).topMargin =
          getStatusBarHeight()
    }
    collapsingToolbarLayout.setCollapsedTitleTypeface(
        TypefaceUtils.load(assets, "fonts/Ubuntu-Regular.ttf"))
    collapsingToolbarLayout.setExpandedTitleTypeface(
        TypefaceUtils.load(assets, "fonts/Ubuntu-Bold.ttf"))

    toolbar.inflateMenu(R.menu.menu_paranormal)
    toolbar.setOnMenuItemClickListener { item ->
      val itemId = item.itemId
      if (itemId == R.id.action_settings) {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
        true
      } else {
        false
      }
    }

    swipe_container.setColorSchemeResources(R.color.color_secondary, R.color.spicy_salmon,
        R.color.sea)
    swipe_container.setOnRefreshListener({
      fetch()
    })

    if (preferences.getBoolean(AppPreferences.WHATS_NEW, false)) {
      preferences.edit().putBoolean(AppPreferences.WHATS_NEW, false).apply()
      val stringBuilder = StringBuilder()
      val whatsNew = resources.getStringArray(R.array.whats_new)
      for (i in whatsNew.indices) {
        stringBuilder.append("- ")
        stringBuilder.append(whatsNew[i])
        if (i != whatsNew.size - 1) {
          stringBuilder.append("\n")
        }
      }
      showDialog(getString(R.string.whats_new_in, BuildConfig.VERSION_NAME),
          stringBuilder.toString())
    } else {
      maybeAskToRate()
    }

    fab_settings.setOnClickListener({ v ->
      if (!widgetDataProvider.hasWidget()) {
        val widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
        openTickerSelector(v, widgetId)
      } else {
        if (isFABOpen) {
          closeFABMenu()
        } else {
          showFABMenu()
        }
      }

      fab_add_stocks.setOnClickListener { v ->
        val appWidgetIds = widgetDataProvider.getAppWidgetIds()
        val widgetId = appWidgetIds[view_pager!!.currentItem]
        openTickerSelector(v, widgetId)
      }

      fab_edit_widget.setOnClickListener { v ->
        val appWidgetIds = widgetDataProvider.getAppWidgetIds()
        val widgetId = appWidgetIds[view_pager!!.currentItem]
        openWidgetSettings(v, widgetId)
      }

    })

    fab_bg.setOnClickListener {
      closeFABMenu()
    }

    view_pager.adapter = adapter
    tabs?.setupWithViewPager(view_pager)
    subtitle?.text = getString(R.string.last_fetch, stocksProvider.lastFetched())
  }

  internal fun openTickerSelector(v: View, widgetId: Int) {
    val intent = TickerSelectorActivity.launchIntent(this, widgetId)
    val rect = Rect()
    v.getGlobalVisibleRect(rect)
    val centerX = (rect.right - ((rect.right - rect.left) / 2))
    val centerY = (rect.bottom - ((rect.bottom - rect.top) / 2))
    intent.putExtra(EXTRA_CENTER_X, centerX)
    intent.putExtra(EXTRA_CENTER_Y, centerY)
    startActivity(intent)
  }

  internal fun openWidgetSettings(v: View, widgetId: Int) {
    val intent = WidgetSettingsActivity.launchIntent(this, widgetId)
    val rect = Rect()
    v.getGlobalVisibleRect(rect)
    val centerX = (rect.right - ((rect.right - rect.left) / 2))
    val centerY = (rect.bottom - ((rect.bottom - rect.top) / 2))
    intent.putExtra(EXTRA_CENTER_X, centerX)
    intent.putExtra(EXTRA_CENTER_Y, centerY)
    startActivity(intent)
  }

  internal fun showFABMenu() {
    if (!isFABOpen) {
      isFABOpen = true
      fab_settings.animate().rotation(45f).setDuration(FAB_ANIMATION_DURATION).start()
      fab_add_stocks.visibility = View.VISIBLE
      fab_edit_widget.visibility = View.VISIBLE
      fab_bg.visibility = View.VISIBLE
      fab_add_stocks.animate().translationY(-resources.getDimension(R.dimen.first_fab_margin))
          .setDuration(FAB_ANIMATION_DURATION).start()
      fab_edit_widget.animate().translationY(-resources.getDimension(R.dimen.second_fab_margin))
          .setDuration(FAB_ANIMATION_DURATION).start()
    }
  }

  internal fun closeFABMenu() {
    if (isFABOpen) {
      isFABOpen = false
      fab_settings.animate().rotation(0f).setDuration(FAB_ANIMATION_DURATION).start()
      fab_bg.visibility = View.GONE
      fab_add_stocks.animate().translationY(0f).setDuration(FAB_ANIMATION_DURATION).start()
      fab_edit_widget.animate().translationY(0f).setDuration(FAB_ANIMATION_DURATION).setListener(
          object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animator: Animator) {
              if (!isFABOpen) {
                fab_add_stocks.visibility = View.GONE
                fab_edit_widget.visibility = View.GONE
              }

            }
          }).start()
    }
  }


  override fun onResume() {
    super.onResume()
    update()
    closeFABMenu()
  }

  internal fun updateHeader() {
    tabs.visibility = if (widgetDataProvider.hasWidget()) View.VISIBLE else View.INVISIBLE
    subtitle?.text = getString(string.last_fetch, stocksProvider.lastFetched())
    if (!widgetDataProvider.hasWidget()) {
      fab_settings.setImageResource(R.drawable.ic_add)
    } else {
      fab_settings.setImageResource(R.drawable.ic_settings)
    }
  }

  internal fun fetch() {
    if (!attemptingFetch) {
      if (isNetworkOnline()) {
        fetchCount++
        // Don't attempt to make many requests in a row if the stocks don't fetch.
        if (fetchCount <= MAX_FETCH_COUNT) {
          attemptingFetch = true
          bind(stocksProvider.fetch()).subscribe(object : SimpleSubscriber<List<Quote>>() {
            override fun onError(e: Throwable) {
              attemptingFetch = false
              CrashLogger.logException(e)
              swipe_container?.isRefreshing = false
              InAppMessage.showMessage(activity_root, getString(R.string.refresh_failed))
            }

            override fun onNext(result: List<Quote>) {
              attemptingFetch = false
              swipe_container?.isRefreshing = false
              update()
            }
          })
        } else {
          attemptingFetch = false
          InAppMessage.showMessage(activity_root, getString(R.string.refresh_failed))
          swipe_container?.isRefreshing = false
        }
      } else {
        attemptingFetch = false
        InAppMessage.showMessage(activity_root, getString(R.string.no_network_message))
        swipe_container?.isRefreshing = false
      }
    }
  }

  internal fun update() {
    adapter.notifyDataSetChanged()
    updateHeader()
    fetchCount = 0
  }

  override fun onSaveInstanceState(outState: Bundle?, outPersistentState: PersistableBundle?) {
    super.onSaveInstanceState(outState, outPersistentState)
    outState?.putBoolean(DIALOG_SHOWN, dialogShown)
  }

  internal fun maybeAskToRate() {
    if (!dialogShown && AppPreferences.shouldPromptRate()) {
      Builder(this).setTitle(R.string.like_our_app)
          .setMessage(R.string.please_rate)
          .setPositiveButton(R.string.yes) { dialog, _ ->
            sendToPlayStore()
            Analytics.trackRateYes()
            AppPreferences.userDidRate()
            dialog.dismiss()
          }
          .setNegativeButton(R.string.later) { dialog, _ ->
            Analytics.trackRateNo()
            dialog.dismiss()
          }
          .create().show()
      dialogShown = true
    }
  }

  internal fun sendToPlayStore() {
    val marketUri: Uri = Uri.parse("market://details?id=" + packageName)
    val marketIntent: Intent = Intent(Intent.ACTION_VIEW, marketUri)
    marketIntent.resolveActivity(packageManager)?.let {
      startActivity(marketIntent)
    }
  }
}