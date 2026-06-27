package com.github.premnirmal.ticker.detail

import android.content.Context
import com.github.premnirmal.ticker.format
import com.github.premnirmal.ticker.formatBigNumbers
import com.github.premnirmal.ticker.formatDate
import com.github.premnirmal.ticker.news.QuoteDetailViewModel.QuoteWithSummary
import com.github.premnirmal.tickerwidget.R

/**
 * Builds the localised list of [QuoteDetailItem] rows for the quote-detail screen from a fetched
 * [QuoteWithSummary]. Kept in `:app` (rather than the shared [QuoteDetailViewModel]) because both the
 * label and the formatted [QuoteDetailItem.data] depend on Android string resources and
 * `Context`-based number/date formatting; the `@StringRes` titles are resolved to plain [String]s
 * here so the shared screen renders a fully-resolved [QuoteDetailItem].
 */
fun buildQuoteDetails(summary: QuoteWithSummary, context: Context): List<QuoteDetailItem> {
    val quote = summary.quote
    val quoteSummary = summary.quoteSummary
    val details = mutableListOf<QuoteDetailItem>()
    fun add(titleRes: Int, data: String?) {
        if (data != null) {
            details.add(QuoteDetailItem(context.getString(titleRes), data))
        }
    }
    add(R.string.quote_details_open, quote.open?.let { quote.priceFormat.format(it) })
    if (quote.dayLow != null && quote.dayHigh != null) {
        add(R.string.quote_details_day_range, "${quote.dayLow!!.format()} - ${quote.dayHigh!!.format()}")
    }
    add(R.string.quote_details_fifty_day_average, quote.fiftyDayAverage?.takeIf { it > 0f }?.format())
    add(
        R.string.quote_details_two_hundred_day_average,
        quote.twoHundredDayAverage?.takeIf { it > 0f }?.format()
    )
    if (quote.fiftyTwoWeekLow != null && quote.fiftyTwoWeekHigh != null) {
        add(
            R.string.quote_details_ftw_range,
            "${quote.fiftyTwoWeekLow!!.format()} - ${quote.fiftyTwoWeekHigh!!.format()}"
        )
    }
    add(R.string.quote_details_volume, quote.regularMarketVolume?.format())
    add(R.string.quote_details_market_cap, quote.marketCap?.formatBigNumbers(context))
    add(R.string.quote_details_pe_ratio, quote.trailingPE?.format())
    add(
        R.string.quote_details_earnings_date,
        quote.earningsTimestamp?.formatDate(context.getString(R.string.date_format_long))
    )
    if (quote.annualDividendRate > 0f && quote.annualDividendYield > 0f) {
        add(R.string.quote_details_dividend_rate, quote.dividendInfo())
    }
    add(
        R.string.quote_details_dividend_date,
        quote.dividendDate?.formatDate(context.getString(R.string.date_format_long))
    )
    add(R.string.quote_details_earnings_growth, quoteSummary?.financialData?.earningsGrowth?.fmt)
    add(R.string.quote_details_revenue_growth, quoteSummary?.financialData?.revenueGrowth?.fmt)
    add(R.string.quote_details_profit_margins, quoteSummary?.financialData?.profitMargins?.fmt)
    add(R.string.quote_details_gross_margins, quoteSummary?.financialData?.grossMargins?.fmt)
    return details
}
