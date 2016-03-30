package com.github.premnirmal.ticker.network

import com.squareup.okhttp.CacheControl
import com.squareup.okhttp.Interceptor
import com.squareup.okhttp.Request
import com.squareup.okhttp.Response

/**
 * Created on 3/17/16.
 */
internal class NoCacheRequestInterceptor : Interceptor {

  override fun intercept(chain: Interceptor.Chain): Response? {
    val request: Request = chain.request().newBuilder()
        .cacheControl(CacheControl.FORCE_NETWORK)
        .build()
    return chain.proceed(request)
  }

}