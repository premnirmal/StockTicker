package com.github.premnirmal.ticker.repo

import android.content.SharedPreferences
import androidx.room.withTransaction
import com.github.premnirmal.ticker.AppPreferences.Companion.HAS_MIGRATED
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.network.data.Holding
import com.github.premnirmal.ticker.network.data.Position
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.repo.data.HoldingRow
import com.github.premnirmal.ticker.repo.data.QuoteRow
import com.google.gson.Gson
import io.paperdb.Paper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.ArrayList
import javax.inject.Inject

/**
 * Created by premnirmal on 2/28/16.
 */
class StocksStorage {

  companion object {
    private const val KEY_STOCKS = "STOCKS"
    private const val KEY_POSITIONS = "POSITIONS_NEW"
    private const val KEY_TICKERS = "TICKERS"
  }

  @Inject lateinit var preferences: SharedPreferences
  @Inject lateinit var gson: Gson
  @Inject lateinit var db: QuotesDB
  @Inject lateinit var quoteDao: QuoteDao

  init {
    Injector.appComponent.inject(this)
  }

  @Deprecated("remove after migration")
  private fun readStocksOld(): List<Quote> {
    return Paper.book().read(KEY_STOCKS, ArrayList<Quote>())
  }

  @Deprecated("remove after migration")
  private fun readTickersOld(): List<String> {
    return Paper.book()
        .read(KEY_TICKERS, ArrayList())
  }

  @Deprecated("remove after migration")
  private fun readPositionsOld(): List<Position> {
    return Paper.book()
        .read(KEY_POSITIONS, ArrayList())
  }

  fun saveTickers(tickers: Set<String>) {
    preferences.edit()
        .putStringSet(KEY_TICKERS, tickers)
        .apply()
  }

  fun readTickers(): Set<String> {
    return preferences.getStringSet(
        KEY_TICKERS, emptySet())!!
  }

  suspend fun migrateIfNecessary(): Boolean {
    if (preferences.getBoolean(HAS_MIGRATED, false)) {
      return false
    }
    return withContext(Dispatchers.IO) {
      val tickersOld = readTickersOld()
      saveTickers(tickersOld.toSet())
      val stocksOld = readStocksOld()
      val positionsOld = readPositionsOld()
      positionsOld.forEach {
        for (stock in stocksOld) {
          if (stock.symbol == it.symbol) {
            stock.position = it
            break
          }
        }
      }
      saveQuotes(stocksOld)
      preferences.edit().putBoolean(HAS_MIGRATED, true).commit()
    }
  }

  suspend fun readQuotes(): List<Quote> {
    val quotesWithHoldings = db.withTransaction { quoteDao.getQuotesWithHoldings() }
    return withContext(Dispatchers.IO) {
      return@withContext quotesWithHoldings.map { quoteWithHoldings ->
        val quote = quoteWithHoldings.quote.toQuote()
        val holdings = quoteWithHoldings.holdings.map { holdingTable ->
          Holding(holdingTable.quoteSymbol, holdingTable.shares, holdingTable.price, holdingTable.id!!)
        }
        quote.position = Position(quote.symbol, holdings.toMutableList())
        quote
      }
    }
  }

  suspend fun saveQuote(quote: Quote) = db.withTransaction {
    quoteDao.upsertQuoteAndHolding(quote.toQuoteRow(), quote.position?.toHoldingRows())
  }

  suspend fun saveQuotes(quotes: List<Quote>) = withContext(Dispatchers.IO) {
    val quoteRows = quotes.map { it.toQuoteRow() }
    val positions = quotes.mapNotNull { it.position }
    db.withTransaction {
      quoteDao.upsertQuotes(quoteRows)
      positions.forEach {
        quoteDao.upsertHoldings(it.symbol, it.toHoldingRows())
      }
    }
  }

  suspend fun removeQuoteBySymbol(symbol: String) = db.withTransaction {
    quoteDao.deleteQuoteAndHoldings(symbol)
  }

  suspend fun removeQuotesBySymbol(tickers: List<String>) = db.withTransaction {
    quoteDao.deleteQuotesAndHoldings(tickers)
  }

  suspend fun addHolding(holding: Holding) = db.withTransaction {
    quoteDao.insertHolding(holding.toHoldingRow())
  }

  suspend fun removeHolding(ticker: String, holding: Holding) = db.withTransaction {
    quoteDao.deleteHolding(HoldingRow(holding.id, ticker, holding.shares, holding.price))
  }

  private fun Quote.toQuoteRow(): QuoteRow {
    return QuoteRow(this.symbol, this.name, this.lastTradePrice, this.changeInPercent,
        this.change, this.stockExchange, this.currency, this.description)
  }

  private fun Position.toHoldingRows(): List<HoldingRow> {
    return this.holdings.map {
      HoldingRow(it.id, this.symbol, it.shares, it.price)
    }
  }

  private fun Holding.toHoldingRow(): HoldingRow {
    return HoldingRow(this.id, this.symbol, this.shares, this.price)
  }

  private fun QuoteRow.toQuote(): Quote {
    val quote = Quote(this.symbol)
    quote.name = this.name
    quote.lastTradePrice = this.lastTradePrice
    quote.changeInPercent = this.changeInPercent
    quote.change = this.change
    quote.stockExchange = this.stockExchange
    quote.currency = this.currency
    quote.description = this.description
    return quote
  }
}
