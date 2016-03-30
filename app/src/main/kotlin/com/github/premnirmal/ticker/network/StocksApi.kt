package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.CrashLogger
import com.github.premnirmal.ticker.Tools
import com.github.premnirmal.ticker.network.historicaldata.HistoricalData
import rx.Observable
import rx.functions.Func2
import java.util.*

/**
 * Created on 3/3/16.
 */
class StocksApi(internal val yahooApi: YahooFinance, internal val googleApi: GoogleFinance) {

  var lastFetched: String? = null

  fun getYahooFinanceStocks(tickers: Array<Any>): Observable<List<Stock>> {
    val query = QueryCreator.buildStocksQuery(tickers)
    return yahooApi.getStocks(query).map({ stockQuery ->
      if (stockQuery == null) {
        ArrayList()
      } else {
        val query = stockQuery.query
        lastFetched = query.created
        query.results.quote
      }
    })
  }

  fun getGoogleFinanceStocks(tickers: Array<Any>): Observable<List<Stock>> {
    val query = QueryCreator.googleStocksQuery(tickers)
    return googleApi.getStock(query).map({ gStocks ->
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
    if (!Tools.googleFinanceEnabled()) {
      val yahooObservable = getYahooFinanceStocks(symbols.toArray())
      return yahooObservable
    } else {
      val yahooSymbols = ArrayList<String>()
      val googleSymbols = ArrayList<String>()
      for (symbol: String in symbols) {
        if (!symbol.equals(Stock.GDAXI_TICKER) && (symbol.startsWith("^") || symbol.startsWith("."))) {
          googleSymbols.add(symbol.replace("^", "."))
        } else {
          yahooSymbols.add(symbol.replace("^", "%5E"))
        }
      }
      val yahooObservable = getYahooFinanceStocks(yahooSymbols.toArray())
      if (googleSymbols.isEmpty()) {
        return yahooObservable
      } else {
        val googleObservable = getGoogleFinanceStocks(googleSymbols.toArray())
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

}