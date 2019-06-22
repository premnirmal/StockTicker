package com.github.premnirmal.ticker.home

import android.app.Activity
import android.app.AlertDialog.Builder
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.base.BaseActivity
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.portfolio.search.SearchFragment
import com.github.premnirmal.ticker.settings.SettingsFragment
import com.github.premnirmal.ticker.settings.WidgetSettingsFragment
import com.github.premnirmal.ticker.showDialog
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import com.github.premnirmal.ticker.widget.WidgetsFragment
import com.github.premnirmal.tickerwidget.BuildConfig
import com.github.premnirmal.tickerwidget.R
import com.github.premnirmal.tickerwidget.R.id
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.activity_paranormal.bottom_navigation
import javax.inject.Inject

/**
 * Created by premnirmal on 2/25/16.
 */
class ParanormalActivity : BaseActivity(), BottomNavigationView.OnNavigationItemSelectedListener,
    HomeFragment.Parent, SettingsFragment.Parent, WidgetSettingsFragment.Parent,
    FragmentManager.OnBackStackChangedListener {

  companion object {
    private const val DIALOG_SHOWN: String = "DIALOG_SHOWN"
    const val ARG_WIDGET_ID = AppWidgetManager.EXTRA_APPWIDGET_ID
    private val FRAGMENT_MAP =
      mapOf<String, Int>(HomeFragment::class.java.name to R.id.action_portfolio,
          WidgetsFragment::class.java.name to R.id.action_widgets,
          SearchFragment::class.java.name to R.id.action_search,
          SettingsFragment::class.java.name to R.id.action_settings)
  }

  @Inject internal lateinit var appPreferences: AppPreferences
  @Inject internal lateinit var widgetDataProvider: WidgetDataProvider

  private var currentChild: ChildFragment? = null
  private var rateDialogShown = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_paranormal)
    Injector.appComponent.inject(this)

    savedInstanceState?.let { rateDialogShown = it.getBoolean(DIALOG_SHOWN, false) }

    bottom_navigation.setOnNavigationItemSelectedListener(this)

    if (savedInstanceState == null) {
      val fragment = HomeFragment()
      supportFragmentManager.beginTransaction().add(R.id.fragment_container, fragment).commit()
      currentChild = fragment
    }

    supportFragmentManager.addOnBackStackChangedListener(this)

    val widgetId = intent.getIntExtra(ARG_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
    if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
      val result = Intent()
      result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
      setResult(Activity.RESULT_OK, result)

      val fragment = WidgetsFragment.newInstance(widgetId)
      supportFragmentManager.beginTransaction().replace(id.fragment_container, fragment)
          .addToBackStack(fragment.javaClass.name).commit()
      currentChild = fragment

      return
    } else {
      setResult(Activity.RESULT_CANCELED)
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
    val entryCount = supportFragmentManager.backStackEntryCount
    if (entryCount > 0 || !maybeAskToRate()) {
      super.onBackPressed()
    }
  }

  override fun onBackStackChanged() {
    updateBottomNav()
  }

  private fun updateBottomNav() {
    val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
    currentChild = currentFragment as? ChildFragment
    val selectedItemId: Int? = FRAGMENT_MAP[currentFragment?.javaClass?.name]
    if (selectedItemId != null && selectedItemId != bottom_navigation.selectedItemId) {
      bottom_navigation.selectedItemId = selectedItemId
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
    val itemId = item.itemId
    val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
    if (FRAGMENT_MAP[currentFragment?.javaClass?.name] != itemId) {
      if (currentChild == null || FRAGMENT_MAP[currentChild?.javaClass?.name] != itemId) {
        val fragment: Fragment = when (itemId) {
          id.action_portfolio -> HomeFragment()
          id.action_widgets -> WidgetsFragment()
          id.action_search -> SearchFragment()
          id.action_settings -> SettingsFragment()
          else -> {
            return false
          }
        }

        supportFragmentManager.beginTransaction().replace(id.fragment_container, fragment)
            .addToBackStack(fragment.javaClass.name).commit()
        currentChild = fragment as ChildFragment
      }
    }
    return true
  }

  // SettingsFragment.Parent

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
    showDialog(getString(R.string.whats_new_in, BuildConfig.VERSION_NAME), stringBuilder.toString())
  }

  // HomeFragment.Parent
  // WidgetSettingsFragment.Parent

  override fun openSearch(widgetId: Int) {
    bottom_navigation.selectedItemId = R.id.action_search
  }
}