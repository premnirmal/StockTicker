package com.github.premnirmal.ticker.network

import android.content.Context
import android.content.pm.PackageManager
import android.util.Base64
import com.github.premnirmal.ticker.components.Injector
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Interceptor.Chain
import okhttp3.Request
import okhttp3.Response
import timber.log.Timber
import java.io.IOException
import java.security.MessageDigest
import javax.inject.Inject

/**
 * Created by premnirmal on 3/17/16.
 */
class RequestInterceptor : Interceptor {

  companion object {
    private const val PACKAGE_HEADER = "XPackage-StockTicker"
    private const val SIGNATURE_HEADER = "XSignature-StockTicker"
  }

  @Inject internal lateinit var context: Context
  private val signature: String? by lazy {
    getAppSignature()
  }

  init {
    Injector.appComponent.inject(this)
  }

  @Throws(IOException::class) override fun intercept(chain: Chain): Response? {
    // Disable cache so we always have fresh quotes.
    val request: Request = chain.request()
        .newBuilder()
        .cacheControl(CacheControl.FORCE_NETWORK)
        .addHeader(PACKAGE_HEADER, context.packageName)
        .addHeader(SIGNATURE_HEADER, signature)
        .build()
    return chain.proceed(request)
  }

  private fun getAppSignature(): String? {
    try {
      val packageInfo =
        context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
      packageInfo.signatures.firstOrNull {
        val md = MessageDigest.getInstance("SHA")
        md.update(it.toByteArray())
        return Base64.encodeToString(md.digest(), Base64.DEFAULT)
            .trim()
      }
    } catch (e: Exception) {
      Timber.e(e)
    }
    return null
  }
}