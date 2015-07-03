package com.github.premnirmal.ticker.network;

import java.io.Serializable;

/**
 * Created by premnirmal on 12/21/14.
 */
public class Stock implements Comparable<Stock>, Serializable {

    private static final long serialVersionUID = -425355L;

    public String symbol;

    public String Name;

    public float LastTradePriceOnly;
    public String LastTradeDate;
    public String ChangeinPercent;
    public String Change;
    public String AverageDailyVolume;
    public String DaysLow;
    public String DaysHigh;
    public String YearLow;
    public String YearHigh;
    public String MarketCapitalization;
    public String DaysRange;
    public String Volume;
    public String StockExchange;

    // Add Position fields
    public boolean IsPosition;
    public float PositionPrice;
    public float PositionShares;

//    "symbol": "YHOO",
//            "AverageDailyVolume": "18131200",
//            "Change": "+1.37",
//            "DaysLow": "41.97",
//            "DaysHigh": "44.38",
//            "YearLow": "32.15",
//            "YearHigh": "52.62",
//            "MarketCapitalization": "41.18B",
//            "LastTradePriceOnly": "43.99",
//            "DaysRange": "41.97 - 44.38",
//            "Name": "Yahoo! Inc.",
//            "Symbol": "YHOO",
//            "Volume": "30098498",
//            "StockExchange": "NMS"

    @Override
    public int compareTo(Stock another) {
        return Double.compare(getChangeFromPercentString(another.ChangeinPercent),
                getChangeFromPercentString(ChangeinPercent));
    }

    static double getChangeFromPercentString(String percentString) {
        if (percentString == null) {
            return -1000000.0d;
        }
        return Double.valueOf(percentString.replace('%', '\0').trim());
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Stock) {
            if (symbol != null) {
                return symbol.equalsIgnoreCase(((Stock) o).symbol);
            }
        } else if (o instanceof String) {
            return ((String) o).equalsIgnoreCase(symbol);
        }
        return false;
    }

    @Override
    public String toString() {
        return symbol;
    }
}
