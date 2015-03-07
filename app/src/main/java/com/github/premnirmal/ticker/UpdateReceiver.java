package com.github.premnirmal.ticker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.github.premnirmal.ticker.model.IStocksProvider;

import javax.inject.Inject;

/**
 * Created by premnirmal on 12/23/14.
 */
public class UpdateReceiver extends BroadcastReceiver {

    @Inject
    IStocksProvider stocksProvider;

    @Inject
    SharedPreferences preferences;

    @Override
    public void onReceive(Context context, Intent intent) {
        Injector.inject(this);
        stocksProvider.fetch();
        preferences.edit().putBoolean(Tools.FIRST_TIME_VIEWING_SWIPELAYOUT, true).apply();
    }
}
