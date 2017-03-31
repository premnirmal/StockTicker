package com.github.premnirmal.ticker.network.data

import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter

/**
 * Created by premnirmal on 3/30/17.
 */
object QueryCreator {

  val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")!!

  fun googleStocksQuery(tickers: Array<Any>): String {
    val commaSeparator = StringBuilder()
    for (o in tickers) {
      val ticker = o.toString()
      if (ticker.length < 10) { // not sure why I have this
        commaSeparator.append(ticker)
        commaSeparator.append(',')
      }
    }
    val length = commaSeparator.length
    if (length > 0) {
      commaSeparator.deleteCharAt(length - 1)
    }
    return commaSeparator.toString()
  }

  fun buildStocksQuery(objects: Array<Any>): String {
    val commaSeparator = StringBuilder()
    for (`object` in objects) {
      val ticker = `object`.toString().replace(" ".toRegex(), "").trim { it <= ' ' }
      if (ticker.length < 10) { // not sure why I have this
        commaSeparator.append(ticker)
        commaSeparator.append(',')
      }
    }
    val length = commaSeparator.length
    if (length > 0) {
      commaSeparator.deleteCharAt(length - 1)
    }

    return "select%20%2A%20from%20yahoo.finance.quotes%20where%20symbol%20in%20(\"" + commaSeparator
        .toString() + "\")"
  }

  fun buildHistoricalDataQuery(ticker: String, start: ZonedDateTime, end: ZonedDateTime): String {
    val stringBuilder = StringBuilder()
    stringBuilder.append(
        "select%20%2A%20from%20yahoo.finance.historicaldata%20where%20symbol%20=%20\"")
    stringBuilder.append(ticker)
    stringBuilder.append("\"%20and%20startDate%20=%20\"")
    stringBuilder.append(formatter.format(start))
    stringBuilder.append("\"%20and%20endDate%20=%20\"")
    stringBuilder.append(formatter.format(end))
    stringBuilder.append('\"')
    return stringBuilder.toString()
  }
}
