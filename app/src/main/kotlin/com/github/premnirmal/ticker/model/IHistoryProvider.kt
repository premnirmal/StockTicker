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

  sealed class Range(val name: String, val duration: Duration) : Serializable {
    val end = LocalDate.now().minusDays(duration.toDays())
    class DateRange(name: String, duration: Duration) : Range(name, duration)
    companion object {
      val ONE_DAY = DateRange("ONE_DAY", Duration.ofDays(1))
      val TWO_WEEKS = DateRange("TWO_WEEKS", Duration.ofDays(14))
      val ONE_MONTH = DateRange("ONE_MONTH", Duration.ofDays(30))
      val THREE_MONTH = DateRange("THREE_MONTH", Duration.ofDays(90))
      val ONE_YEAR = DateRange("ONE_YEAR", Duration.ofDays(365))
      val MAX = DateRange("MAX", Duration.ofDays(20 * 365))

      fun from(name: String): Range {
        return when(name) {
          "ONE_DAY" -> ONE_DAY
          "TWO_WEEKS" -> TWO_WEEKS
          "ONE_MONTH" -> ONE_MONTH
          "THREE_MONTH" -> THREE_MONTH
          "ONE_YEAR" -> ONE_YEAR
          "MAX" -> MAX
          else -> throw IllegalArgumentException("Unknown name $name")
        }
      }
    }
  }
}
