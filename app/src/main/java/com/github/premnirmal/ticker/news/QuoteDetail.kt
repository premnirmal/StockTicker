package com.github.premnirmal.ticker.news

import android.content.Context
import androidx.annotation.StringRes
import com.github.premnirmal.ticker.format
import com.github.premnirmal.ticker.formatBigNumbers
import com.github.premnirmal.ticker.formatDate
import com.github.premnirmal.ticker.news.QuoteDetailViewModel.QuoteWithSummary
import com.github.premnirmal.tickerwidget.R

/**
 * A single row of the quote-detail grid: a string-resource label plus a pre-formatted value.
 *
 * This stays in `:app` (rather than the shared [QuoteDetailViewModel]) because its values mix
 * translated date patterns ([R.string.date_format_long]) and Android-resource number formatting
 * ([formatBigNumbers]); it is derived from the shared ViewModel's quote flow on Android.
 */
data class QuoteDetail(
    @StringRes
    val title: Int,

    val data: String
)

/**
 * Builds the Android string-resource-formatted [QuoteDetail] grid from a fetched quote/summary,
 * mirroring the logic that used to live in `QuoteDetailViewModel.details`.
 */
fun QuoteWithSummary.toQuoteDetails(context: Context): List<QuoteDetail> {
    val quote = quote
    val quoteSummary = quoteSummary
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
