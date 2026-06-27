package com.github.premnirmal.ticker.settings

import android.content.Context
import android.net.Uri
import com.github.premnirmal.ticker.network.data.Quote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.io.FileOutputStream
import java.io.IOException

internal object TickersExporter : KoinComponent {

    private val portfolioSerializer: PortfolioSerializer by inject()

    suspend fun exportTickers(context: Context, uri: Uri, vararg tickers: List<String>): String? = withContext(
        Dispatchers.IO
    ) {
        val tickerList = ArrayList(tickers[0])
        val contentResolver = context.applicationContext.contentResolver
        try {
            contentResolver.openFileDescriptor(uri, "w")?.use {
                FileOutputStream(it.fileDescriptor).use { fileOutputStream ->
                    fileOutputStream.write(portfolioSerializer.serializeTickers(tickerList).toByteArray())
                }
            }
        } catch (e: IOException) {
            Timber.e(e)
            return@withContext null
        }

        return@withContext uri.path
    }
}

internal object PortfolioExporter : KoinComponent {

    private val portfolioSerializer: PortfolioSerializer by inject()

    suspend fun exportQuotes(context: Context, uri: Uri, vararg quoteLists: List<Quote>): String? =
        withContext(Dispatchers.IO) {
            val quoteList: List<Quote> = quoteLists[0]
            val jsonString = portfolioSerializer.serializePortfolio(quoteList)
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
