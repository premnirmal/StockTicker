package com.github.premnirmal.ticker.repo

import com.github.premnirmal.ticker.components.ioDispatcher
import com.github.premnirmal.ticker.network.data.Movement
import com.github.premnirmal.ticker.network.data.MovementType
import com.github.premnirmal.ticker.network.data.Properties
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.network.data.replayLedger
import com.github.premnirmal.ticker.network.data.toPosition
import com.github.premnirmal.ticker.repo.data.FetchLogRow
import com.github.premnirmal.ticker.repo.data.MovementRow
import com.github.premnirmal.ticker.repo.data.PropertiesRow
import com.github.premnirmal.ticker.repo.data.QuoteRow
import kotlinx.coroutines.withContext

/**
 * Multiplatform [QuoteStorage] implementation backed by the shared Room KMP engine ([QuoteDao]) plus
 * a small [TickersStore] for the watchlist symbol set. The Room transactions are expressed via
 * `@Transaction` DAO methods (multiplatform) rather than the Android-only `withTransaction`
 * extension, so the same implementation runs on Android and iOS.
 *
 * Created by premnirmal on 2/28/16.
 */
class StocksStorage(
    private val tickersStore: TickersStore,
    private val quoteDao: QuoteDao
) : QuoteStorage {

    companion object {
        private const val MAX_FETCH_LOG_ROWS = 500
    }

    override fun saveTickers(tickers: Set<String>) {
        tickersStore.saveTickers(tickers)
    }

    override fun readTickers(): Set<String> {
        return tickersStore.readTickers()
    }

    override suspend fun readQuotes(): List<Quote> {
        val quotesWithHoldings = quoteDao.getQuotesWithHoldings()
        return withContext(ioDispatcher) {
            quotesWithHoldings.map { quoteWithHoldings ->
                val quote = quoteWithHoldings.quote.toQuote()
                val movements = quoteWithHoldings.movements
                    .sortedBy { it.id ?: 0L }
                    .map { it.toMovement() }
                quote.movements = movements
                quote.position = movements.replayLedger().toPosition(quote.symbol)
                quote.properties = quoteWithHoldings.properties?.toProperties()
                quote
            }
        }
    }

    override suspend fun readQuote(symbol: String): Quote? {
        val quoteWithHolding = quoteDao.getQuoteWithHoldings(symbol)
        return withContext(ioDispatcher) {
            quoteWithHolding?.let {
                val quote = quoteWithHolding.quote.toQuote()
                val movements = quoteWithHolding.movements
                    .sortedBy { it.id ?: 0L }
                    .map { it.toMovement() }
                quote.movements = movements
                quote.position = movements.replayLedger().toPosition(quote.symbol)
                quote.properties = quoteWithHolding.properties?.toProperties()
                quote
            }
        }
    }

    override suspend fun saveQuote(quote: Quote) {
        quoteDao.upsertQuoteAndProperties(quote.toQuoteRow())
    }

    override suspend fun saveQuotes(quotes: List<Quote>) {
        val quoteRows = quotes.map { it.toQuoteRow() }
        val properties = quotes.mapNotNull { it.properties }
        quoteDao.upsertQuotesWithHoldingsAndProperties(
            quoteRows,
            properties.map { it.toPropertiesRow() }
        )
    }

    override suspend fun removeQuoteBySymbol(symbol: String) {
        quoteDao.deleteQuoteAndHoldings(symbol)
    }

    override suspend fun removeQuotesBySymbol(tickers: List<String>) {
        quoteDao.deleteQuotesAndHoldings(tickers)
    }

    override suspend fun addMovement(movement: Movement): Long {
        return quoteDao.insertMovement(movement.toMovementRow())
    }

    override suspend fun removeMovement(movement: Movement) {
        quoteDao.deleteMovement(movement.toMovementRow())
    }

    override suspend fun readMovements(symbol: String): List<Movement> {
        return quoteDao.getMovements(symbol).map { it.toMovement() }
    }

    override suspend fun saveMovements(symbol: String, movements: List<Movement>) {
        quoteDao.replaceMovements(symbol, movements.map { it.toMovementRow() })
    }

    override suspend fun saveQuoteProperties(
        properties: Properties
    ) {
        quoteDao.upsertProperties(properties.toPropertiesRow())
    }

    suspend fun addFetchLog(
        createdAtMs: Long,
        source: String,
        event: String,
        detail: String
    ) {
        quoteDao.insertAndTrimFetchLog(
            FetchLogRow(
                createdAtMs = createdAtMs,
                source = source,
                event = event,
                detail = detail
            ),
            MAX_FETCH_LOG_ROWS
        )
    }

    suspend fun readFetchLogs(limit: Int): List<FetchLogRow> = quoteDao.getFetchLogs(limit)

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
            this.marketState,
            this.fiftyDayAverage,
            this.twoHundredDayAverage
        )
    }

    private fun Movement.toMovementRow(): MovementRow =
        MovementRow(this.id, this.symbol, this.type.name, this.shares, this.price)

    private fun MovementRow.toMovement(): Movement =
        Movement(this.quoteSymbol, MovementType.valueOf(this.type), this.shares, this.price, this.id)

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
        quote.fiftyDayAverage = this.fiftyDayAverage
        quote.twoHundredDayAverage = this.twoHundredDayAverage
        return quote
    }

    private fun PropertiesRow.toProperties(): Properties {
        return Properties(
            this.quoteSymbol,
            this.notes,
            this.displayname,
            this.alertAbove,
            this.alertBelow
        )
    }

    private fun Properties.toPropertiesRow(): PropertiesRow {
        return PropertiesRow(this.id, this.symbol, this.notes, this.displayname, this.alertAbove, this.alertBelow)
    }
}
