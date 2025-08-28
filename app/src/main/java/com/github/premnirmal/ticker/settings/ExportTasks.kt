package com.github.premnirmal.ticker.settings

import android.content.Context
import android.net.Uri
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.network.data.Quote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.FileOutputStream
import java.io.IOException

internal object TickersExporter {

    suspend fun exportTickers(context: Context, uri: Uri, vararg tickers: List<String>): String? = withContext(
        Dispatchers.IO
    ) {
        val tickerList = ArrayList(tickers[0])
        val contentResolver = context.applicationContext.contentResolver
        try {
            contentResolver.openFileDescriptor(uri, "w")?.use {
                FileOutputStream(it.fileDescriptor).use { fileOutputStream ->
                    for (ticker in tickerList) {
                        fileOutputStream.write(("$ticker, ").toByteArray())
                    }
                }
            }
        } catch (e: IOException) {
            Timber.e(e)
            return@withContext null
        }

        return@withContext uri.path
    }
}

internal object PortfolioExporter {

    private val json = Injector.appComponent().json()

    suspend fun exportQuotes(context: Context, uri: Uri, vararg quoteLists: List<Quote>): String? =
        withContext(Dispatchers.IO) {
            val quoteList: List<Quote> = quoteLists[0]
            val jsonString = json.encodeToString(quoteList)
            val contentResolver = context.applicationContext.contentResolver
            try {
                contentResolver.openFileDescriptor(uri, "rwt")
                    ?.use {
                        FileOutputStream(it.fileDescriptor).use { fileOutputStream ->
                            fileOutputStream.write(jsonString.toByteArray())
                        }
                    }
            } catch (e: IOException) {
                Timber.e(e)
                return@withContext null
            }
            return@withContext uri.path
        }
}
