package com.github.premnirmal.ticker.widget

import android.content.Intent
import android.widget.RemoteViewsService

/**
 * Created by premnirmal on 2/27/16.
 */
class RemoteStockProviderService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsService.RemoteViewsFactory {
        return RemoteStockViewAdapter(applicationContext)
    }
}