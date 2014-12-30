package com.github.premnirmal.ticker.ui;

import android.os.Bundle;

import com.github.premnirmal.ticker.BaseActivity;
import com.github.premnirmal.ticker.network.historicaldata.HistoricalData;
import com.github.premnirmal.ticker.network.historicaldata.Quote;
import com.github.premnirmal.tickerwidget.R;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;

/**
 * Created by premnirmal on 12/30/14.
 */
public class GraphActivity extends BaseActivity {

    public static final String GRAPH_DATA = "GRAPH_DATA";

    private HistoricalData historicalData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);
        historicalData = getIntent().getParcelableExtra(GRAPH_DATA);
        final GraphView graphView = (GraphView) findViewById(R.id.graph);

        final Quote[] quotes = new Quote[historicalData.query.results.quote.size()];
        historicalData.query.results.quote.toArray(quotes);

        final GraphViewSeries series = new GraphViewSeries(quotes);
        graphView.addSeries(series);
    }
}
