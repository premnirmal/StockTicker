package com.github.premnirmal.ticker

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.PersistableBundle
import com.github.premnirmal.ticker.base.BaseActivity
import com.github.premnirmal.ticker.components.Analytics
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.portfolio.PortfolioFragment
import com.github.premnirmal.tickerwidget.BuildConfig
import com.github.premnirmal.tickerwidget.R
import javax.inject.Inject

/**
 * Created by premnirmal on 2/25/16.
 */
class ParanormalActivity : BaseActivity() {

  @Inject
  lateinit internal var preferences: SharedPreferences

  private var dialogShown = false

  companion object {
    val DIALOG_SHOWN: String = "DIALOG_SHOWN"
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Injector.inject(this)
    if (savedInstanceState != null) {
      dialogShown = savedInstanceState.getBoolean(DIALOG_SHOWN, false)
    }
    setContentView(R.layout.activity_paranormal)
    if (savedInstanceState == null) {
      val fragment = PortfolioFragment()
      supportFragmentManager.beginTransaction().add(R.id.fragment_container,
          fragment, fragment.javaClass.simpleName).commit()
    }
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
      showDialog("What\'s new in v" + BuildConfig.VERSION_NAME, stringBuilder.toString())
    } else {
      maybeAskToRate()
    }
  }

  override fun onSaveInstanceState(outState: Bundle?, outPersistentState: PersistableBundle?) {
    super.onSaveInstanceState(outState, outPersistentState)
    outState?.putBoolean(DIALOG_SHOWN, dialogShown)
  }

  protected fun maybeAskToRate() {
    if (!dialogShown && Tools.shouldPromptRate()) {
      AlertDialog.Builder(this).setTitle(R.string.like_our_app)
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
    if (marketIntent.resolveActivity(packageManager) != null) {
      startActivity(marketIntent)
    }
  }
}