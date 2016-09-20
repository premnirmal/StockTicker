package com.github.premnirmal.ticker.network

import android.util.Log
import com.github.premnirmal.ticker.CrashLogger
import com.github.premnirmal.ticker.Tools
import com.github.premnirmal.ticker.network.historicaldata.HistoricalData
import com.github.premnirmal.ticker.network.historicaldata.Quote
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.internal.LinkedTreeMap
import rx.Observable
import rx.functions.Func2
import java.util.*

/**
 * Created on 3/3/16.
 */
class StocksApi(internal val yahooApi: YahooFinance, internal val googleApi: GoogleFinance) {

  companion object {
    val gson = GsonBuilder().create()
  }

  var lastFetched: Long = 0

  fun getYahooFinanceStocks(tickers: Array<Any>): Observable<List<Stock>> {
    val query = QueryCreator.buildStocksQuery(tickers)
    return yahooApi.getStocks(query).map({ json ->
      lastFetched = System.currentTimeMillis()
      if (json == null) {
        ArrayList<Stock>()
      } else {
        val stocks = ArrayList<Stock>()
        val quoteJson = json.asJsonObject
            .get("query").asJsonObject
            .get("results").asJsonObject
            .get("quote")
        if (quoteJson.isJsonArray) {
          for (stockJson in quoteJson as JsonArray) {
            try {
              val stock = gson.fromJson(stockJson, Stock::class.java)
              stocks.add(stock)
            } catch (e: Exception) {
              CrashLogger.logException(e)
            }
          }
        } else if (quoteJson.isJsonObject) {
          try {
            val stock = gson.fromJson(quoteJson, Stock::class.java)
            stocks.add(stock)
          } catch (e: Exception) {
            CrashLogger.logException(e)
          }
        }
        stocks
      }
    })
  }

  fun getGoogleFinanceStocks(tickers: Array<Any>): Observable<List<Stock>> {
    val query = QueryCreator.googleStocksQuery(tickers)
    return googleApi.getStock(query).map({ gStocks ->
      lastFetched = System.currentTimeMillis()
      val stocks = ArrayList<Stock>()
      for (gStock in gStocks) {
        stocks.add(StockConverter.convert(gStock))
      }
      val updatedStocks = StockConverter.convertResponseQuotes(stocks)
      updatedStocks
    }).onErrorReturn({ throwable ->
      CrashLogger.logException(throwable)
      ArrayList<Stock>()
    })
  }

  fun getHistory(query: String): Observable<HistoricalData> {
    return yahooApi.getHistory(query)
  }

  fun getStocks(tickerList: List<String>): Observable<List<Stock>> {
    val symbols = StockConverter.convertRequestSymbols(tickerList)
    val yahooSymbols = ArrayList<String>()
    val googleSymbols = ArrayList<String>()
    if (!Tools.googleFinanceEnabled()) {
      for (symbol: String in symbols) {
        if (!symbol.contains("=") &&
            !symbol.equals(Stock.GDAXI_TICKER) &&
            (symbol.startsWith("^")
                || symbol.startsWith("."))
            || symbol.equals(Stock.XAU_TICKER)
        ) {
          googleSymbols.add(symbol.replace("^", "."))
        } else {
          yahooSymbols.add(symbol.replace("^", "%5E"))
        }
      }
    } else {
      for (symbol: String in symbols) {
        if (!symbol.equals(Stock.GDAXI_TICKER) && !symbol.contains("=")) {
          googleSymbols.add(symbol.replace("^", "."))
        } else {
          yahooSymbols.add(symbol.replace("^", "%5E"))
        }
      }
    }
    val yahooObservable = getYahooFinanceStocks(yahooSymbols.toArray())
    val googleObservable = getGoogleFinanceStocks(googleSymbols.toArray())
    if (googleSymbols.isEmpty()) {
      return yahooObservable
    } else if (yahooSymbols.isEmpty()) {
      return googleObservable
    } else {
      val allStocks = Observable.zip(googleObservable, yahooObservable, { stocks, stocks2 ->
        val zipped: MutableList<Stock> = ArrayList()
        zipped.addAll(stocks2)
        zipped.addAll(stocks)
        zipped as List<Stock>
      })
      return allStocks
    }
  }

}