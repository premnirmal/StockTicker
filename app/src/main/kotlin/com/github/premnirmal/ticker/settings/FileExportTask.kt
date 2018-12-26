package com.github.premnirmal.ticker.settings

import android.os.AsyncTask
import com.github.premnirmal.ticker.AppPreferences
import timber.log.Timber
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.ArrayList

/**
 * Created by premnirmal on 2/27/16.
 */
internal open class FileExportTask : AsyncTask<List<String>, Void, String>() {

  override fun doInBackground(vararg tickers: List<String>): String? {
    val file = AppPreferences.tickersFile
    val tickerList: List<String> = ArrayList(tickers[0])
    try {
      if (file.exists()) {
        file.delete()
        file.createNewFile()
      }
      val fileOutputStream = FileOutputStream(file)
      for (ticker: String in tickerList) {
        fileOutputStream.write(("$ticker, ").toByteArray())
      }
      fileOutputStream.flush()
      fileOutputStream.close()
    } catch (e: FileNotFoundException) {
      Timber.w(Exception(e))
      return null
    } catch (e: IOException) {
      Timber.w(Exception(e))
      return null
    }

    return file.path
  }
}