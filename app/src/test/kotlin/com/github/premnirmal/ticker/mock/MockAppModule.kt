package com.github.premnirmal.ticker.mock

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.StocksApp
import com.github.premnirmal.ticker.components.AppClock
import com.github.premnirmal.ticker.components.RxBus
import com.github.premnirmal.ticker.model.StocksStorage
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Created by premnirmal on 3/22/17.
 */
@Module(includes = arrayOf(MockNetworkModule::class))
class MockAppModule(private val app: StocksApp) {

  @Provides internal fun provideApplicationContext(): Context = app

  @Provides
  @Singleton internal fun provideClock(): AppClock = Mocker.provide(AppClock::class)

  @Provides
  @Singleton internal fun provideEventBus(): RxBus = RxBus()

  @Provides
  @Singleton internal fun provideMainThreadHandler(): Handler = Handler(Looper.getMainLooper())

  @Provides
  @Singleton internal fun provideDefaultSharedPreferences(
      context: Context): SharedPreferences {
    val sharedPreferences = context.getSharedPreferences(AppPreferences.PREFS_NAME,
        Context.MODE_PRIVATE)
    return sharedPreferences
  }

  @Provides
  @Singleton internal fun provideAppWidgetManager(): AppWidgetManager =
      AppWidgetManager.getInstance(app)

  @Provides
  @Singleton internal fun provideAppPreferences(): AppPreferences = AppPreferences()

  @Provides
  @Singleton internal fun provideStorage(): StocksStorage = StocksStorage()
}