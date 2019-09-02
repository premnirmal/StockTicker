package com.github.premnirmal.ticker.settings

import android.os.AsyncTask
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.network.data.Quote
import timber.log.Timber
import java.io.FileOutputStream
import java.io.IOException
import java.util.ArrayList

internal open class TickersExportTask : AsyncTask<List<String>, Void, String>() {

  override fun doInBackground(vararg tickers: List<String>): String? {
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
      return null
    }

    return file.path
  }
}

internal open class PortfolioExportTask : AsyncTask<List<Quote>, Void, String>() {

  private val gson = Injector.appComponent.gson()

  override fun doInBackground(vararg quoteLists: List<Quote>): String? {
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
      return null
    }

    return file.path
  }
}