package com.github.premnirmal.ticker.portfolio

import com.github.premnirmal.ticker.network.data.Quote
import kotlinx.serialization.Serializable

/** Values for one currency, with unavailable metrics represented explicitly rather than as zero. */
@Serializable
data class PortfolioSummary(
    val currency: String,
    val holdings: Double?,
    val gain: Double?,
    val gainPercent: Double?,
    val dayChangePercent: Double?,
    val monthChangePercent: Double?,
)

/** Price performance of today's positions, excluding cash flows, dividends and realised gains. */
object PortfolioSummaryCalculator {
    fun calculate(quotes: List<Quote>, monthCloses: Map<String, Double> = emptyMap()): List<PortfolioSummary> =
        quotes.distinctBy { it.symbol }
            .filter { quote ->
                val lots = quote.position?.holdings.orEmpty()
                lots.any { !it.shares.isFinite() || it.shares < 0f } || lots.sumOf { it.shares.toDouble() } != 0.0
            }
            .groupBy { it.currencyCode.ifBlank { "?" } }
            .toList().sortedBy { it.first }
            .map { (currency, stocks) -> summarize(currency, stocks, monthCloses) }

    private fun summarize(currency: String, stocks: List<Quote>, monthCloses: Map<String, Double>): PortfolioSummary {
        val quantities = stocks.map { quote ->
            quote.position!!.holdings.takeIf { lots -> lots.all { it.shares.isFinite() && it.shares >= 0f } }
                ?.sumOf { it.shares.toDouble() }
        }
        fun value(prices: List<Double?>): Double? {
            if (quantities.any { it == null || !it.isFinite() } || prices.any { it == null || !it.isFinite() || it <= 0 }) {
                return null
            }
            return stocks.indices.sumOf { quantities[it]!! * prices[it]!! }.takeIf { it.isFinite() }
        }
        val current = value(stocks.map { it.lastTradePrice.toDouble() })
        val previous = value(stocks.map { it.previousClose.toDouble() })
        val month = value(stocks.map { monthCloses[it.symbol] })
        val lots = stocks.flatMap { it.position!!.holdings }.filter { it.shares != 0f }
        val cost = if (quantities.any { it == null } || lots.any { !it.price.isFinite() || it.price < 0f }) {
            null
        } else {
            lots.sumOf { it.shares.toDouble() * it.price.toDouble() }.takeIf { it.isFinite() }
        }
        val gain = if (current != null && cost != null) (current - cost).takeIf { it.isFinite() } else null
        return PortfolioSummary(
            currency = currency,
            holdings = current,
            gain = gain,
            gainPercent = percentage(current, cost),
            dayChangePercent = percentage(current, previous),
            monthChangePercent = percentage(current, month),
        )
    }

    private fun percentage(current: Double?, reference: Double?): Double? =
        if (current == null || reference == null || reference <= 0) null
        else ((current / reference - 1) * 100).takeIf { it.isFinite() }
}
