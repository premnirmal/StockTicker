package com.github.premnirmal.ticker.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.widget.RemoteViewsService
import com.github.premnirmal.ticker.components.Injector
import javax.inject.Inject

/**
 * Created by premnirmal on 2/27/16.
 */
class RemoteStockProviderService : RemoteViewsService() {

  @Inject lateinit internal var factory: WidgetAdapterFactory

  override fun onGetViewFactory(intent: Intent): RemoteViewsService.RemoteViewsFactory {
    Injector.appComponent.inject(this)
    val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
        AppWidgetManager.INVALID_APPWIDGET_ID)
    return factory.adapterForWidgetId(appWidgetId)
  }
}