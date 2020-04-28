package com.github.premnirmal.ticker.components

import android.content.Context
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.StocksApp
import com.github.premnirmal.ticker.UpdateReceiver
import com.github.premnirmal.ticker.analytics.GeneralProperties
import com.github.premnirmal.ticker.base.BaseFragment
import com.github.premnirmal.ticker.debug.DbViewerActivity
import com.github.premnirmal.ticker.debug.DbViewerViewModel
import com.github.premnirmal.ticker.home.HomeFragment
import com.github.premnirmal.ticker.home.HomePagerAdapter
import com.github.premnirmal.ticker.home.ParanormalActivity
import com.github.premnirmal.ticker.home.SplashActivity
import com.github.premnirmal.ticker.model.AlarmScheduler
import com.github.premnirmal.ticker.model.ExponentialBackoff
import com.github.premnirmal.ticker.model.HistoryProvider
import com.github.premnirmal.ticker.model.RefreshService
import com.github.premnirmal.ticker.model.StocksProvider
import com.github.premnirmal.ticker.network.NewsProvider
import com.github.premnirmal.ticker.network.StocksApi
import com.github.premnirmal.ticker.network.UserAgentInterceptor
import com.github.premnirmal.ticker.news.GraphActivity
import com.github.premnirmal.ticker.news.GraphViewModel
import com.github.premnirmal.ticker.news.NewsFeedViewModel
import com.github.premnirmal.ticker.news.QuoteDetailActivity
import com.github.premnirmal.ticker.news.QuoteDetailViewModel
import com.github.premnirmal.ticker.portfolio.AddPositionActivity
import com.github.premnirmal.ticker.portfolio.PortfolioFragment
import com.github.premnirmal.ticker.portfolio.StocksAdapter
import com.github.premnirmal.ticker.portfolio.search.SearchActivity
import com.github.premnirmal.ticker.portfolio.search.SearchFragment
import com.github.premnirmal.ticker.repo.StocksStorage
import com.github.premnirmal.ticker.settings.SettingsFragment
import com.github.premnirmal.ticker.settings.WidgetSettingsActivity
import com.github.premnirmal.ticker.settings.WidgetSettingsFragment
import com.github.premnirmal.ticker.widget.RefreshReceiver
import com.github.premnirmal.ticker.widget.RemoteStockViewAdapter
import com.github.premnirmal.ticker.widget.StockWidget
import com.github.premnirmal.ticker.widget.WidgetClickReceiver
import com.github.premnirmal.ticker.widget.WidgetData
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import com.github.premnirmal.ticker.widget.WidgetsFragment
import com.google.gson.Gson

/**
 * Created by premnirmal on 3/3/16.
 */
@javax.inject.Singleton
@dagger.Component(modules = arrayOf(AppModule::class))
interface AppComponent {

  // Activities

  fun inject(paranormalActivity: ParanormalActivity)

  fun inject(addPositionActivity: AddPositionActivity)

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

  fun inject(refreshService: RefreshService)

  fun inject(exponentialBackoff: ExponentialBackoff)

  fun inject(generalProperties: GeneralProperties)

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

  fun inject(homeAdapter: HomePagerAdapter)

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
}