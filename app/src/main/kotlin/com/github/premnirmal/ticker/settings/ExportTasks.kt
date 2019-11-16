package com.github.premnirmal.ticker.settings

import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.network.data.Quote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.FileOutputStream
import java.io.IOException
import java.util.ArrayList

internal object TickersExporter {

  suspend fun exportTickers(vararg tickers: List<String>): String? = withContext(Dispatchers.IO) {
    val file = AppPreferences.tickersFile
    val tickerList = ArrayList(tickers[0])
    try {
      if (file.exists()) {
        file.delete()
        file.createNewFile()
      }
      val fileOutputStream = FileOutputStream(file)
      for (ticker: String in tickerList) {
        fileOutputStream.use { it.write(("$ticker, ").toByteArray()) }
      }
    } catch (e: IOException) {
      Timber.w(e)
      return@withContext null
    }

    return@withContext file.path
  }
}

internal object PortfolioExporter {

  private val gson = Injector.appComponent.gson()

  suspend fun exportQuotes(vararg quoteLists: List<Quote>): String? = withContext(Dispatchers.IO) {
    val file = AppPreferences.portfolioFile
    val quoteList: List<Quote> = quoteLists[0]
    try {
      if (file.exists()) {
        file.delete()
        file.createNewFile()
      }
      val jsonString = gson.toJson(quoteList)
      val fileOutputStream = FileOutputStream(file)
      fileOutputStream.use { it.write(jsonString.toByteArray()) }
    } catch (e: IOException) {
      Timber.w(e)
      return@withContext null
    }

    return@withContext file.path
  }
}