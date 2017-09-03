package com.github.premnirmal.ticker.settings

import android.appwidget.AppWidgetManager
import android.os.AsyncTask
import android.text.TextUtils
import com.github.premnirmal.ticker.components.Analytics
import timber.log.Timber
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.util.Arrays

/**
 * Created by premnirmal on 2/27/16.
 */
internal open class FileImportTask(
    private val widgetDataProvider: WidgetDataProvider) : AsyncTask<String, Void, Boolean>() {

  override fun doInBackground(vararg params: String?): Boolean? {
    if (params.isEmpty() || params[0] == null) {
      return false
    }
    val uri: URI
    try {
      uri = URI(params[0])
    } catch (e: URISyntaxException) {
      Timber.w(e)
      return false
    }

    if (uri.path == null || !uri.path.endsWith(".txt")) {
      return false
    }

    val tickersFile = File(params[0])
    var result: Boolean

    if (!tickersFile.exists()) {
      return false
    }
    val text = StringBuilder()
    try {
      val br = BufferedReader(FileReader(tickersFile))
      var line: String? = br.readLine()
      line?.let {
        text.append(it)
        line = br.readLine()
      }
      val tickers = text.toString()
          .replace(" ".toRegex(), "")
          .split(",".toRegex())
          .dropLastWhile(String::isEmpty)
          .toTypedArray()
      if (widgetDataProvider.hasWidget()) {
        widgetDataProvider.getAppWidgetIds().forEach { widgetId ->
          val widgetData = widgetDataProvider.dataForWidgetId(widgetId)
          widgetData.addTickers(Arrays.asList(*tickers))
        }
      } else {
        val widgetData = widgetDataProvider.dataForWidgetId(AppWidgetManager.INVALID_APPWIDGET_ID)
        widgetData.addTickers(Arrays.asList(*tickers))
      }
      result = true
      Analytics.INSTANCE.trackSettingsChange("IMPORT", TextUtils.join(",", tickers))
    } catch (e: IOException) {
      Timber.w(e)
      result = false
    }

    return result
  }
}