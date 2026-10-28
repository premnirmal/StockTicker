package com.github.premnirmal.ticker.settings

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

/**
 * The iOS DataStore Preferences file path, stored in the app's Documents directory (alongside the
 * Room `quotes-db`). DataStore appends nothing, so the name carries the required `*.preferences_pb`
 * suffix.
 */
@OptIn(ExperimentalForeignApi::class)
fun iosPreferencesDataStorePath(): String {
    val documentsUrl: NSURL = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null
    )!!
    return requireNotNull(documentsUrl.URLByAppendingPathComponent(PREFERENCES_FILE_NAME)?.path)
}

private const val PREFERENCES_FILE_NAME = "ticker.preferences_pb"
