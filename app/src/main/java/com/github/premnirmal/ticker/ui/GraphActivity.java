package com.github.premnirmal.ticker.ui;

import android.os.Bundle;

import com.github.premnirmal.ticker.BaseActivity;
import com.github.premnirmal.ticker.network.historicaldata.HistoricalData;

/**
 * Created by premnirmal on 12/30/14.
 */
public class GraphActivity extends BaseActivity {

    public static final String GRAPH_DATA = "GRAPH_DATA";

    private HistoricalData historicalData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        historicalData = getIntent().getParcelableExtra(GRAPH_DATA);
    }
}
