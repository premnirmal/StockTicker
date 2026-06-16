package com.github.premnirmal.ticker.components

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.work.WorkManager
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.analytics.Analytics
import com.github.premnirmal.ticker.analytics.AnalyticsImpl
import com.github.premnirmal.ticker.analytics.GeneralProperties
import com.github.premnirmal.ticker.components.AppClock.AppClockImpl
import com.github.premnirmal.ticker.home.AppReviewManager
import com.github.premnirmal.ticker.home.IAppReviewManager
import com.github.premnirmal.ticker.model.AlarmScheduler
import com.github.premnirmal.ticker.model.FetchEventLogger
import com.github.premnirmal.ticker.model.StocksProvider
import com.github.premnirmal.ticker.network.StocksApi
import com.github.premnirmal.ticker.repo.QuoteDao
import com.github.premnirmal.ticker.repo.QuotesDB
import com.github.premnirmal.ticker.repo.SharedPreferencesTickersStore
import com.github.premnirmal.ticker.repo.StocksStorage
import com.github.premnirmal.ticker.repo.TickersStore
import com.github.premnirmal.ticker.repo.buildQuotesDB
import com.github.premnirmal.ticker.repo.getQuotesDBBuilder
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    @Singleton @Provides
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Provides @Singleton
    fun provideClock(): AppClock = AppClockImpl

    @Provides @Singleton
    fun provideDefaultSharedPreferences(
        @ApplicationContext context: Context
    ): SharedPreferences =
        context.getSharedPreferences(AppPreferences.PREFS_NAME, MODE_PRIVATE)

    @Provides @Singleton
    fun provideAppWidgetManager(@ApplicationContext context: Context): AppWidgetManager =
        AppWidgetManager.getInstance(context)

    @Provides @Singleton
    fun provideStocksProvider(
        @ApplicationContext context: Context,
        stocksApi: StocksApi,
        sharedPreferences: SharedPreferences,
        appPreferences: AppPreferences,
        clock: AppClock,
        widgetDataProvider: WidgetDataProvider,
        alarmScheduler: AlarmScheduler,
        fetchEventLogger: FetchEventLogger,
        storage: StocksStorage,
        coroutineScope: CoroutineScope
    ): StocksProvider {
        return StocksProvider(
            context,
            stocksApi,
            sharedPreferences,
            clock,
            appPreferences,
            widgetDataProvider,
            alarmScheduler,
            fetchEventLogger,
            storage,
            coroutineScope
        )
    }

    @Provides @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)

    @Provides @Singleton
    fun provideAnalytics(
        @ApplicationContext context: Context,
        properties: dagger.Lazy<GeneralProperties>
    ): Analytics = AnalyticsImpl(context, properties)

    @Provides @Singleton
    fun provideQuotesDB(@ApplicationContext context: Context): QuotesDB {
        return buildQuotesDB(getQuotesDBBuilder(context))
    }

    @Provides @Singleton
    fun provideQuoteDao(db: QuotesDB): QuoteDao = db.quoteDao()

    @Provides @Singleton
    fun provideTickersStore(preferences: SharedPreferences): TickersStore =
        SharedPreferencesTickersStore(preferences)

    @Provides @Singleton
    fun provideStocksStorage(
        tickersStore: TickersStore,
        quoteDao: QuoteDao
    ): StocksStorage = StocksStorage(tickersStore, quoteDao)

    @Provides @Singleton
    fun providesAppReviewManager(@ApplicationContext context: Context): IAppReviewManager {
        return AppReviewManager(context)
    }
}
