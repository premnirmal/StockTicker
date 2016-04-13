package com.github.premnirmal.ticker

import com.github.premnirmal.ticker.portfolio.*
import com.github.premnirmal.ticker.portfolio.drag_drop.RearrangeActivity
import com.github.premnirmal.ticker.settings.SettingsActivity
import com.github.premnirmal.ticker.widget.RemoteStockViewAdapter
import com.github.premnirmal.ticker.widget.StockWidget
import dagger.Component
import javax.inject.Singleton

/**
 * Created on 3/3/16.
 */
@Singleton
@Component(
    modules = arrayOf(AppModule::class)
)
interface AppComponent {

  fun inject(paranormalActivity: ParanormalActivity)

  fun inject(portfolioFragment: PortfolioFragment)

  fun inject(settingsActivity: SettingsActivity)

  fun inject(tickerSelectorActivity: TickerSelectorActivity)

  fun inject(remoteStockViewAdapter: RemoteStockViewAdapter)

  fun inject(stockWidget: StockWidget)

  fun inject(updateReceiver: UpdateReceiver)

  fun inject(refreshReceiver: RefreshReceiver)

  fun inject(graphActivity: GraphActivity)

  fun inject(rearrangeActivity: RearrangeActivity)

  fun inject(addPositionActivity: AddPositionActivity)

  fun inject(editPositionActivity: EditPositionActivity)

  fun inject(refreshWakefulService: RefreshWakefulService)

}