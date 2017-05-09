package com.github.premnirmal.ticker.network

import android.content.SharedPreferences
import com.github.premnirmal.ticker.BaseUnitTest
import com.github.premnirmal.ticker.mock.Mocker
import com.github.premnirmal.ticker.mock.TestApplication
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.network.data.QuoteNet
import com.github.premnirmal.tickerwidget.BuildConfig
import com.google.gson.reflect.TypeToken
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Matchers
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import io.reactivex.Observable
import io.reactivex.observers.TestSubscriber

@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class, application = TestApplication::class)
class StocksApiTest : BaseUnitTest() {

  companion object {
    val TEST_TICKER_LIST = arrayListOf<String>("SPY", "GOOG", "MSFT", "DIA", "AAPL")
  }

  lateinit var yahooFinance: YahooFinance
  lateinit var robinhood: Robindahood
  lateinit var mockPrefs: SharedPreferences

  val stocksApi = StocksApi()

  @Before fun initMocks() {
    yahooFinance = Mocker.provide(YahooFinance::class.java)
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
    val subscriber = TestSubscriber<List<Quote>>()
    stocksApi.getStocks(testTickerList).subscribe(subscriber)
    subscriber.assertNoErrors()
    verify(robinhood).getStocks(anyString())
    val onNextEvents = subscriber.onNextEvents
    assertTrue(onNextEvents.size == 1)
    val stocks = onNextEvents[0]
    assertEquals(testTickerList.size, stocks.size)
  }

}