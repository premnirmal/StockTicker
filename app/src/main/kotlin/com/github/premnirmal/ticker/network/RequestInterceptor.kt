package com.github.premnirmal.ticker.network

import android.content.Context
import com.github.premnirmal.ticker.StocksApp
import com.github.premnirmal.ticker.components.Injector
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Interceptor.Chain
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject

/**
 * Created by premnirmal on 3/17/16.
 */
class RequestInterceptor : Interceptor {

  companion object {
    val PACKAGE_HEADER = "XPackage-StockTicker"
    val SIGNATURE_HEADER = "XSignature-StockTicker"
  }

  @Inject internal lateinit var context: Context

  init {
    Injector.appComponent.inject(this)
  }

  @Throws(IOException::class)
  override fun intercept(chain: Chain): Response? {
    // Disable cache so we always have fresh quotes.
    val request: Request = chain.request().newBuilder()
        .cacheControl(CacheControl.FORCE_NETWORK)
        .addHeader(PACKAGE_HEADER, context.packageName)
        .addHeader(SIGNATURE_HEADER, StocksApp.SIGNATURE)
        .build()
    return chain.proceed(request)
  }
}