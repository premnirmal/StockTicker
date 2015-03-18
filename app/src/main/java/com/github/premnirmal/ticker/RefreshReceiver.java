package com.github.premnirmal.ticker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.github.premnirmal.ticker.model.IStocksProvider;

import javax.inject.Inject;

/**
 * Created by premnirmal on 3/16/15.
 */
public class RefreshReceiver extends BroadcastReceiver {

    @Inject
    IStocksProvider stocksProvider;

    @Override
    public void onReceive(Context context, Intent intent) {
        Injector.inject(this);
        stocksProvider.fetch();
    }
}
