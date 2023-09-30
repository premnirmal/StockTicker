package com.github.premnirmal.ticker.settings

import android.content.Context
import android.net.Uri
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.widget.WidgetData
import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

internal object TickersExporter {

  suspend fun exportTickers(file: File, vararg tickers: List<String>): String? = withContext(Dispatchers.IO) {
    val tickerList = ArrayList(tickers[0])
    try {
      val fileOutputStream = FileOutputStream(file)
      for (ticker: String in tickerList) {
        fileOutputStream.use { it.write(("$ticker, ").toByteArray()) }
      }
    } catch (e: IOException) {
      Timber.e(e)
      return@withContext null
    }

    return@withContext file.path
  }

  suspend fun exportTickers(context: Context, uri: Uri, vararg tickers: List<String>): String? = withContext(Dispatchers.IO) {
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

  private val gson = Injector.appComponent().gson()

  suspend fun exportQuotes(file: File, vararg quoteLists: List<Quote>): String? = withContext(Dispatchers.IO) {
    val quoteList: List<Quote> = quoteLists[0]
    try {
      val jsonString = gson.toJson(quoteList)
      val fileOutputStream = FileOutputStream(file)
      fileOutputStream.use { it.write(jsonString.toByteArray()) }
    } catch (e: IOException) {
      Timber.e(e)
      return@withContext null
    }

    return@withContext file.path
  }

  suspend fun exportQuotes(context: Context, uri: Uri, vararg quoteLists: List<Quote>): String? =
    withContext(Dispatchers.IO) {
      val quoteList: List<Quote> = quoteLists[0]
      val jsonString = gson.toJson(quoteList)
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
internal object WidgetExporter {
  private val gson = Injector.appComponent().gson()
    suspend fun exportWidget(file: File, vararg widgetData: WidgetData): String? =
    withContext(Dispatchers.IO){
    try {
      val jsonString = getJSONFrom(widgetData[0])
      val fileOutputStream = FileOutputStream(file)
      fileOutputStream.use { it.write(jsonString.toByteArray()) }
    } catch (e: IOException){
      Timber.e(e)
      return@withContext null
    }
    return@withContext file.path
  }
  suspend fun exportWidget(context: Context, uri: Uri, vararg widgetData: WidgetData): String? =
    withContext(Dispatchers.IO) {
      try {
        val jsonString = getJSONFrom(widgetData[0])
        val contentResolver = context.applicationContext.contentResolver

         contentResolver.openFileDescriptor(uri, "rwt")
          ?.use {
            FileOutputStream(it.fileDescriptor).use { fileOutputStream ->
              fileOutputStream.write((jsonString.toByteArray()))
            }
          }
      } catch (e: IOException) {
        Timber.e(e)
        return@withContext  null
      }
      return@withContext uri.path
    }

  private fun getJSONFrom(widgetData: WidgetData): String {
    val json = gson.newBuilder().serializeNulls().setPrettyPrinting()
    //json.setExclusionStrategies(FieldExcluder)
    val exportWidget = ExportWidget(widgetData)
    val mygson :String = json.create().toJson(exportWidget)
    return mygson
  }

  object FieldExcluder : ExclusionStrategy {
    override fun shouldSkipField(f: FieldAttributes?): Boolean {
      val name = f?.getAnnotation(SerializedName::class.java)?.value
      if (name ==null) return true
      return false
    }

    override fun shouldSkipClass(clazz: Class<*>?): Boolean {
      val qualifier = clazz?.name
      when (qualifier) {
        "com.github.premnirmal.ticker.widget.WidgetData" -> return false
        "com.github.premnirmal.ticker.AppPreferences" -> return false
        "java.lang.String" -> return false
        "java.util.List" -> return false
        "android.content.SharedPreferences" -> return false
        "int" -> return false
        "java.lang.Integer" -> return false
        "android.app.SharedPreferencesImpl" -> return false

        else
          -> return true
      }
    }
  }

}