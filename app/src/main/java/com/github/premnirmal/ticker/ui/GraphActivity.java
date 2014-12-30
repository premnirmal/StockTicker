package com.github.premnirmal.ticker.ui;

import android.os.Bundle;

import com.github.premnirmal.ticker.BaseActivity;
import com.github.premnirmal.ticker.StocksApp;
import com.github.premnirmal.ticker.model.IHistoryProvider;
import com.github.premnirmal.tickerwidget.R;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import javax.inject.Inject;

import rx.functions.Action1;

/**
 * Created by premnirmal on 12/30/14.
 */
public class GraphActivity extends BaseActivity {

    public static final String GRAPH_DATA = "GRAPH_DATA";

    private String ticker;

    @Inject
    IHistoryProvider historyProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((StocksApp) getApplicationContext()).inject(this);
        setContentView(R.layout.progress);
        ticker = getIntent().getStringExtra(GRAPH_DATA);
        getSupportActionBar().setTitle(ticker);
        historyProvider.getDataPoints(ticker)
                .subscribe(new Action1<DataPoint[]>() {
                    @Override
                    public void call(DataPoint[] dataPoints) {
                        final GraphView graphView = new GraphView(GraphActivity.this);
                        final LineGraphSeries<DataPoint> series = new LineGraphSeries(dataPoints);
                        graphView.addSeries(series);
                        graphView.getGridLabelRenderer()
                                .setLabelFormatter(new DateAsXAxisLabelFormatter(GraphActivity.this));
                        setContentView(graphView);
                    }
                });
    }
}
