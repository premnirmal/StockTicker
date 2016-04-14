package com.github.premnirmal.ticker.network

import android.content.Context
import com.github.premnirmal.ticker.RxBus
import com.github.premnirmal.ticker.Tools
import com.github.premnirmal.ticker.events.NoNetworkEvent
import com.squareup.okhttp.CacheControl
import com.squareup.okhttp.Interceptor
import com.squareup.okhttp.Request
import com.squareup.okhttp.Response
import java.io.IOException

/**
 * Created on 3/17/16.
 */
internal class NoCacheRequestInterceptor(val context: Context, val bus: RxBus) : Interceptor {

  @Throws(IOException::class)
  override fun intercept(chain: Interceptor.Chain): Response? {
    if (!Tools.isNetworkOnline(context)) {
      bus.post(NoNetworkEvent())
      throw IOException("No network connection")
    } else {
      val request: Request = chain.request().newBuilder()
          .cacheControl(CacheControl.FORCE_NETWORK)
          .build()
      return chain.proceed(request)
    }
  }

}