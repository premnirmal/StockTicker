package com.github.premnirmal.ticker.network

import android.content.SharedPreferences
import com.github.premnirmal.ticker.BaseUnitTest
import com.github.premnirmal.ticker.mock.Mocker
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.network.data.QuoteNet
import com.google.gson.reflect.TypeToken
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Matchers
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify

class StocksApiTest : BaseUnitTest() {

  companion object {
    val TEST_TICKER_LIST = arrayListOf<String>("SPY", "GOOG", "MSFT", "DIA", "AAPL")
  }

  lateinit var robinhood: Robindahood
  lateinit var mockPrefs: SharedPreferences

  val stocksApi = StocksApi()

  @Before fun initMocks() {
    robinhood = Mocker.provide(Robindahood::class.java)
    mockPrefs = Mocker.provide(SharedPreferences::class.java)
    val listType = object : TypeToken<List<QuoteNet>>() {}.type
    val stockList = parseJsonFile<List<QuoteNet>>(listType, "Quotes.json")
    val stocks = Observable.just(stockList)
    `when`(robinhood.getStocks(Matchers.anyString()))
        .thenReturn(stocks)
  }

  @After fun clear() {
    Mocker.clearMocks()
  }

  @Test
  fun testGetStocks() {
    val testTickerList = TEST_TICKER_LIST
    val subscriber = TestObserver<List<Quote>>()
    stocksApi.getStocks(testTickerList).subscribe(subscriber)
    subscriber.assertNoErrors()
    verify(robinhood).getStocks(anyString())
    val onNextEvents = subscriber.events[0]
    assertTrue(onNextEvents.size == 1)
    val stocks = onNextEvents[0] as List<*>
    assertEquals(testTickerList.size, stocks.size)
  }

}