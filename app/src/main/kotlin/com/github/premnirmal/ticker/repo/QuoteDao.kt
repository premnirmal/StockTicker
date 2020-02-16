package com.github.premnirmal.ticker.repo

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.github.premnirmal.ticker.repo.data.HoldingRow
import com.github.premnirmal.ticker.repo.data.QuoteRow
import com.github.premnirmal.ticker.repo.data.QuoteWithHoldings

@Dao
interface QuoteDao {

  @Transaction
  @Query("SELECT * FROM QuoteRow")
  suspend fun getQuotesWithHoldings(): List<QuoteWithHoldings>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  @JvmSuppressWildcards
  suspend fun upsertQuotes(quotes: List<QuoteRow>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  @JvmSuppressWildcards
  suspend fun upsertQuote(quote: QuoteRow)

  @Transaction
  suspend fun upsertQuotesAndHoldings(quotes: List<QuoteRow>, holdings: List<HoldingRow>) {
    upsertQuotes(quotes)
    upsertHoldings(holdings)
  }

  @Transaction
  suspend fun upsertQuoteAndHolding(quote: QuoteRow, holdings: List<HoldingRow>?) {
    upsertQuote(quote)
    holdings?.let { upsertHoldings(it) }
  }

  @Transaction
  suspend fun deleteQuoteAndHoldings(quote: QuoteRow, holdings: List<HoldingRow>?) {
    deleteQuote(quote)
    holdings?.let { deleteHoldings(it) }
  }

  @Transaction
  suspend fun deleteQuoteAndHoldings(symbol: String) {
    deleteQuoteById(symbol)
    deleteHoldingsByQuoteId(symbol)
  }

  @Transaction
  suspend fun deleteQuotesAndHoldings(symbols: List<String>) {
    deleteByQuotesId(symbols)
    deleteHoldingsByQuoteIds(symbols)
  }

  @Query("DELETE FROM QuoteRow WHERE symbol = :symbol")
  suspend fun deleteQuoteById(symbol: String)

  @Query("DELETE FROM HoldingRow WHERE quote_symbol = :symbol")
  suspend fun deleteHoldingsByQuoteId(symbol: String)

  @Query("DELETE FROM QuoteRow WHERE symbol IN (:symbols)")
  suspend fun deleteByQuotesId(symbols: List<String>)

  @Query("DELETE FROM HoldingRow WHERE quote_symbol IN (:symbols)")
  suspend fun deleteHoldingsByQuoteIds(symbols: List<String>)

  @Delete
  suspend fun deleteQuote(quote: QuoteRow)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  @JvmSuppressWildcards
  suspend fun upsertHoldings(holdings: List<HoldingRow>)

  @Delete
  suspend fun deleteHolding(holding: HoldingRow)

  @Delete
  @JvmSuppressWildcards
  suspend fun deleteHoldings(holdings: List<HoldingRow>)
}