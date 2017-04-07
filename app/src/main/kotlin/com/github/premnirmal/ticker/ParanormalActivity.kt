package com.github.premnirmal.ticker

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.PersistableBundle
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
    if (savedInstanceState != null) {
      dialogShown = savedInstanceState.getBoolean(DIALOG_SHOWN, false)
    }
    Injector.inject(this)
    setContentView(R.layout.activity_paranormal)
    val fragment = PortfolioFragment()
    supportFragmentManager.beginTransaction().add(R.id.fragment_container,
        fragment, fragment.javaClass.simpleName).commit()
    if (preferences.getBoolean(Tools.WHATS_NEW, false)) {
      preferences.edit().putBoolean(Tools.WHATS_NEW, false).apply()
      val stringBuilder = StringBuilder()
      val whatsNew = resources.getStringArray(R.array.whats_new)
      for (i in whatsNew.indices) {
        stringBuilder.append("\t - ")
        stringBuilder.append(whatsNew[i])
        if (i != whatsNew.size - 1) {
          stringBuilder.append("\n")
        }
      }
      AlertDialog.Builder(this).setTitle("What\'s new in Version " + BuildConfig.VERSION_NAME)
          .setMessage(stringBuilder.toString())
          .setNeutralButton("OK") { dialog, which -> dialog.dismiss() }
          .show()
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
          .setPositiveButton(R.string.yes) { dialog, which ->
            sendToPlayStore()
            Analytics.trackRateYes()
            Tools.userDidRate()
            dialog.dismiss()
          }
          .setNegativeButton(R.string.no) { dialog, which ->
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
    startActivity(marketIntent)
  }
}