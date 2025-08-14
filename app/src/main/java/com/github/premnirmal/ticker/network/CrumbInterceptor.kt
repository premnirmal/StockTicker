package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.AppPreferences
import okhttp3.Interceptor
import okhttp3.Interceptor.Chain
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CrumbInterceptor @Inject constructor(
    private val appPreferences: AppPreferences
) : Interceptor {

    override fun intercept(chain: Chain): Response {
        val crumb = appPreferences.getCrumb()
        val builder = chain.request().newBuilder()
        builder
            .removeHeader("Accept")
            .addHeader(
                "Accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7"
            )
        if (!crumb.isNullOrEmpty()) {
            builder
                .url(chain.request().url.newBuilder().addQueryParameter("crumb", crumb).build())
        }
        return chain.proceed(
            builder.build()
        )
    }
}
