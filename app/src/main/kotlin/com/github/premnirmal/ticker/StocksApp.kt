package com.github.premnirmal.ticker

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.util.Base64
import com.github.premnirmal.ticker.components.Analytics
import com.github.premnirmal.ticker.components.AppComponent
import com.github.premnirmal.ticker.components.AppModule
import com.github.premnirmal.ticker.components.DaggerAppComponent
import com.github.premnirmal.ticker.components.LoggingTree
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.tickerwidget.R
import com.jakewharton.threetenabp.AndroidThreeTen
import timber.log.Timber
import uk.co.chrisjenx.calligraphy.CalligraphyConfig
import java.security.MessageDigest

/**
 * Created by premnirmal on 2/26/16.
 */
open class StocksApp : Application() {

  companion object {

    var SIGNATURE: String? = null

    fun getAppSignature(context: Context): String? {
      try {
        val packageInfo = context.packageManager
            .getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
        packageInfo.signatures.forEach {
          val md = MessageDigest.getInstance("SHA")
          md.update(it.toByteArray())
          val currentSignature = Base64.encodeToString(md.digest(), Base64.DEFAULT).trim()
          return currentSignature
        }
      } catch (e: Exception) {
        Timber.e(e)
      }
      return null
    }

    fun Context.getNavigationBarHeight(): Int {
      val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
      if (resourceId > 0) {
        return resources.getDimensionPixelSize(resourceId)
      }
      return 0
    }
  }

  override fun onCreate() {
    super.onCreate()
    initLogger()
    initThreeTen()
    CalligraphyConfig.initDefault(
        CalligraphyConfig.Builder()
            .setDefaultFontPath("fonts/Ubuntu-Regular.ttf")
            .setFontAttrId(R.attr.fontPath)
            .build())
    Injector.init(createAppComponent())
    initAnalytics()
    SIGNATURE = getAppSignature(this)
  }

  open fun initThreeTen() {
    AndroidThreeTen.init(this)
  }

  open fun createAppComponent(): AppComponent {
    val component: AppComponent = DaggerAppComponent.builder()
        .appModule(AppModule(this))
        .build()
    return component
  }

  protected open fun initAnalytics() {
    Analytics.init(this)
  }

  protected open fun initLogger() {
    Timber.plant(LoggingTree(this))
  }
}