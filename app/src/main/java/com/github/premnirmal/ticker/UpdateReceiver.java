package com.github.premnirmal.ticker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.github.premnirmal.ticker.model.IStocksProvider;
import com.github.premnirmal.tickerwidget.R;

import java.util.Random;

import javax.inject.Inject;

/**
 * Created by premnirmal on 12/23/14.
 */
public class UpdateReceiver extends BroadcastReceiver {

    @Inject
    IStocksProvider stocksProvider;

    @Inject
    SharedPreferences preferences;

    final Random random = new Random(System.currentTimeMillis());

    @Override
    public void onReceive(Context context, Intent intent) {
        Injector.inject(this);
        final String path = context.getString(R.string.package_replaced_string);
        final String intentData = intent.getDataString();
        if (path.equals(intentData) || ("package:" + path).equals(intentData)) {
            stocksProvider.fetch();
            preferences.edit().putBoolean(Tools.FIRST_TIME_VIEWING_SWIPELAYOUT, true).apply();
            preferences.edit().putBoolean(Tools.WHATS_NEW, true).apply();
        } else if (random.nextInt() % 2 == 0) { // randomly change this string so the user sees the animation
            preferences.edit().putBoolean(Tools.FIRST_TIME_VIEWING_SWIPELAYOUT, true).apply();
        }
    }
}
