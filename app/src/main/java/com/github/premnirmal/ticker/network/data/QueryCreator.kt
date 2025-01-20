package com.github.premnirmal.ticker.network.data

import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter

/**
 * Created by premnirmal on 3/30/17.
 */
object QueryCreator {

  val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")!!

  fun buildHistoricalDataQuery(
    ticker: String,
    start: LocalDateTime,
    end: LocalDateTime
  ): String {
    val stringBuilder = StringBuilder()
    stringBuilder.append(
        "select%20%2A%20from%20yahoo.finance.historicaldata%20where%20symbol%20=%20\""
    )
    stringBuilder.append(ticker)
    stringBuilder.append("\"%20and%20startDate%20=%20\"")
    stringBuilder.append(formatter.format(start))
    stringBuilder.append("\"%20and%20endDate%20=%20\"")
    stringBuilder.append(formatter.format(end))
    stringBuilder.append('\"')
    return stringBuilder.toString()
  }
}
