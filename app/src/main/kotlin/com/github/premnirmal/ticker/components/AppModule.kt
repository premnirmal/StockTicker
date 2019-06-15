package com.github.premnirmal.ticker.components

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.StocksApp
import com.github.premnirmal.ticker.components.AppClock.AppClockImpl
import com.github.premnirmal.ticker.model.StocksStorage
import com.github.premnirmal.ticker.network.NetworkModule
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Created by premnirmal on 3/3/16.
 */
@Module(includes = [NetworkModule::class])
class AppModule(private val app: StocksApp) {

  @Provides internal fun provideApplicationContext(): Context = app

  @Provides @Singleton internal fun provideClock(): AppClock = AppClockImpl()

  @Provides @Singleton internal fun provideEventBus(): RxBus = RxBus()

  @Provides @Singleton internal fun provideMainThreadHandler(): Handler =
    Handler(Looper.getMainLooper())

  @Provides @Singleton internal fun provideDefaultSharedPreferences(
    context: Context
  ): SharedPreferences =
    context.getSharedPreferences(AppPreferences.PREFS_NAME, MODE_PRIVATE)

  @Provides @Singleton internal fun provideAppWidgetManager(): AppWidgetManager =
    AppWidgetManager.getInstance(app)

  @Provides @Singleton internal fun provideAppPreferences(): AppPreferences = AppPreferences()

  @Provides @Singleton internal fun provideStorage(): StocksStorage = StocksStorage()
}