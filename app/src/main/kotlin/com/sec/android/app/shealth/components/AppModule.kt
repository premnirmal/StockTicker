package com.sec.android.app.shealth.components

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import androidx.room.Room
import com.sec.android.app.shealth.AppPreferences
import com.sec.android.app.shealth.StocksApp
import com.sec.android.app.shealth.analytics.Analytics
import com.sec.android.app.shealth.analytics.AnalyticsImpl
import com.sec.android.app.shealth.components.AppClock.AppClockImpl
import com.sec.android.app.shealth.network.NetworkModule
import com.sec.android.app.shealth.repo.QuoteDao
import com.sec.android.app.shealth.repo.QuotesDB
import com.sec.android.app.shealth.repo.StocksStorage
import com.sec.android.app.shealth.repo.migrations.MIGRATION_1_2
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Created by android on 3/3/16.
 */
@Module(includes = [NetworkModule::class])
class AppModule(private val app: StocksApp) {

  @Provides fun provideApplicationContext(): Context = app

  @Provides @Singleton fun provideClock(): AppClock = AppClockImpl

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
    )
        .addMigrations(MIGRATION_1_2)
        .build()
  }

  @Provides @Singleton fun provideQuoteDao(db: QuotesDB): QuoteDao {
    return db.quoteDao()
  }
}