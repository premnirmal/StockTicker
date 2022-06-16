package com.github.premnirmal.ticker.model

import com.github.premnirmal.ticker.createTimeString
import com.github.premnirmal.ticker.network.data.Holding
import com.github.premnirmal.ticker.network.data.Position
import com.github.premnirmal.ticker.network.data.Quote
import kotlinx.coroutines.flow.StateFlow
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

/**
 * Created by premnirmal on 2/28/16.
 */
interface IStocksProvider {

  val fetchState: StateFlow<FetchState>

  val nextFetchMs: StateFlow<Long>

  suspend fun fetch(allowScheduling: Boolean = true): FetchResult<List<Quote>>

  fun schedule()

  val tickers: StateFlow<List<String>>

  val portfolio: StateFlow<List<Quote>>

  fun addPortfolio(portfolio: List<Quote>)

  fun getStock(ticker: String): Quote?

  suspend fun fetchStock(ticker: String, allowCache: Boolean = true): FetchResult<Quote>

  suspend fun removeStock(ticker: String): Collection<String>

  suspend fun removeStocks(symbols: Collection<String>)

  fun hasTicker(ticker: String): Boolean

  fun addStock(ticker: String): Collection<String>

  fun addStocks(symbols: Collection<String>): Collection<String>

  fun hasPositions(): Boolean

  fun hasPosition(ticker: String): Boolean

  fun getPosition(ticker: String): Position?

  suspend fun addHolding(
    ticker: String,
    shares: Float,
    price: Float
  ): Holding

  suspend fun removePosition(
    ticker: String,
    holding: Holding
  )

  sealed class FetchState {
    abstract val displayString: String

    object NotFetched : FetchState() {
      override val displayString: String = "--"
    }

    class Success(val fetchTime: Long) : FetchState() {
      override val displayString: String by lazy {
        val instant = Instant.ofEpochMilli(fetchTime)
        val time = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault())
        time.createTimeString()
      }
    }

    class Failure(val exception: Throwable) : FetchState() {
      override val displayString: String by lazy {
        exception.message.orEmpty()
      }
    }
  }

  companion object {
    const val DEFAULT_INTERVAL_MS: Long = 15_000L
  }
}