package com.github.premnirmal.ticker.repo

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.github.premnirmal.ticker.repo.data.FetchLogRow
import com.github.premnirmal.ticker.repo.data.MovementRow
import com.github.premnirmal.ticker.repo.data.PropertiesRow
import com.github.premnirmal.ticker.repo.data.QuoteRow
import com.github.premnirmal.ticker.repo.data.QuoteWithHoldings

@Dao
interface QuoteDao {

    @Transaction
    @Query("SELECT * FROM QuoteRow")
    suspend fun getQuotesWithHoldings(): List<QuoteWithHoldings>

    @Transaction
    @Query("SELECT * FROM QuoteRow where symbol = :symbol")
    suspend fun getQuoteWithHoldings(symbol: String): QuoteWithHoldings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuotes(quotes: List<QuoteRow>): LongArray

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuote(quote: QuoteRow): Long

    @Transaction
    suspend fun upsertQuoteAndProperties(quote: QuoteRow) {
        upsertQuote(quote)
    }

    @Query("DELETE FROM QuoteRow WHERE symbol = :symbol")
    suspend fun deleteQuoteById(symbol: String)

    @Query("DELETE FROM QuoteRow WHERE symbol IN (:symbols)")
    suspend fun deleteByQuotesId(symbols: List<String>)

    @Transaction
    suspend fun deleteQuoteAndHoldings(symbol: String) {   // keep name; callers unchanged
        deleteQuoteById(symbol)
        deleteMovementsBySymbol(symbol)
        deletePropertiesByQuoteId(symbol)
    }

    @Transaction
    suspend fun deleteQuotesAndHoldings(symbols: List<String>) {
        deleteByQuotesId(symbols)
        deleteMovementsBySymbols(symbols)
        deletePropertiesByQuotesId(symbols)
    }

    @Query("SELECT * FROM MovementRow WHERE quote_symbol = :symbol ORDER BY id ASC")
    suspend fun getMovements(symbol: String): List<MovementRow>

    @Insert
    suspend fun insertMovement(movement: MovementRow): Long

    @Delete
    suspend fun deleteMovement(movement: MovementRow)

    @Query("DELETE FROM MovementRow WHERE quote_symbol = :symbol")
    suspend fun deleteMovementsBySymbol(symbol: String)

    @Query("DELETE FROM MovementRow WHERE quote_symbol IN (:symbols)")
    suspend fun deleteMovementsBySymbols(symbols: List<String>)

    @Transaction
    suspend fun replaceMovements(symbol: String, movements: List<MovementRow>) {
        deleteMovementsBySymbol(symbol)
        movements.forEach { insertMovement(it) }
    }

    @Transaction
    suspend fun upsertProperties(
        propertiesRow: PropertiesRow
    ) {
        if (propertiesRow.quoteSymbol.isNotEmpty()) {
            deletePropertiesByQuoteId(propertiesRow.quoteSymbol)
        }
        if (propertiesRow.notes.isNotEmpty() || propertiesRow.displayname.isNotEmpty() || propertiesRow.alertAbove > 0.0f || propertiesRow.alertBelow > 0.0f) {
            insertProperties(propertiesRow)
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProperties(quote: PropertiesRow)

    @Query("DELETE FROM PropertiesRow WHERE properties_quote_symbol = :symbol")
    suspend fun deletePropertiesByQuoteId(symbol: String)

    @Query("DELETE FROM PropertiesRow WHERE properties_quote_symbol IN (:symbols)")
    suspend fun deletePropertiesByQuotesId(symbols: List<String>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFetchLog(log: FetchLogRow): Long

    @Transaction
    suspend fun insertAndTrimFetchLog(log: FetchLogRow, maxRows: Int) {
        insertFetchLog(log)
        trimFetchLogs(maxRows)
    }

    @Transaction
    suspend fun upsertQuotesWithHoldingsAndProperties(
        quotes: List<QuoteRow>,
        properties: List<PropertiesRow>
    ) {
        upsertQuotes(quotes)
        properties.forEach { upsertProperties(it) }
    }

    @Query("SELECT * FROM FetchLogRow ORDER BY created_at_ms DESC LIMIT :limit")
    suspend fun getFetchLogs(limit: Int): List<FetchLogRow>

    @Query(
        "DELETE FROM FetchLogRow WHERE id NOT IN (SELECT id FROM FetchLogRow ORDER BY created_at_ms DESC LIMIT :maxRows)"
    )
    suspend fun trimFetchLogs(maxRows: Int)
}
