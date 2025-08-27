package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.network.data.YahooResponse
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

interface YahooFinance {

    /**
     * Retrieves a list of stock quotes.
     *
     * @param query comma separated list of symbols.
     *
     * @return A List of quotes.
     */
    @GET(
        "v7/finance/quote?format=json"
    )
    suspend fun getStocks(@Query(value = "symbols") query: String): Response<YahooResponse>
}
interface YahooFinanceInitialLoad {
    @GET("/")
    suspend fun initialLoad(): Response<String?>

    @POST
    suspend fun cookieConsent(@Url url: String?, @Body body: RequestBody): Response<String?>
}
interface YahooFinanceCrumb {

    @GET(
        "v1/test/getcrumb"
    )
    suspend fun getCrumb(): Response<String>
}
