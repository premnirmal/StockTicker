package com.github.premnirmal.ticker.components

import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.UpdateReceiver
import com.github.premnirmal.ticker.model.RefreshWorker
import com.github.premnirmal.ticker.notifications.DailySummaryNotificationReceiver
import com.github.premnirmal.ticker.widget.RefreshReceiver
import com.github.premnirmal.ticker.widget.RemoteStockViewAdapter
import com.github.premnirmal.ticker.widget.StockWidget
import com.github.premnirmal.ticker.widget.WidgetClickReceiver
import com.github.premnirmal.ticker.widget.WidgetData
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json

/**
 * Created by premnirmal on 3/3/16.
 */

interface LegacyComponent {
    fun json(): Json
    fun appPreferences(): AppPreferences
    fun inject(widget: StockWidget)
    fun inject(data: WidgetData)
    fun inject(adapter: RemoteStockViewAdapter)
    fun inject(receiver: WidgetClickReceiver)
    fun inject(receiver: RefreshReceiver)
    fun inject(receiver: UpdateReceiver)
    fun inject(receiver: DailySummaryNotificationReceiver)
    fun inject(refreshWorker: RefreshWorker)
}

@InstallIn(SingletonComponent::class)
@EntryPoint
interface AppEntryPoint : LegacyComponent
