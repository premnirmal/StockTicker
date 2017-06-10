package com.github.premnirmal.ticker.home

import android.app.AlertDialog.Builder
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.view.ViewGroup
import com.github.premnirmal.ticker.Tools
import com.github.premnirmal.ticker.base.BaseActivity
import com.github.premnirmal.ticker.components.Analytics
import com.github.premnirmal.ticker.components.CrashLogger
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.components.SimpleSubscriber
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.portfolio.PortfolioFragment
import com.github.premnirmal.ticker.settings.SettingsActivity
import com.github.premnirmal.tickerwidget.BuildConfig
import com.github.premnirmal.tickerwidget.R
import kotlinx.android.synthetic.main.activity_paranormal.activity_root
import kotlinx.android.synthetic.main.activity_paranormal.collapsingToolbarLayout
import kotlinx.android.synthetic.main.activity_paranormal.subtitle
import kotlinx.android.synthetic.main.activity_paranormal.swipe_container
import kotlinx.android.synthetic.main.activity_paranormal.toolbar
import kotlinx.android.synthetic.main.activity_paranormal.view_pager
import uk.co.chrisjenx.calligraphy.TypefaceUtils
import javax.inject.Inject

/**
 * Created by premnirmal on 2/25/16.
 */
class ParanormalActivity : BaseActivity(), PortfolioFragment.PortfolioFragmentCallback {

  @Inject
  lateinit internal var preferences: SharedPreferences
  @Inject
  lateinit internal var stocksProvider: IStocksProvider

  private var dialogShown = false
  private var attemptingFetch = false
  private var fetchCount = 0
  private var adapter: HomePagerAdapter? = null

  companion object {
    val DIALOG_SHOWN: String = "DIALOG_SHOWN"
    val MAX_FETCH_COUNT = 3
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Injector.inject(this)
    savedInstanceState?.let { dialogShown = it.getBoolean(DIALOG_SHOWN, false) }
    setContentView(R.layout.activity_paranormal)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      (toolbar.layoutParams as ViewGroup.MarginLayoutParams).topMargin =
          Tools.getStatusBarHeight(this)
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
        val v = findViewById(itemId)
        val rect = Rect()
        v.getGlobalVisibleRect(rect)
        val centerX = (rect.right - ((rect.right - rect.left) / 2))
        val centerY = (rect.bottom - ((rect.bottom - rect.top) / 2))
        intent.putExtra(EXTRA_CENTER_X, centerX)
        intent.putExtra(EXTRA_CENTER_Y, centerY)
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

    if (preferences.getBoolean(Tools.WHATS_NEW, false)) {
      preferences.edit().putBoolean(Tools.WHATS_NEW, false).apply()
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

    adapter = HomePagerAdapter(supportFragmentManager, packageName,
        AppWidgetManager.getInstance(applicationContext))
    view_pager.adapter = adapter

    subtitle?.text = getString(R.string.last_fetch, stocksProvider.lastFetched())
  }

  override fun onResume() {
    super.onResume()
    subtitle?.text = getString(R.string.last_fetch, stocksProvider.lastFetched())
  }

  override fun fetch() {
    if (Tools.isNetworkOnline(this)) {
      fetchCount++
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
      }
    } else {
      InAppMessage.showMessage(activity_root, getString(R.string.no_network_message))
      swipe_container?.isRefreshing = false
    }
  }

  fun update() {
    adapter?.notifyDataSetChanged()
    subtitle?.text = getString(R.string.last_fetch, stocksProvider.lastFetched())
    fetchCount = 0
  }

  override fun onSaveInstanceState(outState: Bundle?, outPersistentState: PersistableBundle?) {
    super.onSaveInstanceState(outState, outPersistentState)
    outState?.putBoolean(DIALOG_SHOWN, dialogShown)
  }

  private fun maybeAskToRate() {
    if (!dialogShown && Tools.shouldPromptRate()) {
      Builder(this).setTitle(R.string.like_our_app)
          .setMessage(R.string.please_rate)
          .setPositiveButton(R.string.yes) { dialog, _ ->
            sendToPlayStore()
            Analytics.trackRateYes()
            Tools.userDidRate()
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

  private fun sendToPlayStore() {
    val marketUri: Uri = Uri.parse("market://details?id=" + packageName)
    val marketIntent: Intent = Intent(Intent.ACTION_VIEW, marketUri)
    marketIntent.resolveActivity(packageManager)?.let {
      startActivity(marketIntent)
    }
  }
}