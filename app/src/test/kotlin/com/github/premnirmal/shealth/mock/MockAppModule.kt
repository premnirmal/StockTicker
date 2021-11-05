package com.sec.android.app.shealth.mock

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import com.sec.android.app.shealth.AppPreferences
import com.sec.android.app.shealth.StocksApp
import com.sec.android.app.shealth.analytics.Analytics
import com.sec.android.app.shealth.components.AppClock
import com.sec.android.app.shealth.components.AsyncBus
import com.sec.android.app.shealth.repo.StocksStorage
import com.sec.android.app.shealth.repo.QuoteDao
import com.sec.android.app.shealth.repo.QuotesDB
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Created by android on 3/22/17.
 */
@Module(includes = arrayOf(MockNetworkModule::class))
class MockAppModule(private val app: StocksApp) {

  @Provides internal fun provideApplicationContext(): Context = app

  @Provides @Singleton internal fun provideClock(): AppClock = Mocker.provide(AppClock::class)

  @Provides @Singleton internal fun provideEventBus(): AsyncBus = AsyncBus()

  @Provides @Singleton internal fun provideMainThreadHandler(): Handler =
    Handler(Looper.getMainLooper())

  @Provides @Singleton internal fun provideDefaultSharedPreferences(
    context: Context): SharedPreferences {
    return context.getSharedPreferences(AppPreferences.PREFS_NAME, Context.MODE_PRIVATE)
  }

  @Provides @Singleton internal fun provideAppWidgetManager(): AppWidgetManager =
    AppWidgetManager.getInstance(app)

  @Provides @Singleton internal fun provideAppPreferences(): AppPreferences = AppPreferences()

  @Provides @Singleton internal fun provideStorage(): StocksStorage =
    StocksStorage()

  @Provides @Singleton internal fun provideAnalytics(): Analytics = Mocker.provide(Analytics::class)

  @Provides @Singleton fun provideQuotesDB(context: Context): QuotesDB {
    return Mocker.provide(QuotesDB::class)
  }

  @Provides @Singleton fun provideQuoteDao(db: QuotesDB): QuoteDao {
    return Mocker.provide(QuoteDao::class)
  }
}