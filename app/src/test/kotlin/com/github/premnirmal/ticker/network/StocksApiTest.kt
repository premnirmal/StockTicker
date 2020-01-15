package com.github.premnirmal.ticker.network

import android.content.SharedPreferences
import com.github.premnirmal.ticker.BaseUnitTest
import com.github.premnirmal.ticker.mock.Mocker
import com.github.premnirmal.ticker.network.data.QuoteNet
import com.github.premnirmal.ticker.network.data.YahooResponse
import com.google.gson.reflect.TypeToken
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType
import okhttp3.ResponseBody
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class StocksApiTest : BaseUnitTest() {

  companion object {
    val TEST_TICKER_LIST = arrayListOf("SPY", "GOOG", "MSFT", "DIA", "AAPL")
  }

  internal lateinit var robinhoodFinance: Robindahood
  internal lateinit var yahooFinance: YahooFinance
  internal lateinit var mockPrefs: SharedPreferences

  private val stocksApi = StocksApi()

  @Before fun initMocks() {
    runBlocking {
      StocksApi.DEBUG = false
      robinhoodFinance = Mocker.provide(Robindahood::class)
      yahooFinance = Mocker.provide(YahooFinance::class)
      mockPrefs = Mocker.provide(SharedPreferences::class)
      val listType = object : TypeToken<List<QuoteNet>>() {}.type
      val stockList = parseJsonFile<List<QuoteNet>>(listType, "Quotes.json")
      val yahooStockList =
        parseJsonFile<YahooResponse>(YahooResponse::class.java, "YahooQuotes.json")
      whenever(robinhoodFinance.getStocks(any())).thenReturn(stockList)
      whenever(yahooFinance.getStocks(any())).thenReturn(yahooStockList)
    }
  }

  @After fun clear() {
    Mocker.clearMocks()
  }

  @Test fun testGetStocks() {
    runBlocking {
      val testTickerList = TEST_TICKER_LIST
      val stocks = stocksApi.getStocks(testTickerList)
      verify(robinhoodFinance).getStocks(any())
      assertEquals(testTickerList.size, stocks.data.size)
    }
  }

  @Test fun testFallbackToYahoo() {
    runBlocking {
      val error = HttpException(
          Response.error<Any>(
              400, ResponseBody.create(
              MediaType.parse("application/json"),
              "{}"
          )
          )
      )
      doThrow(error).whenever(robinhoodFinance)
          .getStocks(any())
      val testTickerList = TEST_TICKER_LIST
      val result = stocksApi.getStocks(testTickerList)
      verify(robinhoodFinance).getStocks(any())
      verify(yahooFinance).getStocks(any())
      assertEquals(testTickerList.size, result.data.size)
    }
  }

  @Test
  fun testFailure() {
    runBlocking {
      val error = RuntimeException()
      doThrow(error).whenever(robinhoodFinance)
          .getStocks(any())
      doThrow(error).whenever(yahooFinance)
          .getStocks(any())
      val testTickerList = TEST_TICKER_LIST
      val result = stocksApi.getStocks(testTickerList)
      assertFalse(result.wasSuccessful)
      assertTrue(result.wasAuthorized)
      assertTrue(result.hasError)
      verify(robinhoodFinance).getStocks(any())
      verify(yahooFinance).getStocks(any())
    }
  }

  @Test
  fun testUnauthorized() {
    runBlocking {
      val error = HttpException(
          Response.error<List<QuoteNet>>(
              401, ResponseBody.create(
              MediaType.parse("application/json"),
              "{}"
          )
          )
      )
      doThrow(error).whenever(robinhoodFinance)
          .getStocks(any())
      val testTickerList = TEST_TICKER_LIST
      val result = stocksApi.getStocks(testTickerList)
      assertTrue(result.hasError)
      assertFalse(result.wasSuccessful)
      assertFalse(result.wasAuthorized)
      verify(robinhoodFinance).getStocks(any())
      verify(yahooFinance, never()).getStocks(any())
    }
  }
}