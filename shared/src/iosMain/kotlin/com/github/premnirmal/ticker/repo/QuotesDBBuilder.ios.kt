package com.github.premnirmal.ticker.repo

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

/**
 * iOS [QuotesDB] builder. Stores the database in the app's Documents directory under the same
 * `quotes-db` file name so the shared schema and migration chain apply identically to Android.
 */
@OptIn(ExperimentalForeignApi::class)
fun getQuotesDBBuilder(): RoomDatabase.Builder<QuotesDB> {
    val documentsUrl: NSURL = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null
    )!!
    val dbFilePath = requireNotNull(documentsUrl.URLByAppendingPathComponent(QUOTES_DB_NAME)?.path)
    return Room.databaseBuilder<QuotesDB>(name = dbFilePath)
}
