package com.github.premnirmal.ticker

import android.content.Context
import android.content.pm.PackageManager
import android.util.Base64
import androidx.multidex.MultiDexApplication
import com.github.premnirmal.ticker.analytics.Analytics
import com.github.premnirmal.ticker.components.AppComponent
import com.github.premnirmal.ticker.components.AppModule
import com.github.premnirmal.ticker.components.DaggerAppComponent
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.components.LoggingTree
import com.github.premnirmal.tickerwidget.R
import com.jakewharton.threetenabp.AndroidThreeTen
import io.paperdb.Paper
import timber.log.Timber
import java.security.MessageDigest
import io.github.inflationx.calligraphy3.CalligraphyConfig
import io.github.inflationx.calligraphy3.CalligraphyInterceptor
import io.github.inflationx.viewpump.ViewPump

/**
 * Created by premnirmal on 2/26/16.
 */
open class StocksApp : MultiDexApplication() {

  companion object {

    var SIGNATURE: String? = null

    fun getAppSignature(context: Context): String? {
      try {
        val packageInfo =
          context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
        packageInfo.signatures.forEach {
          val md = MessageDigest.getInstance("SHA")
          md.update(it.toByteArray())
          val currentSignature = Base64.encodeToString(md.digest(), Base64.DEFAULT)
              .trim()
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
    ViewPump.init(
        ViewPump.builder()
            .addInterceptor(
                CalligraphyInterceptor(
                    CalligraphyConfig.Builder()
                        .setDefaultFontPath("fonts/Ubuntu-Regular.ttf")
                        .setFontAttrId(R.attr.fontPath)
                        .build()))
            .build())
    Injector.init(createAppComponent())
    initPaper()
    SIGNATURE = getAppSignature(this)
  }

  open fun initPaper() {
    Paper.init(this)
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

  protected open fun initLogger() {
    Timber.plant(LoggingTree(this))
  }
}