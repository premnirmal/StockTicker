package com.github.premnirmal.ticker.settings

import android.appwidget.AppWidgetManager
import android.os.AsyncTask
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import com.google.gson.reflect.TypeToken
import timber.log.Timber
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.util.Arrays

internal abstract class ImportTask: AsyncTask<String, Nothing, Boolean>()

internal open class TickersImportTask(private val widgetDataProvider: WidgetDataProvider) :
    ImportTask() {

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
    try {
      val reader = FileReader(tickersFile)
      val text: String = reader.use { it.readText() }
      val tickers = text
          .replace(" ".toRegex(), "")
          .split(",".toRegex())
          .dropLastWhile(String::isEmpty)
          .toTypedArray()
      if (widgetDataProvider.hasWidget()) {
        widgetDataProvider.getAppWidgetIds()
            .forEach { widgetId ->
              val widgetData = widgetDataProvider.dataForWidgetId(widgetId)
              widgetData.addTickers(Arrays.asList(*tickers))
            }
      } else {
        val widgetData = widgetDataProvider.dataForWidgetId(AppWidgetManager.INVALID_APPWIDGET_ID)
        widgetData.addTickers(Arrays.asList(*tickers))
      }
      result = true
    } catch (e: IOException) {
      Timber.w(e)
      result = false
    }

    return result
  }
}

internal open class PortfolioImportTask(private val stocksProvider: IStocksProvider) :
    ImportTask() {

  private val gson = Injector.appComponent.gson()

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

    if (uri.path == null || !uri.path.endsWith(".json")) {
      return false
    }

    val tickersFile = File(params[0])
    if (!tickersFile.exists()) {
      return false
    }

    return try {
      val reader = FileReader(tickersFile)
      val json: String = reader.use { it.readText() }
      val portfolio: List<Quote> = gson.fromJson(json, genericType<List<Quote>>())
      stocksProvider.addPortfolio(portfolio)
      true
    } catch (e: IOException) {
      Timber.w(e)
      false
    }
  }
}

private inline fun <reified T> genericType() = object: TypeToken<T>() {}.type