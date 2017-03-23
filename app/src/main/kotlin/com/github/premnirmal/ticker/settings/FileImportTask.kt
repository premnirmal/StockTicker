package com.github.premnirmal.ticker.settings

import android.os.AsyncTask
import android.text.TextUtils
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.CrashLogger
import com.github.premnirmal.ticker.Analytics
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.util.*

/**
 * Created by premnirmal on 2/27/16.
 */
internal open class FileImportTask(
    private val stocksProvider: IStocksProvider) : AsyncTask<String, Void, Boolean>() {

  override fun doInBackground(vararg params: String?): Boolean? {
    if (params.size == 0 || params[0] == null) {
      return false
    }
    val uri: URI?
    try {
      uri = URI(params[0])
    } catch (e: URISyntaxException) {
      e.printStackTrace()
      return false
    }

    if (uri == null || uri.path == null || !uri.path.endsWith(".txt")) {
      return false
    }

    val tickersFile = File(params[0])
    var result = false

    if (!tickersFile.exists()) {
      return false
    }
    val text = StringBuilder()
    try {
      val br = BufferedReader(FileReader(tickersFile))
      var line: String? = br.readLine()
      while (line != null) {
        text.append(line)
        line = br.readLine()
      }
      val tickers = text.toString().replace(" ".toRegex(), "").split(
          ",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
      stocksProvider.addStocks(Arrays.asList(*tickers))
      result = true
      Analytics.trackSettingsChange("IMPORT", TextUtils.join(",", tickers))
    } catch (e: IOException) {
      CrashLogger.logException(RuntimeException(e))
      result = false
    }

    return result
  }
}