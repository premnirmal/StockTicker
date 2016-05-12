package com.github.premnirmal.ticker.network;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

/**
 * Created by premnirmal on 12/21/14.
 */
public class Stock implements Comparable<Stock>, Serializable {

  public static final String GDAXI_TICKER = "^GDAXI";
  public static final String XAU_TICKER = "XAU";

  private static final long serialVersionUID = -425355L;

  @SerializedName("Symbol") public String symbol = "";
  @SerializedName("Name") public String Name = "";
  @SerializedName("LastTradePriceOnly") public float LastTradePriceOnly;
  @SerializedName("ChangeinPercent") public String ChangeinPercent = "";
  @SerializedName("Change") public String Change = "";
  @SerializedName("AverageDailyVolume") public String AverageDailyVolume = "";
  @SerializedName("YearLow") public float YearLow;
  @SerializedName("YearHigh") public float YearHigh;
  @SerializedName("StockExchange") public String StockExchange = "";

  // Add Position fields
  public boolean IsPosition;
  public float PositionPrice;
  public int PositionShares;

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

  @Override public int compareTo(Stock another) {
    return Double.compare(getChangeFromPercentString(another.ChangeinPercent),
        getChangeFromPercentString(ChangeinPercent));
  }

  static double getChangeFromPercentString(String percentString) {
    if (percentString == null) {
      return -1000000.0d;
    }
    return Double.valueOf(percentString.replace('%', '\0').trim());
  }

  @Override public boolean equals(Object o) {
    if (o instanceof Stock) {
      if (symbol != null) {
        return symbol.equalsIgnoreCase(((Stock) o).symbol);
      }
    } else if (o instanceof String) {
      return ((String) o).equalsIgnoreCase(symbol);
    }
    return false;
  }

  @Override public int hashCode() {
    return symbol != null ? symbol.hashCode() : super.hashCode();
  }

  @Override public String toString() {
    return symbol;
  }

  public boolean isIndex() {
    return symbol != null && symbol.startsWith("^") && !symbol.equals(GDAXI_TICKER);
  }
}
