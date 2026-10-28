package com.github.premnirmal.ticker.widget

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.github.premnirmal.ticker.TestApplication
import com.github.premnirmal.ticker.components.AppClock
import com.github.premnirmal.ticker.network.ChartApi
import com.github.premnirmal.ticker.network.data.Quote
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class MonthBaselineProviderTest {
    private lateinit var preferences: SharedPreferences
    private var now = Instant.parse("2026-03-02T21:00:00Z").toEpochMilli()
    private val clock = object : AppClock {
        override fun currentTimeMillis() = now
        override fun now() = kotlin.time.Instant.fromEpochMilliseconds(now)
        override fun elapsedRealtime() = now
    }
    private val clients = mutableListOf<HttpClient>()
    private val stock = Quote("A").apply { currencyCode = "USD" }

    @Before fun setup() {
        preferences = ApplicationProvider.getApplicationContext<Context>().getSharedPreferences("month-test", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
    }

    @After fun cleanup() {
        clients.forEach { it.close() }
        preferences.edit().clear().commit()
    }

    private fun provider(engine: MockEngine): MonthBaselineProvider {
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        explicitNulls = false
                    },
                )
            }
        }.also { clients.add(it) }
        return MonthBaselineProvider(ChartApi("https://example.test/v8/finance/", client), preferences, clock)
    }

    private fun history(closes: String = "[100.0,90.0,95.0]"): String {
        // Deliberately unsorted: March 2, February 26 and Friday February 27 (March 1 is Sunday).
        val times = listOf("2026-03-02T14:30:00Z", "2026-02-26T14:30:00Z", "2026-02-27T14:30:00Z")
            .joinToString(",") { Instant.parse(it).epochSecond.toString() }
        return """{"chart":{"result":[{
          "meta":{"currency":"USD","symbol":"A","regularMarketPrice":100.0,"chartPreviousClose":95.0,
          "exchangeTimezoneName":"America/New_York"},
          "timestamp":[$times],"indicators":{"quote":[{"close":$closes}]}
        }],"error":null}}"""
    }

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    @Test fun selectsLastPriorMonthCloseWithoutRequiringOhlcAndPersistsIt() = runTest {
        var calls = 0
        val engine = MockEngine { request ->
            calls++
            assertEquals("1d", request.url.parameters["interval"])
            assertEquals("3mo", request.url.parameters["range"])
            respond(history(), HttpStatusCode.OK, jsonHeaders)
        }
        val first = provider(engine)
        assertTrue(first.refresh(listOf(stock)))
        assertEquals(95.0, first.prices(listOf(stock))["A"])
        assertEquals(95.0, provider(engine).prices(listOf(stock))["A"])
        assertFalse(first.refresh(listOf(stock)))
        assertEquals(1, calls)
    }

    @Test fun manualRefreshAndExpiryRevalidateCachedHistory() = runTest {
        var calls = 0
        val subject = provider(
            MockEngine {
                calls++
                respond(history(), HttpStatusCode.OK, jsonHeaders)
            },
        )
        subject.refresh(listOf(stock))
        now += 1000
        assertTrue(subject.refresh(listOf(stock), force = true))
        now += 24 * 60 * 60 * 1000L
        assertTrue(subject.refresh(listOf(stock)))
        assertEquals(3, calls)
    }

    @Test fun missingPriorHistoryAndNullClosesAreUnavailable() = runTest {
        val subject = provider(MockEngine { respond(history("[100.0,null,null]"), HttpStatusCode.OK, jsonHeaders) })
        subject.refresh(listOf(stock))
        assertTrue(subject.prices(listOf(stock)).isEmpty())
    }

    @Test fun failureBackoffPreservesValidBaselineButNeverCrossesMonthBoundary() = runTest {
        var calls = 0
        var fail = false
        val subject = provider(
            MockEngine {
                calls++
                if (fail) {
                    respondError(HttpStatusCode.InternalServerError)
                } else {
                    respond(history(), HttpStatusCode.OK, jsonHeaders)
                }
            },
        )
        subject.refresh(listOf(stock))
        fail = true
        now += 1000
        subject.refresh(listOf(stock), force = true)
        assertEquals(95.0, subject.prices(listOf(stock))["A"])
        now += 1000
        assertFalse(subject.refresh(listOf(stock), force = true))
        assertEquals(2, calls)
        now += 15 * 60 * 1000L
        assertTrue(subject.refresh(listOf(stock)))
        assertEquals(3, calls)
        now = Instant.parse("2026-04-01T12:00:00Z").toEpochMilli()
        assertTrue(subject.prices(listOf(stock)).isEmpty())
        subject.refresh(listOf(stock))
        assertTrue(subject.prices(listOf(stock)).isEmpty())
    }

    @Test fun monthRolloverUsesExchangeTimezone() = runTest {
        now = Instant.parse("2026-03-01T00:30:00Z").toEpochMilli() // Still February in New York.
        val subject = provider(MockEngine { respond(history(), HttpStatusCode.OK, jsonHeaders) })
        subject.refresh(listOf(stock))
        assertTrue(subject.prices(listOf(stock)).isEmpty()) // No January history in this fixture.
        now = Instant.parse("2026-03-01T05:00:00Z").toEpochMilli()
        subject.refresh(listOf(stock))
        assertEquals(95.0, subject.prices(listOf(stock))["A"])
    }

    @Test fun concurrentWidgetsShareRequestsAndLimitNetworkConcurrency() = runTest {
        val calls = AtomicInteger()
        val active = AtomicInteger()
        val maximum = AtomicInteger()
        val subject = provider(
            MockEngine {
                calls.incrementAndGet()
                val count = active.incrementAndGet()
                maximum.updateAndGet { maxOf(it, count) }
                delay(100)
                active.decrementAndGet()
                respond(history(), HttpStatusCode.OK, jsonHeaders)
            },
        )
        val stocks = (1..9).map { Quote("S$it") }
        val first = async { subject.refresh(stocks, force = true) }
        val second = async { subject.refresh(stocks, force = true) }
        awaitAll(first, second)
        assertEquals(9, calls.get())
        assertTrue(maximum.get() <= 4)
    }

    @Test fun concurrentManualRefreshesShareAnInFlightRequestEvenWhenTimeAdvances() = runTest {
        var calls = 0
        val subject = provider(
            MockEngine {
                calls++
                delay(100)
                now += 1000
                respond(history(), HttpStatusCode.OK, jsonHeaders)
            },
        )
        val first = async { subject.refresh(listOf(stock), force = true) }
        val second = async {
            delay(10)
            now += 1000
            subject.refresh(listOf(stock), force = true)
        }
        awaitAll(first, second)
        assertEquals(1, calls)
    }
}
