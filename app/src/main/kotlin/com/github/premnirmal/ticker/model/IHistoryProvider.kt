package com.github.premnirmal.ticker.model

import com.github.premnirmal.ticker.network.data.DataPoint
import org.threeten.bp.LocalDate
import java.io.Serializable

interface IHistoryProvider {

  suspend fun getHistoricalDataShort(symbol: String): FetchResult<List<DataPoint>>

  suspend fun getHistoricalDataByRange(
    symbol: String,
    range: Range
  ): FetchResult<List<DataPoint>>

  sealed class Range(val end: LocalDate) : Serializable {
    class DateRange(end: LocalDate) : Range(end)
    companion object {
      val ONE_MONTH = DateRange(LocalDate.now().minusMonths(1))
      val THREE_MONTH = DateRange(LocalDate.now().minusMonths(3))
      val ONE_YEAR = DateRange(LocalDate.now().minusYears(1))
      val MAX = DateRange(LocalDate.now().minusYears(20))
    }
  }
}