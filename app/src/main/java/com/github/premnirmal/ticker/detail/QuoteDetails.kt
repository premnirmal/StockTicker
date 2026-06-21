package com.github.premnirmal.ticker.detail

import android.content.Context
import androidx.annotation.StringRes
import com.github.premnirmal.ticker.format
import com.github.premnirmal.ticker.formatBigNumbers
import com.github.premnirmal.ticker.formatDate
import com.github.premnirmal.ticker.news.QuoteDetailViewModel.QuoteWithSummary
import com.github.premnirmal.tickerwidget.R

/**
 * A single localised key/value row shown in the quote-detail grid.
 *
 * This stays in `:app` (rather than the shared [com.github.premnirmal.ticker.news.QuoteDetailViewModel])
 * because both the label ([title]) and the formatted [data] depend on Android string resources and
 * `Context`-based number/date formatting.
 */
data class QuoteDetail(
    @StringRes
    val title: Int,
    val data: String
)

/**
 * Builds the localised list of [QuoteDetail] rows for the quote-detail screen from a fetched
 * [QuoteWithSummary]. Hoisted out of the shared ViewModel so the Android string-resource and
 * `Context`-based formatting stay in `:app`.
 */
fun buildQuoteDetails(summary: QuoteWithSummary, context: Context): List<QuoteDetail> {
    val quote = summary.quote
    val quoteSummary = summary.quoteSummary
    val details = mutableListOf<QuoteDetail>()
    quote.open?.let {
        details.add(
            QuoteDetail(
                R.string.quote_details_open,
                quote.priceFormat.format(it)
            )
        )
    }
    if (quote.dayLow != null && quote.dayHigh != null) {
        details.add(
            QuoteDetail(
                R.string.quote_details_day_range,
                "${quote.dayLow!!.format()} - ${quote.dayHigh!!.format()}"
            )
        )
    }
    quote.fiftyDayAverage?.let {
        if (it > 0f) {
            details.add(
                QuoteDetail(
                    R.string.quote_details_fifty_day_average,
                    it.format()
                )
            )
        }
    }
    quote.twoHundredDayAverage?.let {
        if (it > 0f) {
            details.add(
                QuoteDetail(
                    R.string.quote_details_two_hundred_day_average,
                    it.format()
                )
            )
        }
    }
    if (quote.fiftyTwoWeekLow != null && quote.fiftyTwoWeekHigh != null) {
        details.add(
            QuoteDetail(
                R.string.quote_details_ftw_range,
                "${quote.fiftyTwoWeekLow!!.format()} - ${quote.fiftyTwoWeekHigh!!.format()}"
            )
        )
    }
    quote.regularMarketVolume?.let {
        details.add(
            QuoteDetail(
                R.string.quote_details_volume,
                it.format()
            )
        )
    }
    quote.marketCap?.let {
        details.add(
            QuoteDetail(
                R.string.quote_details_market_cap,
                it.formatBigNumbers(context)
            )
        )
    }
    quote.trailingPE?.let {
        details.add(
            QuoteDetail(
                R.string.quote_details_pe_ratio,
                it.format()
            )
        )
    }
    quote.earningsTimestamp?.let {
        details.add(
            QuoteDetail(
                R.string.quote_details_earnings_date,
                it.formatDate(context.getString(R.string.date_format_long))
            )
        )
    }
    if (quote.annualDividendRate > 0f && quote.annualDividendYield > 0f) {
        details.add(
            QuoteDetail(
                R.string.quote_details_dividend_rate,
                quote.dividendInfo()
            )
        )
    }
    quote.dividendDate?.let {
        details.add(
            QuoteDetail(
                R.string.quote_details_dividend_date,
                it.formatDate(context.getString(R.string.date_format_long))
            )
        )
    }
    quoteSummary?.financialData?.earningsGrowth?.fmt?.let {
        details.add(
            QuoteDetail(
                R.string.quote_details_earnings_growth,
                it
            )
        )
    }
    quoteSummary?.financialData?.revenueGrowth?.fmt?.let {
        details.add(
            QuoteDetail(
                R.string.quote_details_revenue_growth,
                it
            )
        )
    }
    quoteSummary?.financialData?.profitMargins?.fmt?.let {
        details.add(
            QuoteDetail(
                R.string.quote_details_profit_margins,
                it
            )
        )
    }
    quoteSummary?.financialData?.grossMargins?.fmt?.let {
        details.add(
            QuoteDetail(
                R.string.quote_details_gross_margins,
                it
            )
        )
    }
    return details
}
