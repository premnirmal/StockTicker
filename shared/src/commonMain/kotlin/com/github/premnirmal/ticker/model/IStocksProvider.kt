package com.github.premnirmal.ticker.model

import com.github.premnirmal.ticker.network.data.Holding
import com.github.premnirmal.ticker.network.data.Position
import com.github.premnirmal.ticker.network.data.Quote
import kotlinx.coroutines.flow.StateFlow

/**
 * Platform-neutral contract for the central stock/portfolio data provider: the observable watchlist
 * and portfolio state, plus the operations to add/remove tickers and holdings, fetch quotes and
 * schedule the periodic refresh.
 *
 * This is the shared "provider interface" of the multiplatform persistence/refresh story: it mirrors
 * the existing [RefreshScheduler] / [com.github.premnirmal.ticker.repo.QuoteStorage] /
 * [com.github.premnirmal.ticker.UserPreferences] splits — the common contract lives in `commonMain`
 * (in terms of the already-shared `Quote`/`Position`/`Holding`/`FetchResult` models), while the
 * Android implementation wires it to platform infrastructure (`Context`/`SharedPreferences`,
 * `AlarmScheduler`, `WidgetDataProvider`, the Room-backed `StocksStorage`). On Android this is
 * implemented by `StocksProvider`; the iOS app provides its own implementation.
 *
 * The state of the most recent refresh is exposed through [fetchState]; its [FetchState] display
 * string is formatted behind the multiplatform [formatFetchTime] boundary, so it is part of this
 * shared contract rather than the concrete implementations.
 */
interface IStocksProvider {

  companion object {
    /**
     * Default polling interval (in millis) for the real-time refresh loops while a screen is in the
     * foreground and the market is open. Shared so the common ViewModels can drive the same cadence
     * on Android and iOS.
     */
    const val DEFAULT_INTERVAL_MS: Long = 15_000L
  }

  /** The current watchlist symbols, as an observable flow. */
  val tickers: StateFlow<List<String>>

  /** The state of the most recent refresh (not fetched / success / failure), as an observable flow. */
  val fetchState: StateFlow<FetchState>

  /** The current portfolio quotes, as an observable flow. */
  val portfolio: StateFlow<List<Quote>>

  /** Epoch-millis timestamp of the next scheduled refresh, as an observable flow. */
  val nextFetchMs: StateFlow<Long>

  /** Schedules the next periodic refresh. [reason] is used for diagnostics/logging only. */
  fun scheduleUpdate(reason: String = "regular")

  /** Whether [ticker] is in the watchlist. */
  fun hasTicker(ticker: String): Boolean

  /**
   * Fetches fresh quotes for the whole watchlist. When [allowScheduling] is `true` the next refresh
   * is (re)scheduled as part of the fetch.
   */
  suspend fun fetch(allowScheduling: Boolean = true): FetchResult<List<Quote>>

  /** Schedules the periodic refresh if one is not already pending. */
  fun schedule()

  /** Adds [ticker] to the watchlist, returning the updated set of symbols. */
  fun addStock(ticker: String): Collection<String>

  /** Whether any watchlist quote has holdings. */
  fun hasPositions(): Boolean

  /** Whether [ticker] has holdings. */
  fun hasPosition(ticker: String): Boolean

  /** The [Position] for [ticker], or `null` if there is none. */
  fun getPosition(ticker: String): Position?

  /** Adds a holding ([shares] at [price]) to [ticker], returning the created [Holding]. */
  suspend fun addHolding(ticker: String, shares: Float, price: Float): Holding

  /** Removes [holding] from [ticker], returning whether it was removed. */
  suspend fun removePosition(ticker: String, holding: Holding): Boolean

  /** Adds [symbols] to the watchlist, returning the updated set of symbols. */
  fun addStocks(symbols: Collection<String>): Collection<String>

  /** Removes [ticker] from the watchlist, returning the updated set of symbols. */
  suspend fun removeStock(ticker: String): Collection<String>

  /** Removes [symbols] from the watchlist. */
  suspend fun removeStocks(symbols: Collection<String>)

  /** Removes stale persisted data. */
  suspend fun cleanup()

  /**
   * Fetches a single quote for [ticker]. When [allowCache] is `true` a cached quote may be returned
   * instead of hitting the network.
   */
  suspend fun fetchStock(ticker: String, allowCache: Boolean = true): FetchResult<Quote>

  /** The cached [Quote] for [ticker], or `null` if there is none. */
  fun getStock(ticker: String): Quote?

  /** Seeds the provider with a previously loaded [portfolio]. */
  fun addPortfolio(portfolio: List<Quote>)
}
