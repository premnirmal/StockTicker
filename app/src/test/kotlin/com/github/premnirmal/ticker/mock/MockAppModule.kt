package com.github.premnirmal.ticker.mock

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.SharedPreferences
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.StocksApp
import com.github.premnirmal.ticker.analytics.Analytics
import com.github.premnirmal.ticker.components.AppClock
import com.github.premnirmal.ticker.components.AsyncBus
import com.github.premnirmal.ticker.model.AlarmScheduler
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.model.StocksStorage
import com.github.premnirmal.ticker.network.NewsProvider
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Created by premnirmal on 3/22/17.
 */
@Module(includes = arrayOf(MockNetworkModule::class))
class MockAppModule(private val app: StocksApp) {

  @Provides internal fun provideApplicationContext(): Context = app

  @Provides @Singleton internal fun provideClock(): AppClock = Mocker.provide(AppClock::class)

  @Provides @Singleton internal fun provideEventBus(): AsyncBus = AsyncBus()

  @Provides @Singleton internal fun provideDefaultSharedPreferences(
    context: Context): SharedPreferences {
    val sharedPreferences =
      context.getSharedPreferences(AppPreferences.PREFS_NAME, Context.MODE_PRIVATE)
    return sharedPreferences
  }

  @Provides @Singleton internal fun provideAppWidgetManager(): AppWidgetManager =
    AppWidgetManager.getInstance(app)

  @Provides @Singleton internal fun provideAppPreferences(): AppPreferences = AppPreferences()

  @Provides @Singleton internal fun provideStorage(): StocksStorage = StocksStorage()

  @Provides @Singleton internal fun provideAnalytics(): Analytics = Mocker.provide(Analytics::class)

  @Provides @Singleton internal fun provideStocksProvider(): IStocksProvider =
    Mocker.provide(IStocksProvider::class)

  @Provides @Singleton internal fun provideWidgetDataFactory(): WidgetDataProvider =
    Mocker.provide(WidgetDataProvider::class)

  @Provides @Singleton internal fun provideNewsProvider(): NewsProvider =
    Mocker.provide(NewsProvider::class)

  @Provides @Singleton internal fun provideAlarmScheduler(): AlarmScheduler =
    Mocker.provide(AlarmScheduler::class)
}