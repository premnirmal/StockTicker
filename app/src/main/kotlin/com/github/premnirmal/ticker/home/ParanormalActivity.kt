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
import android.util.DisplayMetrics
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.base.BaseActivity
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.components.isNetworkOnline
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.network.SimpleSubscriber
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.portfolio.PortfolioFragment
import com.github.premnirmal.ticker.settings.SettingsActivity
import com.github.premnirmal.ticker.settings.WidgetSettingsActivity
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import com.github.premnirmal.tickerwidget.BuildConfig
import com.github.premnirmal.tickerwidget.R
import com.github.premnirmal.tickerwidget.R.array
import com.github.premnirmal.tickerwidget.R.string
import kotlinx.android.synthetic.main.activity_paranormal.add_stocks_container
import kotlinx.android.synthetic.main.activity_paranormal.collapsingToolbarLayout
import kotlinx.android.synthetic.main.activity_paranormal.edit_widget_container
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
class ParanormalActivity : BaseActivity(), PortfolioFragment.Callback {

  companion object {
    const val DIALOG_SHOWN: String = "DIALOG_SHOWN"
    const val MAX_FETCH_COUNT = 3
    const val FAB_ANIMATION_DURATION = 200L
  }

  @Inject internal lateinit var preferences: SharedPreferences
  @Inject internal lateinit var stocksProvider: IStocksProvider
  @Inject internal lateinit var widgetDataProvider: WidgetDataProvider
  @Inject internal lateinit var appPreferences: AppPreferences

