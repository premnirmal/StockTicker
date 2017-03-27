package com.github.premnirmal.ticker.network

import android.content.Context
import com.github.premnirmal.ticker.RxBus
import com.github.premnirmal.ticker.Tools
import com.github.premnirmal.ticker.events.NoNetworkEvent
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Interceptor.Chain
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

/**
 * Created by premnirmal on 3/17/16.
 */
internal class NoCacheRequestInterceptor(val context: Context, val bus: RxBus) : Interceptor {

  @Throws(IOException::class)
  override fun intercept(chain: Chain): Response? {
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