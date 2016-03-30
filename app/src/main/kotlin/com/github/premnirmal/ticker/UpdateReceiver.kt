package com.github.premnirmal.ticker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.tickerwidget.R
import java.util.*
import javax.inject.Inject

/**
 * Created by premnirmal on 2/27/16.
 */
class UpdateReceiver : BroadcastReceiver() {

  @Inject
  lateinit internal var stocksProvider: IStocksProvider

  @Inject
  lateinit internal var preferences: SharedPreferences

  internal val random = Random(System.currentTimeMillis())

  override fun onReceive(context: Context, intent: Intent) {
    Injector.getAppComponent().inject(this)
    val path = context.getString(R.string.package_replaced_string)
    val intentData = intent.dataString
    if (path == intentData || "package:" + path == intentData) {
      stocksProvider.fetch()
      preferences.edit().putBoolean(Tools.FIRST_TIME_VIEWING_SWIPELAYOUT, true).apply()
      preferences.edit().putBoolean(Tools.WHATS_NEW, true).apply()
    } else if (random.nextInt() % 2 == 0) {
      // randomly change this string so the user sees the animation
      preferences.edit().putBoolean(Tools.FIRST_TIME_VIEWING_SWIPELAYOUT, true).apply()
    }
  }
}