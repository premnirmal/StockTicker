package com.github.premnirmal.ticker.network

import android.content.SharedPreferences
import com.github.premnirmal.ticker.BaseUnitTest
import com.github.premnirmal.ticker.Tools
import com.github.premnirmal.ticker.mock.Mocker
import com.github.premnirmal.ticker.mock.TestApplication
import com.github.premnirmal.ticker.network.data.GStock
import com.github.premnirmal.ticker.network.data.Stock
import com.github.premnirmal.tickerwidget.BuildConfig
import com.google.gson.reflect.TypeToken
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Matchers
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import rx.Observable
import rx.observers.TestSubscriber

@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class, application = TestApplication::class)
class StocksApiTest : BaseUnitTest() {

  companion object {
    val TEST_TICKER_LIST = arrayListOf<String>("SPY", "GOOG", "MSFT", "DIA", "AAPL")
  }

  lateinit var yahooFinance: YahooFinance
  lateinit var googleFinance: GoogleFinance
  lateinit var mockPrefs: SharedPreferences

  val stocksApi = StocksApi()

  @Before fun initMocks() {
    yahooFinance = Mocker.provide(YahooFinance::class.java)
    googleFinance = Mocker.provide(GoogleFinance::class.java)
    mockPrefs = Mocker.provide(SharedPreferences::class.java)
    val yStocksJson = parseJsonFile("QuotesFromYahooFinance.json")
    val listType = object : TypeToken<List<GStock>>() {}.type
    val gStocksList = parseJsonFile<List<GStock>>(listType, "QuotesFromGoogleFinance.json")
    val yahooStocks = Observable.just(yStocksJson)
    val googleStocks = Observable.just(gStocksList)
    `when`(yahooFinance.getStocks(Matchers.anyString()))
        .thenReturn(yahooStocks)
    `when`(googleFinance.getStocks(Matchers.anyString()))
        .thenReturn(googleStocks)
  }

  @After fun clear() {
    Mocker.clearMocks()
  }

  @Test
  fun testGetStocks() {
    val testTickerList = TEST_TICKER_LIST
    val subscriber = TestSubscriber<List<Stock>>()
    stocksApi.getStocks(testTickerList).subscribe(subscriber)
    subscriber.assertNoErrors()
    val onNextEvents = subscriber.onNextEvents
    assertTrue(onNextEvents.size == 1)
    val stocks = onNextEvents[0]
    assertEquals(testTickerList.size, stocks.size)
  }

  @Test
  fun testGetStocksYahoo() {
    `when`(mockPrefs.getBoolean(eq(Tools.ENABLE_GOOGLE_FINANCE), anyBoolean())).thenReturn(false)

    val testTickerList = TEST_TICKER_LIST
    val subscriber = TestSubscriber<List<Stock>>()
    stocksApi.getStocks(testTickerList).subscribe(subscriber)

    verify(yahooFinance).getStocks(anyString())

    subscriber.assertNoErrors()
    val onNextEvents = subscriber.onNextEvents
    assertTrue(onNextEvents.size == 1)
    val stocks = onNextEvents[0]
    assertEquals(testTickerList.size, stocks.size)
  }

  @Test
  fun testGetStocksGoogle() {
    `when`(mockPrefs.getBoolean(eq(Tools.ENABLE_GOOGLE_FINANCE), anyBoolean())).thenReturn(true)

    val testTickerList = TEST_TICKER_LIST
    val subscriber = TestSubscriber<List<Stock>>()
    stocksApi.getStocks(testTickerList).subscribe(subscriber)

    verify(googleFinance).getStocks(anyString())

    subscriber.assertNoErrors()
    val onNextEvents = subscriber.onNextEvents
    assertTrue(onNextEvents.size == 1)
    val stocks = onNextEvents[0]
    assertEquals(testTickerList.size, stocks.size)
  }

}