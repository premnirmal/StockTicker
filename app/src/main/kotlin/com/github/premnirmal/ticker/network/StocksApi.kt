package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.CrashLogger
import com.github.premnirmal.ticker.Injector
import com.github.premnirmal.ticker.Tools
import com.github.premnirmal.ticker.network.data.QueryCreator
import com.github.premnirmal.ticker.network.data.Stock
import com.github.premnirmal.ticker.network.data.historicaldata.HistoricalData
import com.google.gson.Gson
import com.google.gson.JsonArray
import rx.Observable
import java.util.ArrayList
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by premnirmal on 3/3/16.
 */
@Singleton class StocksApi @Inject constructor() {

  @Inject internal lateinit var gson: Gson
  @Inject internal lateinit var yahooApi: YahooFinance
  @Inject internal lateinit var googleApi: GoogleFinance

  var lastFetched: Long = 0

  init {
    Injector.inject(this)
  }

  private fun getYahooFinanceStocks(tickers: Array<Any>): Observable<List<Stock>> {
    val query = QueryCreator.buildStocksQuery(tickers)
    return yahooApi.getStocks(query).map({ json ->
      lastFetched = System.currentTimeMillis()
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
    })
  }

  private fun getGoogleFinanceStocks(tickers: Array<Any>): Observable<List<Stock>> {
    val query = QueryCreator.googleStocksQuery(tickers)
    return googleApi.getStock(query).map({ gStocks ->
      lastFetched = System.currentTimeMillis()
      val stocks = gStocks.map { StockConverter.convert(it) }
      val updatedStocks = StockConverter.convertResponseQuotes(stocks)
      updatedStocks
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
            symbol != Stock.GDAXI_TICKER &&
            symbol != Stock.GSPC_TICKER &&
            (symbol.startsWith("^")
                || symbol.startsWith("."))
            || symbol == Stock.XAU_TICKER
            ) {
          googleSymbols.add(symbol.replace("^", "."))
        } else {
          yahooSymbols.add(symbol.replace("^", "%5E"))
        }
      }
    } else {
      for (symbol: String in symbols) {
        if (symbol != Stock.GDAXI_TICKER && symbol != Stock.GSPC_TICKER
            && !symbol.contains("=")) {
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
      val allStocks: Observable<List<Stock>> = Observable.zip(googleObservable, yahooObservable,
          { stocks, stocks2 ->
            val zipped: MutableList<Stock> = ArrayList()
            zipped.addAll(stocks2)
            zipped.addAll(stocks)
            if (zipped.isEmpty()) {
              throw Exception("No stocks returned")
            } else {
              zipped
            }
          })
      return allStocks
    }
  }

}