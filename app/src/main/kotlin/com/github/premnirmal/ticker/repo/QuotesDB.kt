package com.github.premnirmal.ticker.repo

import androidx.room.Database
import androidx.room.RoomDatabase
import com.github.premnirmal.ticker.repo.data.HoldingRow
import com.github.premnirmal.ticker.repo.data.QuoteRow

@Database(entities = [QuoteRow::class, HoldingRow::class], version = 1)
abstract class QuotesDB : RoomDatabase() {
  abstract fun quoteDao(): QuoteDao
}