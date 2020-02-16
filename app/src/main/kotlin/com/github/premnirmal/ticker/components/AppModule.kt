package com.github.premnirmal.ticker.components

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import androidx.room.Room
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.StocksApp
import com.github.premnirmal.ticker.analytics.Analytics
import com.github.premnirmal.ticker.analytics.AnalyticsImpl
import com.github.premnirmal.ticker.components.AppClock.AppClockImpl
import com.github.premnirmal.ticker.repo.StocksStorage
import com.github.premnirmal.ticker.network.NetworkModule
import com.github.premnirmal.ticker.repo.QuoteDao
import com.github.premnirmal.ticker.repo.QuotesDB
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Created by premnirmal on 3/3/16.
 */
@Module(includes = [NetworkModule::class])
class AppModule(private val app: StocksApp) {

  @Provides fun provideApplicationContext(): Context = app

  @Provides @Singleton fun provideClock(): AppClock = AppClockImpl()

  @Provides @Singleton fun provideEventBus(): AsyncBus = AsyncBus()

  @Provides @Singleton fun provideMainThreadHandler(): Handler =
    Handler(Looper.getMainLooper())

  @Provides @Singleton fun provideDefaultSharedPreferences(
    context: Context
  ): SharedPreferences =
    context.getSharedPreferences(AppPreferences.PREFS_NAME, MODE_PRIVATE)

  @Provides @Singleton fun provideAppWidgetManager(): AppWidgetManager =
    AppWidgetManager.getInstance(app)

  @Provides @Singleton fun provideAppPreferences(): AppPreferences = AppPreferences()

  @Provides @Singleton fun provideAnalytics(): Analytics = AnalyticsImpl()

  @Provides @Singleton fun provideStorage(): StocksStorage =
    StocksStorage()
  
  @Provides @Singleton fun provideQuotesDB(context: Context): QuotesDB {
    return Room.databaseBuilder(
        context.applicationContext,
        QuotesDB::class.java, "quotes-db"
    ).build()
  }

  @Provides @Singleton fun provideQuoteDao(db: QuotesDB): QuoteDao {
    return db.quoteDao()
  }
}