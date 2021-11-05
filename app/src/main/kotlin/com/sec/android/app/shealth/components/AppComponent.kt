package com.sec.android.app.shealth.components

import android.content.Context
import com.sec.android.app.shealth.AppPreferences
import com.sec.android.app.shealth.StocksApp
import com.sec.android.app.shealth.UpdateReceiver
import com.sec.android.app.shealth.analytics.GeneralProperties
import com.sec.android.app.shealth.base.BaseFragment
import com.sec.android.app.shealth.debug.DbViewerActivity
import com.sec.android.app.shealth.debug.DbViewerViewModel
import com.sec.android.app.shealth.home.HomeFragment
import com.sec.android.app.shealth.home.HomeViewModel
import com.sec.android.app.shealth.home.ParanormalActivity
import com.sec.android.app.shealth.home.SplashActivity
import com.sec.android.app.shealth.model.AlarmScheduler
import com.sec.android.app.shealth.model.ExponentialBackoff
import com.sec.android.app.shealth.model.HistoryProvider
import com.sec.android.app.shealth.model.RefreshWorker
import com.sec.android.app.shealth.model.StocksProvider
import com.sec.android.app.shealth.network.NewsProvider
import com.sec.android.app.shealth.network.StocksApi
import com.sec.android.app.shealth.network.UserAgentInterceptor
import com.sec.android.app.shealth.news.GraphActivity
import com.sec.android.app.shealth.news.GraphViewModel
import com.sec.android.app.shealth.news.NewsFeedViewModel
import com.sec.android.app.shealth.news.QuoteDetailActivity
import com.sec.android.app.shealth.news.QuoteDetailViewModel
import com.sec.android.app.shealth.notifications.DailySummaryNotificationReceiver
import com.sec.android.app.shealth.portfolio.AddAlertsActivity
import com.sec.android.app.shealth.portfolio.AddNotesActivity
import com.sec.android.app.shealth.portfolio.AddPositionActivity
import com.sec.android.app.shealth.portfolio.AlertsViewModel
import com.sec.android.app.shealth.portfolio.NotesViewModel
import com.sec.android.app.shealth.portfolio.PortfolioFragment
import com.sec.android.app.shealth.portfolio.StocksAdapter
import com.sec.android.app.shealth.portfolio.search.SearchActivity
import com.sec.android.app.shealth.portfolio.search.SearchFragment
import com.sec.android.app.shealth.portfolio.search.SearchViewModel
import com.sec.android.app.shealth.repo.StocksStorage
import com.sec.android.app.shealth.settings.SettingsFragment
import com.sec.android.app.shealth.settings.WidgetSettingsActivity
import com.sec.android.app.shealth.settings.WidgetSettingsFragment
import com.sec.android.app.shealth.widget.RefreshReceiver
import com.sec.android.app.shealth.widget.RemoteStockViewAdapter
import com.sec.android.app.shealth.widget.StockWidget
import com.sec.android.app.shealth.widget.WidgetClickReceiver
import com.sec.android.app.shealth.widget.WidgetData
import com.sec.android.app.shealth.widget.WidgetDataProvider
import com.sec.android.app.shealth.widget.WidgetsFragment
import com.google.gson.Gson

/**
 * Created by android on 3/3/16.
 */
@javax.inject.Singleton
@dagger.Component(modules = arrayOf(AppModule::class))
interface AppComponent {

  // Activities

  fun inject(paranormalActivity: ParanormalActivity)

  fun inject(addPositionActivity: AddPositionActivity)

  fun inject(addNotesActivity: AddNotesActivity)

  fun inject(addAlertsActivity: AddAlertsActivity)

  fun inject(splashActivity: SplashActivity)

  fun inject(newsFeedActivity: QuoteDetailActivity)

  fun inject(graphActivity: GraphActivity)

  fun inject(searchActivity: SearchActivity)

  fun inject(widgetSettingsActivity: WidgetSettingsActivity)

  fun inject(dbViewerActivity: DbViewerActivity)

  // Components

  fun inject(stocksApp: StocksApp.InjectionHolder)

  fun inject(stocksStorage: StocksStorage)

  fun inject(appPreferences: AppPreferences)

  fun inject(stocksProvider: StocksProvider)

  fun inject(historicalDataProvider: HistoryProvider)

  fun inject(alarmScheduler: AlarmScheduler)

  fun inject(updateReceiver: UpdateReceiver)

  fun inject(refreshReceiver: RefreshReceiver)

  fun inject(refreshWorker: RefreshWorker)

  fun inject(exponentialBackoff: ExponentialBackoff)

  fun inject(generalProperties: GeneralProperties)

  fun inject(dailySummaryNotificationReceiver: DailySummaryNotificationReceiver)

  // Network

  fun inject(stocksApi: StocksApi)

  fun inject(newsProvider: NewsProvider)

  fun inject(interceptor: UserAgentInterceptor)

  // Widget

  fun inject(stockWidget: StockWidget)

  fun inject(widgetClickReceiver: WidgetClickReceiver)

  fun inject(widgetDataProvider: WidgetDataProvider)

  fun inject(widgetData: WidgetData)

  fun inject(remoteStockViewAdapter: RemoteStockViewAdapter)

  // UI

  fun inject(holder: BaseFragment.InjectionHolder)

  fun inject(holder: PortfolioFragment.InjectionHolder)

  fun inject(homeFragment: HomeFragment)

  fun inject(fragment: SearchFragment)

  fun inject(settingsFragment: SettingsFragment)

  fun inject(stocksAdapter: StocksAdapter)

  fun inject(fragment: WidgetsFragment)

  fun inject(widgetSettingsFragment: WidgetSettingsFragment)

  fun appContext(): Context

  fun gson(): Gson

  // ViewModels

  fun inject(dbViewerViewModel: DbViewerViewModel)

  fun inject(quoteDetailViewModel: QuoteDetailViewModel)

  fun inject(graphViewModel: GraphViewModel)

  fun inject(newsFeedViewModel: NewsFeedViewModel)

  fun inject(totalHoldingsViewModel: HomeViewModel)

  fun inject(notesViewModel: NotesViewModel)

  fun inject(alertsViewModel: AlertsViewModel)

  fun inject(searchViewModel: SearchViewModel)
}
