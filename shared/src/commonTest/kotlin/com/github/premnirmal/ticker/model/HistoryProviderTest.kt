package com.github.premnirmal.ticker.model

import com.github.premnirmal.ticker.network.ChartApi
import com.github.premnirmal.ticker.network.installDefaults
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HistoryProviderTest {

    private val jsonHeaders =
        headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private fun chartJson(): String =
        """
        {"chart":{"result":[{
          "meta":{"currency":"USD","symbol":"AAPL","regularMarketPrice":12.0,"chartPreviousClose":10.0},
          "timestamp":[300,100,200],
          "indicators":{"quote":[{
            "open":[3.0,1.0,2.0],
            "high":[3.5,1.5,2.5],
            "low":[2.5,0.5,1.5],
            "close":[3.2,1.2,2.2]
          }]}
        }],"error":null}}
        """.trimIndent()

    /** A chart whose second candle has a null close so it must be filtered out. */
    private fun chartJsonWithGap(): String =
        """
        {"chart":{"result":[{
          "meta":{"currency":"USD","symbol":"AAPL","regularMarketPrice":12.0,"chartPreviousClose":10.0},
          "timestamp":[100,200],
          "indicators":{"quote":[{
            "open":[1.0,2.0],
            "high":[1.5,2.5],
            "low":[0.5,1.5],
            "close":[1.2,null]
          }]}
        }],"error":null}}
        """.trimIndent()

    private fun historyProvider(engine: MockEngine): HistoryProvider {
        val client = HttpClient(engine) { installDefaults() }
        return HistoryProvider(
            chartApi = ChartApi(baseUrl = "https://query1.finance.yahoo.com/v8/finance/", httpClient = client)
        )
    }

    @Test
    fun fetchDataByRange_mapsAndSortsDataPoints() = runTest {
        val provider = historyProvider(MockEngine { respond(chartJson(), HttpStatusCode.OK, jsonHeaders) })

        val result = provider.fetchDataByRange("AAPL", Range.ONE_DAY)

        assertTrue(result.wasSuccessful)
        val data = result.data
        assertEquals(10.0f, data.chartPreviousClose)
        assertEquals(12.0f, data.regularMarketPrice)
        assertEquals(2.0f, data.change)
        // Points are returned ordered by their timestamp regardless of input order.
        assertEquals(listOf(100f, 200f, 300f), data.dataPoints.map { it.xVal })
        assertEquals(1.2f, data.dataPoints.first().closeVal)
        assertTrue(data.isUp)
    }

    @Test
    fun fetchDataByRange_dropsPointsWithMissingValues() = runTest {
        val provider = historyProvider(MockEngine { respond(chartJsonWithGap(), HttpStatusCode.OK, jsonHeaders) })

        val result = provider.fetchDataByRange("AAPL", Range.ONE_DAY)

        assertTrue(result.wasSuccessful)
        assertEquals(listOf(100f), result.data.dataPoints.map { it.xVal })
    }

    @Test
    fun fetchDataByRange_returnsFailureOnError() = runTest {
        val provider = historyProvider(MockEngine { respondError(HttpStatusCode.InternalServerError) })

        val result = provider.fetchDataByRange("AAPL", Range.ONE_DAY)

        assertFalse(result.wasSuccessful)
    }
}
