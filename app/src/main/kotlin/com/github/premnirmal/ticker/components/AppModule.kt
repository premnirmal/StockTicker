package com.github.premnirmal.ticker.components

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.room.Room
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.StocksApp
import com.github.premnirmal.ticker.analytics.Analytics
import com.github.premnirmal.ticker.analytics.AnalyticsImpl
import com.github.premnirmal.ticker.components.AppClock.AppClockImpl
import com.github.premnirmal.ticker.network.NetworkModule
import com.github.premnirmal.ticker.repo.QuoteDao
import com.github.premnirmal.ticker.repo.QuotesDB
import com.github.premnirmal.ticker.repo.StocksStorage
import com.github.premnirmal.ticker.repo.migrations.MIGRATION_1_2
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/**
 * Created by premnirmal on 3/3/16.
 */
@Module(includes = [NetworkModule::class])
class AppModule(private val app: StocksApp) {

  @Provides fun provideApplicationContext(): Context = app

  @Singleton @Provides fun provideApplicationScope(): CoroutineScope {
    return CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
  }

  @Provides @Singleton fun provideClock(): AppClock = AppClockImpl

  @Provides @Singleton fun provideDefaultSharedPreferences(
    context: Context
  ): SharedPreferences =
    context.getSharedPreferences(AppPreferences.PREFS_NAME, MODE_PRIVATE)

  @Provides @Singleton fun provideAppWidgetManager(): AppWidgetManager =
    AppWidgetManager.getInstance(app)

  @Provides @Singleton internal fun provideWidgetDataProvider(): WidgetDataProvider =
    WidgetDataProvider()

  @Provides @Singleton fun provideAppPreferences(): AppPreferences = AppPreferences()

  @Provides @Singleton fun provideAnalytics(): Analytics = AnalyticsImpl()

  @Provides @Singleton fun provideStorage(): StocksStorage =
    StocksStorage()

  @Provides @Singleton fun provideQuotesDB(context: Context): QuotesDB {
    return Room.databaseBuilder(
        context.applicationContext,
        QuotesDB::class.java, "quotes-db"
    )
        .addMigrations(MIGRATION_1_2)
        .build()
  }

  @Provides @Singleton fun provideQuoteDao(db: QuotesDB): QuoteDao {
    return db.quoteDao()
  }
}