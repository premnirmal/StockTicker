package com.github.premnirmal.ticker.network;

import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;

/**
 * Created by premnirmal on 12/30/14.
 */
public final class QueryCreator {

  private QueryCreator() {
  }

  public final static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  public static String googleStocksQuery(Object[] tickers) {
    final StringBuilder commaSeparator = new StringBuilder();
    for (Object o : tickers) {
      final String ticker = o.toString();
      if (ticker.length() < 10) { // not sure why I have this
        commaSeparator.append(ticker);
        commaSeparator.append(',');
      }
    }
    final int length = commaSeparator.length();
    if (length > 0) {
      commaSeparator.deleteCharAt(length - 1);
    }
    return commaSeparator.toString();
  }

  public static String buildStocksQuery(Object[] objects) {
    final StringBuilder commaSeparator = new StringBuilder();
    for (Object object : objects) {
      final String ticker = object.toString().replaceAll(" ", "").trim();
      if (ticker.length() < 10) { // not sure why I have this
        commaSeparator.append(ticker);
        commaSeparator.append(',');
      }
    }
    final int length = commaSeparator.length();
    if (length > 0) {
      commaSeparator.deleteCharAt(length - 1);
    }

    return "select%20%2A%20from%20yahoo.finance.quotes%20where%20symbol%20in%20(\"" + commaSeparator
        .toString() + "\")";
  }

  public static String buildHistoricalDataQuery(String ticker, ZonedDateTime start, ZonedDateTime end) {
    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(
        "select%20%2A%20from%20yahoo.finance.historicaldata%20where%20symbol%20=%20\"");
    stringBuilder.append(ticker);
    stringBuilder.append("\"%20and%20startDate%20=%20\"");
    stringBuilder.append(formatter.format(start));
    stringBuilder.append("\"%20and%20endDate%20=%20\"");
    stringBuilder.append(formatter.format(end));
    stringBuilder.append('\"');
    return stringBuilder.toString();
  }
}
