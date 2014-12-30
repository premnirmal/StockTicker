package com.github.premnirmal.ticker.ui;

import android.os.Bundle;

import com.github.premnirmal.ticker.BaseActivity;
import com.github.premnirmal.ticker.StocksApp;
import com.github.premnirmal.ticker.model.IHistoryProvider;
import com.github.premnirmal.ticker.network.historicaldata.History;
import com.github.premnirmal.ticker.network.historicaldata.Quote;
import com.github.premnirmal.tickerwidget.R;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;

import javax.inject.Inject;

import rx.functions.Action1;

/**
 * Created by premnirmal on 12/30/14.
 */
public class GraphActivity extends BaseActivity {

    public static final String GRAPH_DATA = "GRAPH_DATA";

    private String ticker;
    private History history;

    @Inject
    IHistoryProvider historyProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((StocksApp) getApplicationContext()).inject(this);
        setContentView(R.layout.progress);
        ticker = getIntent().getStringExtra(GRAPH_DATA);
        getSupportActionBar().setTitle(ticker);
        historyProvider.getHistory(ticker)
                .subscribe(new Action1<History>() {
                    @Override
                    public void call(History hist) {
                        history = hist;
                        final LineGraphView graphView = new LineGraphView(GraphActivity.this);
                        final Quote[] quotes = new Quote[history.quote.size()];
                        history.quote.toArray(quotes);
                        final GraphViewSeries series = new GraphViewSeries(quotes);
                        graphView.addSeries(series);
                        setContentView(graphView);
                    }
                });
    }
}
