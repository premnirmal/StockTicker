package com.github.premnirmal.ticker.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

import com.github.premnirmal.ticker.BaseActivity;
import com.github.premnirmal.ticker.StocksApp;
import com.github.premnirmal.ticker.Tools;
import com.github.premnirmal.ticker.model.IHistoryProvider;
import com.github.premnirmal.tickerwidget.R;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.OnDataPointTapListener;
import com.jjoe64.graphview.series.PointsGraphSeries;
import com.jjoe64.graphview.series.Series;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import javax.inject.Inject;

import rx.functions.Action1;

/**
 * Created by premnirmal on 12/30/14.
 */
public class GraphActivity extends BaseActivity {

    public static final String GRAPH_DATA = "GRAPH_DATA";

    private final DateTimeFormatter formatter = DateTimeFormat.forPattern("MM/dd/YYYY");

    private String ticker;
    private DataPoint[] dataPoints;

    @Inject
    IHistoryProvider historyProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((StocksApp) getApplicationContext()).inject(this);
        setContentView(R.layout.progress);
        ticker = getIntent().getStringExtra(GRAPH_DATA);
        getSupportActionBar().hide();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (dataPoints == null) {
            if (Tools.isNetworkOnline(this)) {
                historyProvider.getDataPoints(ticker)
                        .doOnError(new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                showDialog(throwable.getMessage());
                            }
                        })
                        .subscribe(new Action1<DataPoint[]>() {
                            @Override
                            public void call(DataPoint[] data) {
                                dataPoints = data;
                                loadGraph();
                            }
                        });
            } else {
                showDialog(getString(R.string.no_network_message));
            }
        } else {
            loadGraph();
        }
    }

    private void loadGraph() {
        setContentView(R.layout.activity_graph);
        final GraphView graphView = (GraphView) findViewById(R.id.graph);
        final TextView tickerName = (TextView) findViewById(R.id.ticker);
        tickerName.setText(ticker);
        final TextView dataPointValue = (TextView) findViewById(R.id.dataPointValue);
        final LineGraphSeries<DataPoint> series = new LineGraphSeries(dataPoints);
        graphView.addSeries(series);

        final PointsGraphSeries disposableSeries = new PointsGraphSeries(new DataPointInterface[]{dataPoints[dataPoints.length - 1]});
        graphView.addSeries(disposableSeries);
        disposableSeries.setColor(Color.GREEN);
        disposableSeries.setShape(PointsGraphSeries.Shape.POINT);
        disposableSeries.setSize(10f);

        series.setDrawBackground(true);
        series.setBackgroundColor(getResources().getColor(R.color.maroon));
        series.setOnDataPointTapListener(new OnDataPointTapListener() {
            @Override
            public void onTap(Series series, DataPointInterface dataPointInterface) {
                final StringBuilder dataPointText = new StringBuilder();
                final DateTime dateTime = new DateTime((long) dataPointInterface.getX());
                dataPointText.append(formatter.print(dateTime));
                dataPointText.append(" // ");
                dataPointText.append("$");
                dataPointText.append(dataPointInterface.getY());
                dataPointValue.setText(dataPointText.toString());
                disposableSeries.resetData(new DataPointInterface[]{dataPointInterface});
            }
        });
        final GridLabelRenderer gridLabelRenderer = graphView.getGridLabelRenderer();
        gridLabelRenderer.setLabelFormatter(new DateAsXAxisLabelFormatter(this));
        gridLabelRenderer.setNumHorizontalLabels(5);
        final Viewport viewport = graphView.getViewport();
        viewport.setXAxisBoundsManual(true);
        viewport.setMinX(dataPoints[0].getX());
        viewport.setMaxX(dataPoints[dataPoints.length - 1].getX());
    }
}
