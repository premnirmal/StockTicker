package com.github.premnirmal.ticker.network

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YahooFinanceCookies @Inject constructor() : CookieJar {

  private val _cookies = ArrayList<Cookie>()

  override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
    if (url.toString().equals("https://finance.yahoo.com/", true)) {
      _cookies.clear()
    }
    _cookies.addAll(cookies)
  }

  override fun loadForRequest(url: HttpUrl): List<Cookie> {
    return _cookies
  }
}
