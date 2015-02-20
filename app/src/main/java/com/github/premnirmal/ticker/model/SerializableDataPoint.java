package com.github.premnirmal.ticker.model;

import com.jjoe64.graphview.series.DataPoint;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by premnirmal on 2/20/15.
 */
public class SerializableDataPoint extends DataPoint implements Serializable {

    public SerializableDataPoint(double x, double y) {
        super(x, y);
    }

    public SerializableDataPoint(Date x, double y) {
        super(x, y);
    }
}
