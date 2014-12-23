package com.github.premnirmal.ticker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.github.premnirmal.ticker.model.IStocksProvider;

import javax.inject.Inject;

/**
 * Created by premnirmal on 12/23/14.
 */
public class UpdateReceiver extends BroadcastReceiver {

    @Inject
    IStocksProvider stocksProvider;

    @Override
    public void onReceive(Context context, Intent intent) {
        ((StocksApp)context.getApplicationContext()).inject(this);
        stocksProvider.fetch();
    }
}
