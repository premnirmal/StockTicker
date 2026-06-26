package com.github.premnirmal.ticker.repo

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Android [QuotesDB] builder. Uses the same database file name (`quotes-db`) as the previous
 * Android-only Room setup so existing installs keep their data and run the same migration chain.
 */
fun getQuotesDBBuilder(context: Context): RoomDatabase.Builder<QuotesDB> {
    val appContext = context.applicationContext
    return Room.databaseBuilder<QuotesDB>(
        context = appContext,
        name = appContext.getDatabasePath(QUOTES_DB_NAME).absolutePath
    )
}
