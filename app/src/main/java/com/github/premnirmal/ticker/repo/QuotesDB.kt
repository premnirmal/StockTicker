package com.github.premnirmal.ticker.repo

import androidx.room.Database
import androidx.room.RoomDatabase
import com.github.premnirmal.ticker.repo.data.FetchLogRow
import com.github.premnirmal.ticker.repo.data.HoldingRow
import com.github.premnirmal.ticker.repo.data.PropertiesRow
import com.github.premnirmal.ticker.repo.data.QuoteRow

@Database(
    entities = [QuoteRow::class, HoldingRow::class, PropertiesRow::class, FetchLogRow::class],
    version = 9,
    exportSchema = true
)
abstract class QuotesDB : RoomDatabase() {
    abstract fun quoteDao(): QuoteDao
}
