package com.github.premnirmal.ticker.repo

import com.github.premnirmal.ticker.network.data.Holding
import com.github.premnirmal.ticker.network.data.Properties
import com.github.premnirmal.ticker.network.data.Quote

/**
 * Platform-neutral contract for persisting the user's watchlist tickers, quotes, holdings and
 * per-quote properties. All of its inputs/outputs are already-shared `commonMain` models
 * ([Quote], [Holding], [Properties]), so presentation/state code can depend on this abstraction
 * rather than a platform persistence implementation.
 *
 * This is the shared "storage interface" of the multiplatform persistence story. The concrete
 * implementation ([StocksStorage]) now lives in `commonMain` too, backed by the shared **Room KMP**
 * engine ([QuoteDao]/[QuotesDB]); only the platform database builder (and the bundled-SQLite driver)
 * is provided per target via `expect`/`actual`, so both Android and iOS share the same storage code.
 * It mirrors the existing
 * [com.github.premnirmal.ticker.model.RefreshScheduler] and
 * [com.github.premnirmal.ticker.network.CrumbProvider] splits (shared contract in `commonMain`,
 * platform glue per target).
 *
 * Implemented by the shared [StocksStorage]. Operations that take/return platform-only types
 * (e.g. the `java.time`-formatted fetch-log rows) intentionally stay off this contract and are
 * handled as platform extensions on the concrete implementation.
 */
interface QuoteStorage {

  /** Persists the set of watchlist ticker symbols. */
  fun saveTickers(tickers: Set<String>)

  /** Reads the persisted set of watchlist ticker symbols (empty if none). */
  fun readTickers(): Set<String>

  /** Reads all persisted quotes, each with its holdings and properties populated. */
  suspend fun readQuotes(): List<Quote>

  /** Reads the persisted quote for [symbol], with its holdings and properties, or `null`. */
  suspend fun readQuote(symbol: String): Quote?

  /** Persists a single [quote] along with its holdings. */
  suspend fun saveQuote(quote: Quote)

  /** Persists a batch of [quotes] along with their holdings and properties. */
  suspend fun saveQuotes(quotes: List<Quote>)

  /** Removes the quote (and its holdings) for [symbol]. */
  suspend fun removeQuoteBySymbol(symbol: String)

  /** Removes the quotes (and their holdings) for [tickers]. */
  suspend fun removeQuotesBySymbol(tickers: List<String>)

  /** Adds a [holding], returning the id assigned to the newly persisted row. */
  suspend fun addHolding(holding: Holding): Long

  /** Removes a [holding] for [ticker]. */
  suspend fun removeHolding(ticker: String, holding: Holding)

  /** Persists per-quote [properties] (notes, display name, alerts). */
  suspend fun saveQuoteProperties(properties: Properties)
}
