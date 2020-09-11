package com.github.premnirmal.ticker.model

import com.github.premnirmal.ticker.network.data.DataPoint
import org.threeten.bp.Duration
import org.threeten.bp.LocalDate
import java.io.Serializable

interface IHistoryProvider {

  suspend fun fetchDataShort(symbol: String): FetchResult<List<DataPoint>>

  suspend fun fetchDataByRange(
    symbol: String,
    range: Range
  ): FetchResult<List<DataPoint>>

  sealed class Range(val duration: Duration) : Serializable {
    val end = LocalDate.now().minusDays(duration.toDays())
    class DateRange(duration: Duration) : Range(duration)
    companion object {
      val ONE_DAY = DateRange(Duration.ofDays(1))
      val TWO_WEEKS = DateRange(Duration.ofDays(14))
      val ONE_MONTH = DateRange(Duration.ofDays(30))
      val THREE_MONTH = DateRange(Duration.ofDays(90))
      val ONE_YEAR = DateRange(Duration.ofDays(365))
      val MAX = DateRange(Duration.ofDays(20 * 365))
    }
  }
}