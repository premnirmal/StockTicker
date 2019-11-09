package com.github.premnirmal.ticker.components

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.StocksApp
import com.github.premnirmal.ticker.analytics.Analytics
import com.github.premnirmal.ticker.analytics.AnalyticsImpl
import com.github.premnirmal.ticker.components.AppClock.AppClockImpl
import com.github.premnirmal.ticker.model.AlarmScheduler
import com.github.premnirmal.ticker.model.HistoryProvider
import com.github.premnirmal.ticker.model.IHistoryProvider
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.model.StocksProvider
import com.github.premnirmal.ticker.model.StocksStorage
import com.github.premnirmal.ticker.network.NetworkModule
import com.github.premnirmal.ticker.network.NewsProvider
import com.github.premnirmal.ticker.widget.WidgetDataProvider
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

  @Provides @Singleton internal fun provideEventBus(): AsyncBus = AsyncBus()

  @Provides @Singleton internal fun provideDefaultSharedPreferences(
    context: Context
  ): SharedPreferences =
    context.getSharedPreferences(AppPreferences.PREFS_NAME, MODE_PRIVATE)

  @Provides @Singleton internal fun provideAppWidgetManager(): AppWidgetManager =
    AppWidgetManager.getInstance(app)

  @Provides @Singleton internal fun provideAppPreferences(): AppPreferences = AppPreferences()

  @Provides @Singleton internal fun provideStorage(): StocksStorage = StocksStorage()

  @Provides @Singleton internal fun provideAnalytics(): Analytics = AnalyticsImpl()

  @Provides @Singleton internal fun provideNewsProvider(): NewsProvider = NewsProvider()

  @Provides @Singleton internal fun provideStocksProvider(): IStocksProvider = StocksProvider()

  @Provides @Singleton internal fun provideHistoricalDataProvider(): IHistoryProvider =
    HistoryProvider()

  @Provides @Singleton internal fun provideAlarmScheduler(): AlarmScheduler = AlarmScheduler()

  @Provides @Singleton internal fun provideWidgetDataFactory(): WidgetDataProvider =
    WidgetDataProvider()
}