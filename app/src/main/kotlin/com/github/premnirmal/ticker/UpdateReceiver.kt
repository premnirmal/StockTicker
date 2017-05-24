package com.github.premnirmal.ticker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.tickerwidget.R
import javax.inject.Inject

/**
 * Created by premnirmal on 2/27/16.
 */
class UpdateReceiver : BroadcastReceiver() {

  @Inject lateinit internal var stocksProvider: IStocksProvider
  @Inject lateinit internal var preferences: SharedPreferences

  override fun onReceive(context: Context, intent: Intent) {
    Injector.inject(this)
    val path = context.getString(R.string.package_replaced_string)
    val intentData = intent.dataString
    if (path == intentData ||
        context.packageName == intentData ||
        "package:" + path == intentData ||
        "package:" + context.packageName == intentData
        ) {
      stocksProvider.fetch().subscribe(SimpleSubscriber())
      preferences.edit().putBoolean(Tools.WHATS_NEW, true).apply()
    }
  }
}