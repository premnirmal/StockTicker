package com.github.premnirmal.ticker.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.widget.RemoteViewsService

/**
 * Created by premnirmal on 2/27/16.
 */
class RemoteStockProviderService : RemoteViewsService() {

  override fun onGetViewFactory(intent: Intent): RemoteViewsService.RemoteViewsFactory {
    val appWidgetId =
      intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
    return RemoteStockViewAdapter(appWidgetId)
  }
}