  private var dialogShown = false
  private var attemptingFetch = false
  private var isFABOpen = false
  private var fetchCount = 0
  private val adapter: HomePagerAdapter by lazy {
    HomePagerAdapter(supportFragmentManager)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_paranormal)
    Injector.appComponent.inject(this)
    savedInstanceState?.let { dialogShown = it.getBoolean(DIALOG_SHOWN, false) }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      (toolbar.layoutParams as ViewGroup.MarginLayoutParams).topMargin =
          getStatusBarHeight()
    }

    if (!hasNavBar()) {
      val margin = resources.getDimensionPixelSize(R.dimen.fab_margin_bottom_without_nav)
      (fab_settings.layoutParams as MarginLayoutParams).bottomMargin = margin
      (add_stocks_container.layoutParams as MarginLayoutParams).bottomMargin = margin
      (edit_widget_container.layoutParams as MarginLayoutParams).bottomMargin = margin
    }

    collapsingToolbarLayout.setCollapsedTitleTypeface(
        TypefaceUtils.load(assets, "fonts/Ubuntu-Regular.ttf"))
    collapsingToolbarLayout.setExpandedTitleTypeface(
        TypefaceUtils.load(assets, "fonts/Ubuntu-Bold.ttf"))

    swipe_container.setColorSchemeResources(R.color.color_primary_dark, R.color.spicy_salmon,
        R.color.sea)
    swipe_container.setOnRefreshListener { fetch() }

    view_pager.adapter = adapter
    tabs?.setupWithViewPager(view_pager)
    subtitle?.text = getString(R.string.last_fetch, stocksProvider.lastFetched())

    val tutorialShown = appPreferences.tutorialShown()
    if (!tutorialShown) {
      showTutorial()
    }

    if (appPreferences.getLastSavedVersionCode() < BuildConfig.VERSION_CODE) {
      showWhatsNew()
    } else {
      maybeAskToRate()
    }

    toolbar.inflateMenu(R.menu.menu_paranormal)
    toolbar.setOnMenuItemClickListener { item ->
      val itemId = item.itemId
      when (itemId) {
        R.id.action_settings -> {
          val intent = Intent(this, SettingsActivity::class.java)
          startActivity(intent)
          true
        }
        R.id.action_tutorial -> {
          showTutorial()
          true
        }
        R.id.action_whats_new -> {
          showWhatsNew()
          true
        }
        else -> false
      }
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
    })

    fab_add_stocks.setOnClickListener { v ->
      val appWidgetIds = widgetDataProvider.getAppWidgetIds()
      if (appWidgetIds.isNotEmpty()) {
        val widgetId = appWidgetIds[view_pager!!.currentItem]
        openTickerSelector(v, widgetId)
      }
    }

    fab_edit_widget.setOnClickListener { v ->
      val appWidgetIds = widgetDataProvider.getAppWidgetIds()
      if (appWidgetIds.isNotEmpty()) {
        val widgetId = appWidgetIds[view_pager!!.currentItem]
        openWidgetSettings(v, widgetId)
      }
    }

    fab_bg.setOnClickListener {
      closeFABMenu()
    }
  }

  private fun showTutorial() {
    showDialog(getString(string.how_to_title), getString(string.how_to))
    appPreferences.setTutorialShown(true)
  }

  private fun showWhatsNew() {
    appPreferences.saveVersionCode(BuildConfig.VERSION_CODE)
    val stringBuilder = StringBuilder()
    val whatsNew = resources.getStringArray(array.whats_new)
    whatsNew.indices.forEach {
      stringBuilder.append("- ")
      stringBuilder.append(whatsNew[it])
      if (it != whatsNew.size - 1) {
        stringBuilder.append("\n")
      }
    }
    showDialog(getString(string.whats_new_in, BuildConfig.VERSION_NAME),
        stringBuilder.toString())
  }

  private fun openWidgetSettings(v: View, widgetId: Int) {
    val intent = WidgetSettingsActivity.launchIntent(this, widgetId)
    val rect = Rect()
    v.getGlobalVisibleRect(rect)
    val centerX = (rect.right - ((rect.right - rect.left) / 2))
    val centerY = (rect.bottom - ((rect.bottom - rect.top) / 2))
    intent.putExtra(EXTRA_CENTER_X, centerX)
    intent.putExtra(EXTRA_CENTER_Y, centerY)
    startActivity(intent)
  }

  private fun showFABMenu() {
    if (!isFABOpen) {
      isFABOpen = true
      fab_settings.animate().rotationBy(45f).setDuration(FAB_ANIMATION_DURATION).start()
      add_stocks_container.visibility = View.VISIBLE
      edit_widget_container.visibility = View.VISIBLE
      fab_bg.visibility = View.VISIBLE
      fab_bg.animate().alpha(1f).setDuration(FAB_ANIMATION_DURATION).setListener(null).start()
      add_stocks_container.animate().translationY(-resources.getDimension(R.dimen.first_fab_margin))
          .setDuration(FAB_ANIMATION_DURATION).start()
      edit_widget_container.animate().translationY(
          -resources.getDimension(R.dimen.second_fab_margin))
          .setDuration(FAB_ANIMATION_DURATION).start()
    }
  }

  private fun closeFABMenu() {
    if (isFABOpen) {
      isFABOpen = false
      fab_settings.animate().rotationBy(-45f).setDuration(FAB_ANIMATION_DURATION).start()
      fab_bg.visibility = View.VISIBLE
      fab_bg.animate().alpha(0f).setDuration(FAB_ANIMATION_DURATION)
          .setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
              fab_bg.visibility = View.GONE
            }
          }).start()
      add_stocks_container.animate().translationY(0f).setDuration(FAB_ANIMATION_DURATION).start()
      edit_widget_container.animate().translationY(0f).setDuration(
          FAB_ANIMATION_DURATION).setListener(
          object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animator: Animator) {
              if (!isFABOpen) {
                add_stocks_container.visibility = View.GONE
                edit_widget_container.visibility = View.GONE
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

  private fun updateHeader() {
    tabs.visibility = if (widgetDataProvider.hasWidget()) View.VISIBLE else View.INVISIBLE
    subtitle?.text = getString(R.string.last_fetch, stocksProvider.lastFetched())
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
              swipe_container?.isRefreshing = false
              InAppMessage.showMessage(this@ParanormalActivity, getString(string.refresh_failed))
            }

            override fun onNext(result: List<Quote>) {
              attemptingFetch = false
              swipe_container?.isRefreshing = false
              update()
            }
          })
        } else {
          attemptingFetch = false
          InAppMessage.showMessage(this, getString(string.refresh_failed))
          swipe_container?.isRefreshing = false
        }
      } else {
        attemptingFetch = false
        InAppMessage.showMessage(this, getString(string.no_network_message))
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

  private fun maybeAskToRate() {
    if (!dialogShown && appPreferences.shouldPromptRate()) {
      Builder(this).setTitle(R.string.like_our_app)
          .setMessage(R.string.please_rate)
          .setPositiveButton(R.string.yes) { dialog, _ ->
            sendToPlayStore()
            appPreferences.userDidRate()
            dialog.dismiss()
          }
          .setNegativeButton(R.string.later) { dialog, _ ->
            dialog.dismiss()
          }
          .create().show()
      dialogShown = true
    }
  }

  private fun sendToPlayStore() {
    val marketUri: Uri = Uri.parse("market://details?id=" + packageName)
    val marketIntent = Intent(Intent.ACTION_VIEW, marketUri)
    marketIntent.resolveActivity(packageManager)?.let {
      startActivity(marketIntent)
    }
  }

  private fun hasNavBar(): Boolean {
    val hasSoftwareKeys: Boolean
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
      val display = windowManager.defaultDisplay
      val realDisplayMetrics = DisplayMetrics()
      display.getRealMetrics(realDisplayMetrics)

      val realHeight = realDisplayMetrics.heightPixels
      val realWidth = realDisplayMetrics.widthPixels

      val displayMetrics = DisplayMetrics()
      display.getMetrics(displayMetrics)

      val displayHeight = displayMetrics.heightPixels
      val displayWidth = displayMetrics.widthPixels

      hasSoftwareKeys = realWidth - displayWidth > 0 || realHeight - displayHeight > 0
    } else {
      val hasMenuKey = ViewConfiguration.get(this).hasPermanentMenuKey()
      val hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK)
      hasSoftwareKeys = !hasMenuKey && !hasBackKey
    }
    return hasSoftwareKeys
  }


  // PortfolioFragment.Callback

  override fun onDragStarted() {
    swipe_container.isEnabled = false
  }

  override fun onDragEnded() {
    swipe_container.isEnabled = true
  }
}