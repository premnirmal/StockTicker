package com.github.premnirmal.ticker.home

import android.app.AlertDialog.Builder
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.view.MenuItem
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.base.BaseActivity
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.portfolio.search.SearchFragment
import com.github.premnirmal.ticker.settings.SettingsFragment
import com.github.premnirmal.ticker.showDialog
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import com.github.premnirmal.ticker.widget.WidgetsFragment
import com.github.premnirmal.tickerwidget.BuildConfig
import com.github.premnirmal.tickerwidget.R
import kotlinx.android.synthetic.main.activity_paranormal.bottom_navigation
import javax.inject.Inject

/**
 * Created by premnirmal on 2/25/16.
 */
class ParanormalActivity : BaseActivity(), BottomNavigationView.OnNavigationItemSelectedListener,
    HomeFragment.Parent {

  companion object {
    private const val DIALOG_SHOWN: String = "DIALOG_SHOWN"
  }

  @Inject internal lateinit var appPreferences: AppPreferences
  @Inject internal lateinit var widgetDataProvider: WidgetDataProvider

  private var rateDialogShown = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_paranormal)
    Injector.appComponent.inject(this)

    savedInstanceState?.let { rateDialogShown = it.getBoolean(DIALOG_SHOWN, false) }

    bottom_navigation.setOnNavigationItemSelectedListener(this)

    if (savedInstanceState == null) {
      bottom_navigation.selectedItemId = R.id.action_portfolio
    }

    val tutorialShown = appPreferences.tutorialShown()
    if (!tutorialShown) {
      showTutorial()
    }

    if (appPreferences.getLastSavedVersionCode() < BuildConfig.VERSION_CODE) {
      showWhatsNew()
    }
  }

  override fun onResume() {
    super.onResume()
    bottom_navigation.menu.findItem(R.id.action_widgets).isEnabled = widgetDataProvider.hasWidget()
  }

  override fun onBackPressed() {
    if (!maybeAskToRate()) {
      super.onBackPressed()
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    outState.putBoolean(DIALOG_SHOWN, rateDialogShown)
    super.onSaveInstanceState(outState)
  }

  private fun maybeAskToRate(): Boolean {
    if (!rateDialogShown && appPreferences.shouldPromptRate()) {
      Builder(this).setTitle(R.string.like_our_app).setMessage(R.string.please_rate)
          .setPositiveButton(R.string.yes) { dialog, _ ->
            sendToPlayStore()
            appPreferences.userDidRate()
            dialog.dismiss()
          }.setNegativeButton(R.string.later) { dialog, _ ->
            dialog.dismiss()
          }.create().show()
      rateDialogShown = true
      return true
    }
    return false
  }

  private fun sendToPlayStore() {
    val marketUri: Uri = Uri.parse("market://details?id=$packageName")
    val marketIntent = Intent(Intent.ACTION_VIEW, marketUri)
    marketIntent.resolveActivity(packageManager)?.let {
      startActivity(marketIntent)
    }
  }

  // BottomNavigationView.OnNavigationItemSelectedListener

  override fun onNavigationItemSelected(item: MenuItem): Boolean {
    val fragment = when (item.itemId) {
      R.id.action_portfolio -> HomeFragment()
      R.id.action_widgets -> WidgetsFragment()
      R.id.action_search -> SearchFragment()
      R.id.action_settings -> SettingsFragment()
      else -> {
        return false
      }
    }
    supportFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment)
        .addToBackStack(fragment.javaClass.name).commit()
    return true
  }

  // HomeFragment.Parent

  override fun showTutorial() {
    showDialog(getString(R.string.how_to_title), getString(R.string.how_to))
    appPreferences.setTutorialShown(true)
  }

  override fun showWhatsNew() {
    appPreferences.saveVersionCode(BuildConfig.VERSION_CODE)
    val stringBuilder = StringBuilder()
    val whatsNew = resources.getStringArray(R.array.whats_new)
    whatsNew.indices.forEach {
      stringBuilder.append("- ")
      stringBuilder.append(whatsNew[it])
      if (it != whatsNew.size - 1) {
        stringBuilder.append("\n")
      }
    }
    showDialog(
        getString(R.string.whats_new_in, BuildConfig.VERSION_NAME), stringBuilder.toString()
    )
  }

  override fun openWidgetSettings(widgetId: Int) {
    bottom_navigation.selectedItemId = R.id.action_widgets
  }

  override fun openSearch(widgetId: Int) {
    bottom_navigation.selectedItemId = R.id.action_search
  }
}