package com.github.premnirmal.ticker.widget;

import android.content.Intent;
import android.widget.RemoteViewsService;

/**
 * Created by premnirmal on 12/21/14.
 */
public class RemoteStockProviderService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteStockViewAdapter(getApplicationContext());
    }
}
