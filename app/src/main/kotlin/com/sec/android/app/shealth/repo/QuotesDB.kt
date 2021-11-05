package com.sec.android.app.shealth.repo

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sec.android.app.shealth.repo.data.HoldingRow
import com.sec.android.app.shealth.repo.data.PropertiesRow
import com.sec.android.app.shealth.repo.data.QuoteRow

@Database(
    entities = [QuoteRow::class, HoldingRow::class, PropertiesRow::class], version = 2,
    exportSchema = true
)
abstract class QuotesDB : RoomDatabase() {
  abstract fun quoteDao(): QuoteDao
}