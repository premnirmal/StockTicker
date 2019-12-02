package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.network.data.SuggestionsNet
import com.google.gson.Gson
import okhttp3.ResponseBody
import java.io.IOException
import java.util.regex.Pattern

/**
 * Created by premnirmal on 3/3/16.
 */
internal class StupidYahooWrapConverter(gson: Gson) : BaseConverter<SuggestionsNet>(gson) {

  override fun convert(value: ResponseBody?): SuggestionsNet? {
    try {
      val bodyString = value?.let { getString(it.byteStream()) }
      val m = PATTERN_RESPONSE.matcher(bodyString)
      if (m.find()) {
        return gson.fromJson(m.group(1), SuggestionsNet::class.java)
      }
      throw error("Invalid response")
    } catch (e: IOException) {
      return null
    }
  }

  companion object {

    private val PATTERN_RESPONSE =
      Pattern.compile("YAHOO\\.Finance\\.SymbolSuggest\\.ssCallback\\((\\{.*?\\})\\)")
  }
}