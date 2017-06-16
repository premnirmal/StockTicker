package com.github.premnirmal.ticker.components

import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.UpdateReceiver
import com.github.premnirmal.ticker.home.HomePagerAdapter
import com.github.premnirmal.ticker.home.ParanormalActivity
import com.github.premnirmal.ticker.model.StocksProvider
import com.github.premnirmal.ticker.model.StocksStorage
import com.github.premnirmal.ticker.network.RequestInterceptor
import com.github.premnirmal.ticker.network.StocksApi
import com.github.premnirmal.ticker.network.UserAgentInterceptor
import com.github.premnirmal.ticker.portfolio.AddPositionActivity
import com.github.premnirmal.ticker.portfolio.EditPositionActivity
import com.github.premnirmal.ticker.portfolio.PortfolioFragment
import com.github.premnirmal.ticker.portfolio.StocksAdapter
import com.github.premnirmal.ticker.portfolio.search.TickerSelectorActivity
import com.github.premnirmal.ticker.settings.SettingsActivity
import com.github.premnirmal.ticker.settings.WidgetSettingsActivity
import com.github.premnirmal.ticker.widget.RefreshReceiver
import com.github.premnirmal.ticker.widget.RemoteStockViewAdapter
import com.github.premnirmal.ticker.widget.StockWidget
import com.github.premnirmal.ticker.widget.WidgetClickReceiver
import com.github.premnirmal.ticker.widget.WidgetData
import com.github.premnirmal.ticker.widget.WidgetDataProvider

/**
 * Created by premnirmal on 3/3/16.
 */
@javax.inject.Singleton
@dagger.Component(
    modules = arrayOf(AppModule::class)
)
interface AppComponent {

  fun inject(stocksStorage: StocksStorage)

  fun inject(appPreferences: AppPreferences)

  fun inject(stocksProvider: StocksProvider)

  fun inject(widgetDataProvider: WidgetDataProvider)

  fun inject(widgetData: WidgetData)

  fun inject(stocksApi: StocksApi)

  fun inject(paranormalActivity: ParanormalActivity)

  fun inject(homeAdapter: HomePagerAdapter)

  fun inject(holder: PortfolioFragment.InjectionHolder)

  fun inject(settingsActivity: SettingsActivity)

  fun inject(tickerSelectorActivity: TickerSelectorActivity)

  fun inject(remoteStockViewAdapter: RemoteStockViewAdapter)

  fun inject(stockWidget: StockWidget)

  fun inject(updateReceiver: UpdateReceiver)

  fun inject(refreshReceiver: RefreshReceiver)

  fun inject(addPositionActivity: AddPositionActivity)

  fun inject(editPositionActivity: EditPositionActivity)

  fun inject(interceptor: RequestInterceptor)

  fun inject(interceptor: UserAgentInterceptor)

  fun inject(stocksAdapter: StocksAdapter)

  fun inject(widgetSettingsActivity: WidgetSettingsActivity)

  fun inject(widgetClickReceiver: WidgetClickReceiver)

}