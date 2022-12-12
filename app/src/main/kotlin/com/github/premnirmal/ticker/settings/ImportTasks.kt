package com.github.premnirmal.ticker.settings

import android.appwidget.AppWidgetManager
import android.content.Context
import android.net.Uri
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.model.StocksProvider
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import com.google.gson.reflect.TypeToken
import timber.log.Timber
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

internal interface ImportTask {

  suspend fun import(context: Context, fileUri: Uri): Boolean
}

internal open class TickersImportTask(private val widgetDataProvider: WidgetDataProvider) :
    ImportTask {

  override suspend fun import(context: Context, fileUri: Uri): Boolean {
    var result = false
    val contentResolver = context.applicationContext.contentResolver
    try {
      contentResolver.openInputStream(fileUri)
          ?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
              val text: String = reader.readText()
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
                val widgetData =
                  widgetDataProvider.dataForWidgetId(AppWidgetManager.INVALID_APPWIDGET_ID)
                widgetData.addTickers(listOf(*tickers))
              }
              result = true
            }
          }
    } catch (e: IOException) {
      Timber.e(e)
      result = false
    }

    return result
  }
}

internal open class PortfolioImportTask(private val stocksProvider: StocksProvider) :
    ImportTask {

  private val gson = Injector.appComponent().gson()

  override suspend fun import(context: Context, fileUri: Uri): Boolean {
    val contentResolver = context.applicationContext.contentResolver
    return try {
      contentResolver.openInputStream(fileUri)?.use { inputStream ->
        BufferedReader(InputStreamReader(inputStream)).use { reader ->
          val json: String = reader.readText()
          val portfolio: List<Quote> = gson.fromJson(json, genericType<List<Quote>>())
          stocksProvider.addPortfolio(portfolio)
          true
        }
      } ?: false
    } catch (e: Exception) {
      Timber.w(e)
      return false
    }
  }
}

private inline fun <reified T> genericType() = object : TypeToken<T>() {}.type