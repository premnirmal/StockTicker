package com.github.premnirmal.ticker.components

import com.github.premnirmal.ticker.home.ParanormalActivity
import com.github.premnirmal.ticker.Tools
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
import com.github.premnirmal.ticker.widget.RemoteStockViewAdapter
import com.github.premnirmal.ticker.widget.StockWidget
import com.github.premnirmal.ticker.widget.WidgetData

/**
 * Created by premnirmal on 2/26/16.
 */
object Injector {

  lateinit var appComponent: AppComponent

  internal fun init(ac: AppComponent) {
    Injector.appComponent = ac
  }

  fun inject(any: Any) {
    if (any is StocksProvider) {
      Injector.appComponent.inject(any)
    } else if (any is WidgetData) {
      Injector.appComponent.inject(any)
    } else if (any is StocksApi) {
      Injector.appComponent.inject(any)
    } else if (any is ParanormalActivity) {
      Injector.appComponent.inject(any)
    } else if (any is PortfolioFragment.InjectionHolder) {
      Injector.appComponent.inject(any)
    } else if (any is SettingsActivity) {
      Injector.appComponent.inject(any)
    } else if (any is TickerSelectorActivity) {
      Injector.appComponent.inject(any)
    } else if (any is RemoteStockViewAdapter) {
      Injector.appComponent.inject(any)
    } else if (any is StockWidget) {
      Injector.appComponent.inject(any)
    } else if (any is UpdateReceiver) {
      Injector.appComponent.inject(any)
    } else if (any is RefreshReceiver) {
      Injector.appComponent.inject(any)
    } else if (any is AddPositionActivity) {
      Injector.appComponent.inject(any)
    } else if (any is EditPositionActivity) {
      Injector.appComponent.inject(any)
    } else if (any is Tools) {
      Injector.appComponent.inject(any)
    } else if (any is StocksStorage) {
      Injector.appComponent.inject(any)
    } else if (any is RequestInterceptor) {
      Injector.appComponent.inject(any)
    } else if (any is UserAgentInterceptor) {
      Injector.appComponent.inject(any)
    } else if (any is StocksAdapter) {
      Injector.appComponent.inject(any)
    } else {
      throw Exception("This class is not injectable in AppComponent!")
    }
  }
}