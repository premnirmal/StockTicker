package com.github.premnirmal.ticker.network

import android.os.Build
import okhttp3.Interceptor
import okhttp3.Interceptor.Chain
import okhttp3.Response
import com.github.premnirmal.tickerwidget.BuildConfig
import java.io.IOException

internal class UserAgentInterceptor : Interceptor {

  val HEADER_USER_AGENT = "UserAgent"
  val USER_AGENT = "com.github.premnirmal.tickerwidget/%s (Android; %s; %s; %s)"

  @Throws(IOException::class)
  override fun intercept(chain: Chain): Response {
    val userAgent = String.format(USER_AGENT, BuildConfig.VERSION_CODE, Build.MODEL,
        Build.VERSION.SDK_INT, Build.VERSION.RELEASE)
    val newRequest = chain.request().newBuilder()
        .addHeader(HEADER_USER_AGENT, userAgent)
        .build()
    return chain.proceed(newRequest)
  }

}