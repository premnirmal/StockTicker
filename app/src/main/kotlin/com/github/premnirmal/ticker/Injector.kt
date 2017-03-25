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
class Injector private constructor(val appComponent: AppComponent) {

  init {
    instance = this
  }

  companion object {

    lateinit var instance: Injector

    internal fun init(appComponent: AppComponent) {
      instance = Injector(appComponent)
    }

    fun inject(any: Any) {
      if (any is StocksProvider) {
        instance.appComponent.inject(any)
      } else if (any is HistoryProvider) {
        instance.appComponent.inject(any)
      }  else if (any is StocksApi) {
        instance.appComponent.inject(any)
      } else if (any is ParanormalActivity) {
        instance.appComponent.inject(any)
      }else if (any is PortfolioFragment) {
        instance.appComponent.inject(any)
      } else if (any is SettingsActivity) {
        instance.appComponent.inject(any)
      } else if (any is TickerSelectorActivity) {
        instance.appComponent.inject(any)
      } else if (any is RemoteStockViewAdapter) {
        instance.appComponent.inject(any)
      } else if (any is StockWidget) {
        instance.appComponent.inject(any)
      } else if (any is UpdateReceiver) {
        instance.appComponent.inject(any)
      } else if (any is RefreshReceiver) {
        instance.appComponent.inject(any)
      } else if (any is GraphActivity) {
        instance.appComponent.inject(any)
      } else if (any is RearrangeActivity) {
        instance.appComponent.inject(any)
      } else if (any is AddPositionActivity) {
        instance.appComponent.inject(any)
      } else if (any is EditPositionActivity) {
        instance.appComponent.inject(any)
      } else {
        throw Exception("This class is not injectable in AppComponent!")
      }
    }
  }

}