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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.AdapterView;
import android.widget.GridView;
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
import com.github.premnirmal.ticker.ui.ScrollDetector;
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
    private final ScrollDetector scrollListener = new ScrollDetector() {
        @Override
        public void onScrollUp() {
            animateButton(false);
        }

        @Override
        public void onScrollDown() {
            animateButton(true);
        }
    };
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

            final TextView lastUpdatedTextView = (TextView) findViewById(R.id.last_updated);
            if (lastUpdatedTextView != null) {
                lastUpdatedTextView.setText("Last updated: " + stocksProvider.lastFetched());
            }

            final GridView adapterView = (GridView) findViewById(R.id.stockList);
            if (adapterView != null) {
                scrollListener.setListView(adapterView);
                adapterView.setOnScrollListener(scrollListener);

                if (stocksAdapter == null) {
                    stocksAdapter = new StocksAdapter(stocksProvider,
                            new StocksAdapter.OnRemoveClickListener() {
                                @Override
                                public void onRemoveClick(View view, Stock stock, int position) {
                                    promptRemove(stock);
                                }
                            });
                } else {
                    stocksAdapter.refresh(stocksProvider);
                }

                adapterView.setAdapter(stocksAdapter);

                if (listViewState != null) {
                    adapterView.onRestoreInstanceState(listViewState);
                }

                adapterView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                        new AlertDialog.Builder(getActivity())
                                .setItems(R.array.graph_or_positions, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        final Intent intent;
                                        if (which == 0) {
                                            intent = new Intent(activity, GraphActivity.class);
                                            intent.putExtra(GraphActivity.GRAPH_DATA, stocksAdapter.getItem(position));
                                        } else {
                                            intent = new Intent(activity, EditPositionActivity.class);
                                            intent.putExtra(EditPositionActivity.TICKER, stocksAdapter.getItem(position).symbol);
                                        }
                                        getActivity().startActivity(intent);
                                    }
                                }).show();
                    }
                });

                if (stocksAdapter.getCount() > 1) {
                    if (Tools.firstTimeViewingSwipeLayout()) {
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                final SwipeLayout layout = (SwipeLayout) adapterView.getChildAt(0);
                                if (layout != null) {
                                    layout.open(true);
                                    handler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (layout != null) layout.close();
                                            final SwipeLayout secondLayout = (SwipeLayout) adapterView.getChildAt(1);
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

    private final Interpolator interp = new OvershootInterpolator();

    private void animateButton(boolean show) {
        final View button = findViewById(R.id.add_ticker_button);
        final CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) button.getLayoutParams();
        final int translationY = show ? 0 : (button.getHeight() + layoutParams.topMargin);
        button.animate().setInterpolator(interp)
                .setDuration(400)
                .translationY(translationY);
    }

    private void promptRemove(final Stock stock) {
        stocksProvider.removeStock(stock.symbol);
        if (stocksAdapter.remove(stock)) {
            stocksAdapter.notifyDataSetChanged();
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
        listViewState = ((GridView) findViewById(R.id.stockList)).onSaveInstanceState();
        outState.putParcelable(LIST_INSTANCE_STATE, listViewState);
    }
}
