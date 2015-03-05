package com.github.premnirmal.ticker.portfolio;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.premnirmal.ticker.Injector;
import com.github.premnirmal.ticker.RxBus;
import com.github.premnirmal.ticker.Tools;
import com.github.premnirmal.ticker.events.NoNetworkEvent;
import com.github.premnirmal.ticker.events.StockUpdatedEvent;
import com.github.premnirmal.ticker.model.IStocksProvider;
import com.github.premnirmal.ticker.settings.SettingsActivity;
import com.github.premnirmal.tickerwidget.R;
import com.terlici.dragndroplist.DragNDropListView;

import javax.inject.Inject;

import rx.functions.Action1;

/**
 * Created by premnirmal on 3/4/15.
 */
public class PortfolioFragment extends BaseFragment {

    @Inject
    IStocksProvider stocksProvider;

    @Inject
    RxBus bus;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private AlertDialog alertDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Injector.inject(this);
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        update();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final Context context = inflater.getContext();
        final SharedPreferences preferences = context.getSharedPreferences(Tools.PREFS_NAME, Context.MODE_PRIVATE);
        final View view;
        if (preferences.getBoolean(Tools.SETTING_AUTOSORT, false)) {
            view = inflater.inflate(R.layout.portfolio_fragment, null);
        } else {
            view = inflater.inflate(R.layout.portfolio_fragment_draggable, null);
        }
        bind(bus.toObserverable()).subscribe(new Action1<Object>() {
            @Override
            public void call(Object event) {
                if (event instanceof NoNetworkEvent) {
                    noNetwork((NoNetworkEvent) event);
                } else if (event instanceof StockUpdatedEvent) {
                    update();
                }
            }
        });
        if (!Tools.isNetworkOnline(context.getApplicationContext())) {
            noNetwork(new NoNetworkEvent());
        }
        return view;
    }


    private void update() {
        final FragmentActivity activity = getActivity();
        activity.supportInvalidateOptionsMenu();
        if (stocksProvider.getStocks() == null) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    update();
                }
            }, 600);
        }
        final SharedPreferences preferences = activity.getSharedPreferences(Tools.PREFS_NAME, Context.MODE_PRIVATE);

        ((TextView) findViewById(R.id.last_updated)).setText("Last updated: " + stocksProvider.lastFetched());

        final AdapterView adapterView = (AdapterView) findViewById(R.id.stockList);
        final StocksAdapter adapter = new StocksAdapter(stocksProvider,
                preferences.getBoolean(Tools.SETTING_AUTOSORT, false));
        if (!preferences.getBoolean(Tools.SETTING_AUTOSORT, false)
                && adapterView instanceof DragNDropListView) {
            ((DragNDropListView) adapterView).setDragNDropAdapter(adapter);
        } else {
            adapterView.setAdapter(adapter);
        }
        adapterView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                final Intent intent = new Intent(activity, GraphActivity.class);
                intent.putExtra(GraphActivity.GRAPH_DATA, adapter.getItem(position));
                startActivity(intent);
            }
        });

        adapterView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                new AlertDialog.Builder(activity)
                        .setMessage("Remove stock?")
                        .setPositiveButton("Remove", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                stocksProvider.removeStock(adapter.getItem(position).symbol);
                                update();
                            }
                        })
                        .show();
                update();
                return true;
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        inflater.inflate(R.menu.menu_paranormal, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final FragmentActivity activity = getActivity();
        if (item.getItemId() == R.id.action_add_ticker) {
            final Intent intent = new Intent(activity, TickerSelectorActivity.class);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.action_settings) {
            final Intent intent = new Intent(activity, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.action_update) {
            if (!Tools.isNetworkOnline(activity.getApplicationContext())) {
                noNetwork(new NoNetworkEvent());
            } else {
                stocksProvider.fetch();
                item.setActionView(new ProgressBar(activity));
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void noNetwork(NoNetworkEvent event) {
        final boolean showing = alertDialog != null && !alertDialog.isShowing();
        if (!showing) {
            alertDialog = showDialog(getString(R.string.no_network_message));
        }
    }
}
