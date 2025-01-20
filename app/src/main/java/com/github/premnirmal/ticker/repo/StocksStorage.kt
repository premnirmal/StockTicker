package com.github.premnirmal.ticker.repo

import android.content.SharedPreferences
import androidx.room.withTransaction
import com.github.premnirmal.ticker.network.data.Holding
import com.github.premnirmal.ticker.network.data.Position
import com.github.premnirmal.ticker.network.data.Properties
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.repo.data.HoldingRow
import com.github.premnirmal.ticker.repo.data.PropertiesRow
import com.github.premnirmal.ticker.repo.data.QuoteRow
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by premnirmal on 2/28/16.
 */
@Singleton
class StocksStorage @Inject constructor(
  private val preferences: SharedPreferences,
  private val gson: Gson,
  private val db: QuotesDB,
  private val quoteDao: QuoteDao
) {

  companion object {
    private const val KEY_TICKERS = "TICKERS"
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

  suspend fun readQuotes(): List<Quote> {
    val quotesWithHoldings = db.withTransaction { quoteDao.getQuotesWithHoldings() }
    return withContext(Dispatchers.IO) {
      return@withContext quotesWithHoldings.map { quoteWithHoldings ->
        val quote = quoteWithHoldings.quote.toQuote()
        val holdings = quoteWithHoldings.holdings.map { holdingTable ->
          Holding(
              holdingTable.quoteSymbol, holdingTable.shares, holdingTable.price, holdingTable.id!!
          )
        }
        quote.position = Position(quote.symbol, holdings.toMutableList())
        quote.properties = quoteWithHoldings.properties?.toProperties()
        quote
      }
    }
  }

  suspend fun readQuote(symbol: String): Quote? {
    val quoteWithHolding = db.withTransaction { quoteDao.getQuoteWithHoldings(symbol) }
    return withContext(Dispatchers.IO) {
      quoteWithHolding?.let {
        val quote = quoteWithHolding.quote.toQuote()
        val holdings = quoteWithHolding.holdings.map { holdingTable ->
          Holding(
              holdingTable.quoteSymbol, holdingTable.shares, holdingTable.price, holdingTable.id!!
          )
        }
        quote.position = Position(quote.symbol, holdings.toMutableList())
        quote.properties = quoteWithHolding.properties?.toProperties()
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
    val properties = quotes.mapNotNull { it.properties }
    db.withTransaction {
      quoteDao.upsertQuotes(quoteRows)
      positions.forEach {
        quoteDao.upsertHoldings(it.symbol, it.toHoldingRows())
      }
      properties.forEach {
        quoteDao.upsertProperties(it.toPropertiesRow())
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

  suspend fun removeHolding(
    ticker: String,
    holding: Holding
  ) = db.withTransaction {
    quoteDao.deleteHolding(HoldingRow(holding.id, ticker, holding.shares, holding.price))
  }

  suspend fun saveQuoteProperties(
    properties: Properties
  ) = db.withTransaction {
    quoteDao.upsertProperties(properties.toPropertiesRow())
  }

  private fun Quote.toQuoteRow(): QuoteRow {
    return QuoteRow(
        this.symbol, this.name, this.lastTradePrice, this.changeInPercent,
        this.change, this.stockExchange, this.currencyCode,
        this.isPostMarket, this.annualDividendRate, this.annualDividendYield,
        this.dayHigh, this.dayLow, this.previousClose, this.open,
        this.regularMarketVolume?.toFloat(), this.trailingPE,
        this.fiftyTwoWeekLowChange, this.fiftyTwoWeekLowChangePercent,
        this.fiftyTwoWeekHighChange, this.fiftyTwoWeekHighChangePercent,
        this.fiftyTwoWeekLow, this.fiftyTwoWeekHigh,
        this.dividendDate?.toFloat(),
        this.earningsTimestamp?.toFloat(),
        this.marketCap?.toFloat(),
        this.tradeable,
        this.triggerable,
        this.marketState
    )
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
    val quote = Quote(
        symbol = this.symbol,
        name = this.name,
        lastTradePrice = this.lastTradePrice,
        changeInPercent = this.changeInPercent,
        change = this.change
    )
    quote.name = this.name
    quote.lastTradePrice = this.lastTradePrice
    quote.changeInPercent = this.changeInPercent
    quote.change = this.change
    quote.stockExchange = this.stockExchange
    quote.currencyCode = this.currency
    quote.isPostMarket = this.isPostMarket
    quote.annualDividendRate = this.annualDividendRate
    quote.annualDividendYield = this.annualDividendYield
    quote.dayHigh = this.dayHigh
    quote.dayLow = this.dayLow
    quote.previousClose = this.previousClose
    quote.open = this.open
    quote.regularMarketVolume = this.regularMarketVolume?.toLong()
    quote.trailingPE = this.peRatio
    quote.fiftyTwoWeekLowChange = this.fiftyTwoWeekLowChange
    quote.fiftyTwoWeekLowChangePercent = this.fiftyTwoWeekLowChangePercent
    quote.fiftyTwoWeekHighChange = this.fiftyTwoWeekHighChange
    quote.fiftyTwoWeekHighChangePercent = this.fiftyTwoWeekHighChangePercent
    quote.fiftyTwoWeekLow = this.fiftyTwoWeekLow
    quote.fiftyTwoWeekHigh = this.fiftyTwoWeekHigh
    quote.dividendDate = this.dividendDate?.toLong()
    quote.earningsTimestamp = this.earningsDate?.toLong()
    quote.marketCap = this.marketCap?.toLong()
    quote.tradeable = this.isTradeable ?: false
    quote.triggerable = this.isTriggerable ?: false
    quote.marketState = this.marketState ?: ""
    return quote
  }

  private fun PropertiesRow.toProperties(): Properties {
    return Properties(
        this.quoteSymbol, this.notes, this.alertAbove, this.alertBelow
    )
  }

  private fun Properties.toPropertiesRow(): PropertiesRow {
    return PropertiesRow(this.id, this.symbol, this.notes, this.alertAbove, this.alertBelow)
  }
}
