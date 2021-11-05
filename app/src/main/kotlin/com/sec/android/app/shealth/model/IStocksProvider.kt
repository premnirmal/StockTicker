package com.sec.android.app.shealth.model

import com.sec.android.app.shealth.createTimeString
import com.sec.android.app.shealth.network.data.Holding
import com.sec.android.app.shealth.network.data.Position
import com.sec.android.app.shealth.network.data.Quote
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

/**
 * Created by android on 2/28/16.
 */
interface IStocksProvider {

  val fetchState: FetchState

  fun nextFetch(): String

  fun nextFetchMs(): Long

  suspend fun fetch(): FetchResult<List<Quote>>

  fun schedule()

  fun getTickers(): List<String>

  fun getPortfolio(): List<Quote>

  fun addPortfolio(portfolio: List<Quote>)

  fun getStock(ticker: String): Quote?

  suspend fun fetchStock(ticker: String): FetchResult<Quote>

  fun removeStock(ticker: String): Collection<String>

  fun removeStocks(symbols: Collection<String>)

  fun hasTicker(ticker: String): Boolean

  fun addStock(ticker: String): Collection<String>

  fun addStocks(symbols: Collection<String>): Collection<String>

  fun hasPositions(): Boolean

  fun hasPosition(ticker: String): Boolean

  fun getPosition(ticker: String): Position?

  fun addHolding(
    ticker: String,
    shares: Float,
    price: Float
  ): Holding

  fun removePosition(
    ticker: String,
    holding: Holding
  )

  sealed class FetchState {
    abstract val displayString: String

    object NotFetched : FetchState() {
      override val displayString: String = "--"
    }

    data class Success(val fetchTime: Long) : FetchState() {
      override val displayString: String by lazy {
        val instant = Instant.ofEpochMilli(fetchTime)
        val time = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault())
        time.createTimeString()
      }
    }

    data class Failure(val exception: Throwable) : FetchState() {
      override val displayString: String by lazy {
        exception.message.orEmpty()
      }
    }
  }
}