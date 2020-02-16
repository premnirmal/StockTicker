package com.github.premnirmal.ticker

import android.content.Context
import android.content.pm.PackageManager
import android.util.Base64
import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDexApplication
import com.github.premnirmal.ticker.analytics.Analytics
import com.github.premnirmal.ticker.components.AppComponent
import com.github.premnirmal.ticker.components.AppModule
import com.github.premnirmal.ticker.components.DaggerAppComponent
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.components.LoggingTree
import com.github.premnirmal.tickerwidget.BuildConfig
import com.github.premnirmal.tickerwidget.R
import com.jakewharton.threetenabp.AndroidThreeTen
import io.github.inflationx.calligraphy3.CalligraphyConfig
import io.github.inflationx.calligraphy3.CalligraphyInterceptor
import io.github.inflationx.viewpump.ViewPump
import io.paperdb.Paper
import timber.log.Timber
import java.security.MessageDigest
import javax.inject.Inject

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
  }


  class InjectionHolder {
    @Inject lateinit var analytics: Analytics
    @Inject lateinit var appPreferences: AppPreferences
  }

  private val holder = InjectionHolder()

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
    Injector.appComponent.inject(holder)
    AppCompatDelegate.setDefaultNightMode(holder.appPreferences.nightMode)
    initPaper()
    SIGNATURE = getAppSignature(this)
    initAnalytics()
    if (BuildConfig.DEBUG) {
      initStetho()
    }
  }

  open fun initStetho() {
    StethoInitializer.initialize(this)
  }

  open fun initPaper() {
    Paper.init(this)
  }

  open fun initThreeTen() {
    AndroidThreeTen.init(this)
  }

  open fun createAppComponent(): AppComponent {
    return DaggerAppComponent.builder()
        .appModule(AppModule(this))
        .build()
  }

  protected open fun initLogger() {
    Timber.plant(LoggingTree(this))
  }

  protected open fun initAnalytics() {
    holder.analytics.initialize(this)
  }
}