package com.github.premnirmal.ticker.portfolio;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.daimajia.swipe.SwipeLayout;
import com.github.premnirmal.ticker.InAppMessage;
import com.github.premnirmal.ticker.Injector;
import com.github.premnirmal.ticker.RxBus;
import com.github.premnirmal.ticker.Tools;
import com.github.premnirmal.ticker.events.NoNetworkEvent;
import com.github.premnirmal.ticker.events.StockUpdatedEvent;
import com.github.premnirmal.ticker.model.IStocksProvider;
import com.github.premnirmal.ticker.network.Stock;
import com.github.premnirmal.ticker.settings.SettingsActivity;
import com.github.premnirmal.ticker.ui.SpacingDecoration;
import com.github.premnirmal.tickerwidget.R;

import javax.inject.Inject;

import rx.functions.Action1;

/**
 * Created by premnirmal on 3/4/15.
 */
public class PortfolioFragment extends BaseFragment {

    private static final String LIST_INSTANCE_STATE = "LIST_INSTANCE_STATE";

    @Inject
    IStocksProvider stocksProvider;

    @Inject
    RxBus bus;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Parcelable listViewState;
    private StocksAdapter stocksAdapter;
    private View rootView;

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
        final View view = inflater.inflate(R.layout.portfolio_fragment, null);
        rootView = view.findViewById(R.id.fragment_root);
        final RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.stockList);
        recyclerView.addItemDecoration(new SpacingDecoration(context.getResources().getDimensionPixelSize(R.dimen.list_spacing)));
        recyclerView.setLayoutManager(new GridLayoutManager(context, 2));
        final SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_container);
        swipeRefreshLayout.setColorSchemeResources(R.color.color_secondary, R.color.spicy_salmon, R.color.sea);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (!Tools.isNetworkOnline(getActivity().getApplicationContext())) {
                    noNetwork(new NoNetworkEvent());
                    swipeRefreshLayout.setRefreshing(false);
                } else {
                    stocksProvider.fetch();
                }
            }
        });

        bind(bus.toObserverable()).subscribe(new Action1<Object>() {
            @Override
            public void call(Object event) {
                if (event instanceof NoNetworkEvent) {
                    noNetwork((NoNetworkEvent) event);
                    swipeRefreshLayout.setRefreshing(false);
                } else if (event instanceof StockUpdatedEvent) {
                    update();
                    swipeRefreshLayout.setRefreshing(false);
                }
            }
        });
        if (!Tools.isNetworkOnline(context.getApplicationContext())) {
            noNetwork(new NoNetworkEvent());
        }
        if (savedInstanceState != null) {
            listViewState = savedInstanceState.getParcelable(LIST_INSTANCE_STATE);
        }

        final View addButton = view.findViewById(R.id.add_ticker_button);
        final CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) addButton.getLayoutParams();
        params.setBehavior(new FABBehaviour());
        addButton.setLayoutParams(params);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent intent = new Intent(v.getContext(), TickerSelectorActivity.class);
                startActivity(intent);
            }
        });

        return view;
    }

    private void update() {
        final FragmentActivity activity = getActivity();
        if (activity != null) {
            if (stocksProvider.getStocks() == null) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        update();
                    }
                }, 600);
            }

            final TextView lastUpdatedTextView = findViewById(R.id.last_updated);
            if (lastUpdatedTextView != null) {
                lastUpdatedTextView.setText("Last updated: " + stocksProvider.lastFetched());
            }

            final RecyclerView recyclerView = findViewById(R.id.stockList);
            if (recyclerView != null) {
                if (stocksAdapter == null) {
                    stocksAdapter = new StocksAdapter(stocksProvider,
                            new StocksAdapter.OnStockClickListener() {
                                @Override
                                public void onRemoveClick(View view, Stock stock, int position) {
                                    promptRemove(stock, position);
                                }

                                @Override
                                public void onClick(final Stock stock) {
                                    new AlertDialog.Builder(getActivity())
                                            .setItems(R.array.graph_or_positions, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    final Intent intent;
                                                    if (which == 0) {
                                                        intent = new Intent(activity, GraphActivity.class);
                                                        intent.putExtra(GraphActivity.GRAPH_DATA, stock);
                                                    } else {
                                                        intent = new Intent(activity, EditPositionActivity.class);
                                                        intent.putExtra(EditPositionActivity.TICKER, stock.symbol);
                                                    }
                                                    getActivity().startActivity(intent);
                                                }
                                            }).show();
                                }
                            });
                } else {
                    stocksAdapter.refresh(stocksProvider);
                }

                recyclerView.setAdapter(stocksAdapter);

                if (listViewState != null) {
                    recyclerView.getLayoutManager().onRestoreInstanceState(listViewState);
                }

                if (stocksAdapter.getItemCount() > 1) {
                    if (Tools.firstTimeViewingSwipeLayout()) {
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                final SwipeLayout layout = (SwipeLayout) recyclerView.getChildAt(0);
                                if (layout != null) {
                                    layout.open(true);
                                    handler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (layout != null) layout.close();
                                            final SwipeLayout secondLayout = (SwipeLayout) recyclerView.getChildAt(1);
                                            if (secondLayout != null) {
                                                secondLayout.open(true);
                                                handler.postDelayed(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        if (secondLayout != null)
                                                            secondLayout.close();
                                                    }
                                                }, 600);
                                            }
                                        }
                                    }, 600);
                                }
                            }
                        }, 1000);
                    }
                }
            }
        }
    }

    private void promptRemove(final Stock stock, int position) {
        stocksProvider.removeStock(stock.symbol);
        if (stocksAdapter.remove(stock)) {
            stocksAdapter.notifyItemRemoved(position);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        inflater.inflate(R.menu.menu_paranormal, menu);
        final MenuItem rearrangeItem = menu.findItem(R.id.action_rearrange);
        rearrangeItem.setEnabled(!Tools.autoSortEnabled());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final FragmentActivity activity = getActivity();
        final int itemId = item.getItemId();
        if (itemId == R.id.action_settings) {
            final Intent intent = new Intent(activity, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (itemId == R.id.action_rearrange) {
            startActivity(new Intent(getActivity(), RearrangeActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void noNetwork(NoNetworkEvent event) {
        InAppMessage.showMessage(rootView, getString(R.string.no_network_message));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        listViewState = ((RecyclerView) findViewById(R.id.stockList)).getLayoutManager().onSaveInstanceState();
        outState.putParcelable(LIST_INSTANCE_STATE, listViewState);
    }
}
