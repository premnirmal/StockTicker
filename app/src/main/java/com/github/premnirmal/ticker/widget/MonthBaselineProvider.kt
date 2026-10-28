package com.github.premnirmal.ticker.widget

import android.content.SharedPreferences
import androidx.core.content.edit
import com.github.premnirmal.ticker.components.AppClock
import com.github.premnirmal.ticker.network.ChartApi
import com.github.premnirmal.ticker.network.data.Quote
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/** Shared by all Android widgets. Never performs network I/O from a composable or a state update. */
class MonthBaselineProvider(
    private val api: ChartApi,
    private val preferences: SharedPreferences,
    private val clock: AppClock,
) {
    private val locks = ConcurrentHashMap<String, Mutex>()
    private val generations = ConcurrentHashMap<String, Long>()
    private val requests = Semaphore(4)
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    internal data class Baseline(
        val month: String,
        val zone: String,
        val currency: String,
        val price: Double? = null,
        val closeDate: String? = null,
        val fetchedAt: Long = 0,
        val attemptedAt: Long = 0,
        val failed: Boolean = false,
    )

    fun prices(quotes: List<Quote>): Map<String, Double> = quotes.mapNotNull { quote ->
        val cached = read(quote.symbol) ?: return@mapNotNull null
        val zone = zone(cached.zone, quote)
        cached.price?.takeIf {
            it.isFinite() && it > 0 && cached.month == month(zone) && cached.currency == quote.currencyCode
        }?.let { quote.symbol to it }
    }.toMap()

    suspend fun refresh(quotes: List<Quote>, force: Boolean = false): Boolean = coroutineScope {
        quotes.distinctBy { it.symbol }.map { quote -> async { refreshSymbol(quote, force) } }
            .awaitAll().any { it }
    }

    private suspend fun refreshSymbol(quote: Quote, force: Boolean): Boolean {
        val generation = generations[quote.symbol] ?: 0L
        return locks.getOrPut(quote.symbol) { Mutex() }.withLock {
            // A request queued behind an in-flight fetch shares that fetch, including manual refreshes.
            if ((generations[quote.symbol] ?: 0L) != generation) return@withLock false
            val cached = read(quote.symbol)
            val localZone = zone(cached?.zone, quote)
            val now = clock.currentTimeMillis()
            val currentMonth = month(localZone)
            val sameMonth = cached?.month == currentMonth && cached.currency == quote.currencyCode
            if (sameMonth) {
                val age = now - cached.attemptedAt
                if (cached.failed && age in 0 until RETRY_MS) return@withLock false
                if (!cached.failed && !force && now - cached.fetchedAt in 0 until CACHE_MS) return@withLock false
            }
            try {
                val data = requests.withPermit { api.fetchChartData(quote.symbol, "1d", "3mo") }
                check(data.chart.error == null) { "History response contains an error" }
                val result = data.chart.result.first()
                check(result.meta.currency == quote.currencyCode) { "History currency does not match the holding" }
                val exchangeZone = zone(result.meta.exchangeTimezoneName, quote)
                val firstDay = YearMonth.from(Instant.ofEpochMilli(now).atZone(exchangeZone)).atDay(1)
                val closes = result.indicators?.quote?.firstOrNull()?.close.orEmpty()
                val baseline = result.timestamp.orEmpty().mapIndexedNotNull { index, timestamp ->
                    val close = closes.getOrNull(index)
                    val date = Instant.ofEpochSecond(timestamp).atZone(exchangeZone).toLocalDate()
                    if (date < firstDay && close != null && close.isFinite() && close > 0) {
                        Triple(timestamp, date, close)
                    } else {
                        null
                    }
                }.maxByOrNull { it.first }
                write(
                    quote.symbol,
                    Baseline(
                        month = YearMonth.from(firstDay).toString(),
                        zone = exchangeZone.id,
                        currency = result.meta.currency,
                        price = baseline?.third,
                        closeDate = baseline?.second?.toString(),
                        fetchedAt = now,
                        attemptedAt = now,
                    ),
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Timber.w(error, "Month baseline unavailable for %s", quote.symbol)
                val previous = if (sameMonth) cached else null
                write(
                    quote.symbol,
                    Baseline(
                        month = currentMonth,
                        zone = localZone.id,
                        currency = quote.currencyCode,
                        price = previous?.price,
                        closeDate = previous?.closeDate,
                        fetchedAt = previous?.fetchedAt ?: 0,
                        attemptedAt = now,
                        failed = true,
                    ),
                )
            }
            generations[quote.symbol] = generation + 1
            true
        }
    }

    private fun month(zone: ZoneId): String =
        YearMonth.from(Instant.ofEpochMilli(clock.currentTimeMillis()).atZone(zone)).toString()

    private fun zone(name: String?, quote: Quote): ZoneId = try {
        if (name.isNullOrBlank()) {
            ZoneOffset.ofTotalSeconds((quote.gmtOffSetMilliseconds / 1000).toInt())
        } else {
            ZoneId.of(name)
        }
    } catch (_: Exception) {
        ZoneOffset.UTC
    }

    private fun read(symbol: String): Baseline? = try {
        preferences.getString(symbol, null)?.let { json.decodeFromString<Baseline>(it) }
    } catch (_: Exception) {
        null
    }

    private fun write(symbol: String, baseline: Baseline) {
        preferences.edit { putString(symbol, json.encodeToString(Baseline.serializer(), baseline)) }
    }

    companion object {
        private val CACHE_MS = TimeUnit.HOURS.toMillis(24)
        private val RETRY_MS = TimeUnit.MINUTES.toMillis(15)
    }
}
