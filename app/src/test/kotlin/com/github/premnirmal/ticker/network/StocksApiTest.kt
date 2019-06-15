package com.github.premnirmal.ticker.network

import android.content.SharedPreferences
import com.github.premnirmal.ticker.BaseUnitTest
import com.github.premnirmal.ticker.mock.Mocker
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.network.data.QuoteNet
import com.github.premnirmal.ticker.network.data.YahooResponse
import com.google.gson.reflect.TypeToken
import io.reactivex.Observable
import io.reactivex.exceptions.CompositeException
import io.reactivex.observers.TestObserver
import okhttp3.MediaType
import okhttp3.ResponseBody
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Matchers
import org.mockito.Mockito.`when`
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import retrofit2.HttpException
import retrofit2.Response
import java.net.SocketTimeoutException

class StocksApiTest : BaseUnitTest() {

  companion object {
    val TEST_TICKER_LIST = arrayListOf("SPY", "GOOG", "MSFT", "DIA", "AAPL")
  }

  internal lateinit var robinhoodFinance: Robindahood
  internal lateinit var yahooFinance: YahooFinance
  internal lateinit var mockPrefs: SharedPreferences

  private val stocksApi = StocksApi()

  @Before fun initMocks() {
    robinhoodFinance = Mocker.provide(Robindahood::class)
    yahooFinance = Mocker.provide(YahooFinance::class)
    mockPrefs = Mocker.provide(SharedPreferences::class)
    val listType = object : TypeToken<List<QuoteNet>>() {}.type
    val stockList = parseJsonFile<List<QuoteNet>>(listType, "Quotes.json")
    val yahooStockList = parseJsonFile<YahooResponse>(YahooResponse::class.java, "YahooQuotes.json")
    val yahooStocks = Observable.just(yahooStockList)
    val stocks = Observable.just(stockList)
    `when`(robinhoodFinance.getStocks(Matchers.anyString())).thenReturn(stocks)
    `when`(yahooFinance.getStocks(Matchers.anyString())).thenReturn(yahooStocks)
  }

  @After fun clear() {
    Mocker.clearMocks()
  }

  @Test fun testGetStocks() {
    val testTickerList = TEST_TICKER_LIST
    val subscriber = TestObserver<List<Quote>>()
    stocksApi.getStocks(testTickerList)
        .subscribe(subscriber)
    subscriber.assertNoErrors()
    verify(robinhoodFinance).getStocks(anyString())
    val onNextEvents = subscriber.events[0]
    assertTrue(onNextEvents.size == 1)
    val stocks = onNextEvents[0] as List<*>
    assertEquals(testTickerList.size, stocks.size)
  }

  @Test fun testFallbackToYahoo() {
    val error = HttpException(
        Response.error<Any>(
            400, ResponseBody.create(
            MediaType.parse("application/json"),
            "{}"
        )
        )
    )
    val result = Observable.error<List<QuoteNet>>(error)
    `when`(robinhoodFinance.getStocks(Matchers.anyString())).thenReturn(result)
    val testTickerList = TEST_TICKER_LIST
    val subscriber = TestObserver<List<Quote>>()
    stocksApi.getStocks(testTickerList)
        .subscribe(subscriber)
    subscriber.assertNoErrors()
    verify(robinhoodFinance).getStocks(anyString())
    verify(yahooFinance).getStocks(anyString())
    val onNextEvents = subscriber.events[0]
    assertTrue(onNextEvents.size == 1)
    val stocks = onNextEvents[0] as List<*>
    assertEquals(testTickerList.size, stocks.size)
  }

  @Test fun testFailure() {
    val error = SocketTimeoutException()
    val result = Observable.error<List<QuoteNet>>(error)
    `when`(robinhoodFinance.getStocks(Matchers.anyString())).thenReturn(result)
    val testTickerList = TEST_TICKER_LIST
    val subscriber = TestObserver<List<Quote>>()
    stocksApi.getStocks(testTickerList)
        .subscribe(subscriber)
    subscriber.assertError { e ->
      e is CompositeException
    }
    verify(robinhoodFinance).getStocks(anyString())
    verify(yahooFinance, never()).getStocks(anyString())
    val onNextEvents = subscriber.events[0]
    assertTrue(onNextEvents.isEmpty())
  }
}