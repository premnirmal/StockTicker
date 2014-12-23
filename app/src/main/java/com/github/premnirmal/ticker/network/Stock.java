package com.github.premnirmal.ticker.network;

/**
 * Created by premnirmal on 12/21/14.
 */
public class Stock implements Comparable<Stock> {

    public String symbol;

    public String Name;

    public float LastTradePriceOnly;
    public String LastTradeDate;
    public String ChangeinPercent;
    public String Change;

    @Override
    public int compareTo(Stock another) {
        return Double.compare(getChangeFromPercentString(another.ChangeinPercent),
                getChangeFromPercentString(ChangeinPercent));
    }

    static double getChangeFromPercentString(String percentString) {
        if(percentString == null) {
            return -1000000.0d;
        }
        return Double.valueOf(percentString.replace('%','\0').trim());
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof Stock) {
            if(symbol != null) {
                return symbol.replace("^","").equalsIgnoreCase(((Stock)o).symbol);
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return symbol;
    }
}
