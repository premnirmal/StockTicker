package com.github.premnirmal.ticker

import com.github.premnirmal.ticker.model.HistoryProvider
import com.github.premnirmal.ticker.model.StocksProvider
import com.github.premnirmal.ticker.network.StocksApi
import com.github.premnirmal.ticker.portfolio.AddPositionActivity
import com.github.premnirmal.ticker.portfolio.EditPositionActivity
import com.github.premnirmal.ticker.portfolio.GraphActivity
import com.github.premnirmal.ticker.portfolio.PortfolioFragment
import com.github.premnirmal.ticker.portfolio.TickerSelectorActivity
import com.github.premnirmal.ticker.portfolio.drag_drop.RearrangeActivity
import com.github.premnirmal.ticker.settings.SettingsActivity
import com.github.premnirmal.ticker.widget.RemoteStockViewAdapter
import com.github.premnirmal.ticker.widget.StockWidget

/**
 * Created by premnirmal on 2/26/16.
 */
object Injector {

  lateinit var appComponent: AppComponent
  
  internal fun init(ac: AppComponent) {
    appComponent = ac
  }

  fun inject(any: Any) {
    if (any is StocksProvider) {
      appComponent.inject(any)
    } else if (any is HistoryProvider) {
      appComponent.inject(any)
    } else if (any is StocksApi) {
      appComponent.inject(any)
    } else if (any is ParanormalActivity) {
      appComponent.inject(any)
    } else if (any is PortfolioFragment) {
      appComponent.inject(any)
    } else if (any is SettingsActivity) {
      appComponent.inject(any)
    } else if (any is TickerSelectorActivity) {
      appComponent.inject(any)
    } else if (any is RemoteStockViewAdapter) {
      appComponent.inject(any)
    } else if (any is StockWidget) {
      appComponent.inject(any)
    } else if (any is UpdateReceiver) {
      appComponent.inject(any)
    } else if (any is RefreshReceiver) {
      appComponent.inject(any)
    } else if (any is GraphActivity) {
      appComponent.inject(any)
    } else if (any is RearrangeActivity) {
      appComponent.inject(any)
    } else if (any is AddPositionActivity) {
      appComponent.inject(any)
    } else if (any is EditPositionActivity) {
      appComponent.inject(any)
    } else if (any is Tools) {
      appComponent.inject(any)
    } else {
      throw Exception("This class is not injectable in AppComponent!")
    }
  }
}