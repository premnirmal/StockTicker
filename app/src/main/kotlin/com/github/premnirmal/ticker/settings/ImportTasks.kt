package com.github.premnirmal.ticker.settings

import android.appwidget.AppWidgetManager
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

internal interface ImportTask {
  suspend fun import(filePath: String): Boolean
}

internal open class TickersImportTask(private val widgetDataProvider: WidgetDataProvider) :
    ImportTask {

  override suspend fun import(filePath: String): Boolean {
    if (filePath.isEmpty()) {
      return false
    }
    val uri: URI
    try {
      uri = URI(filePath)
    } catch (e: URISyntaxException) {
      Timber.e(e)
      return false
    }

    if (uri.path == null || !uri.path.endsWith(".txt")) {
      return false
    }

    val tickersFile = File(filePath)
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
              widgetData.addTickers(listOf(*tickers))
            }
      } else {
        val widgetData = widgetDataProvider.dataForWidgetId(AppWidgetManager.INVALID_APPWIDGET_ID)
        widgetData.addTickers(listOf(*tickers))
      }
      result = true
    } catch (e: IOException) {
      Timber.e(e)
      result = false
    }

    return result
  }
}

internal open class PortfolioImportTask(private val stocksProvider: IStocksProvider) :
    ImportTask {

  private val gson = Injector.appComponent.gson()

  override suspend fun import(filePath: String): Boolean {
    if (filePath.isEmpty()) {
      return false
    }
    val uri: URI
    try {
      uri = URI(filePath)
    } catch (e: URISyntaxException) {
      Timber.e(e)
      return false
    }

    if (uri.path == null || !uri.path.endsWith(".json")) {
      return false
    }

    val tickersFile = File(filePath)
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

private inline fun <reified T> genericType() = object : TypeToken<T>() {}.type