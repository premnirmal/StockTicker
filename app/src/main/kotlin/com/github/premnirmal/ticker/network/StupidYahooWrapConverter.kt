package com.github.premnirmal.ticker.network

import com.google.gson.Gson
import okhttp3.ResponseBody
import java.io.IOException
import java.lang.reflect.Type
import java.util.regex.Pattern

/**
 * Created on 3/3/16.
 */
internal class StupidYahooWrapConverter(gson: Gson) : BaseConverter<Suggestions>(gson) {

  override fun convert(value: ResponseBody?): Suggestions? {
    try {
      val bodyString = getString(value!!.byteStream())
      val m = PATTERN_RESPONSE.matcher(bodyString)
      if (m.find()) {
        val suggestions = gson.fromJson(m.group(1), Suggestions::class.java)
        return suggestions
      }
      throw error("Invalid response")
    } catch (e: IOException) {
      e.printStackTrace()
      return null
    }
  }

  companion object {

    private val PATTERN_RESPONSE = Pattern.compile(
        "YAHOO\\.Finance\\.SymbolSuggest\\.ssCallback\\((\\{.*?\\})\\)")
  }
}