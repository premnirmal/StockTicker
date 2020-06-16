package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.network.data.SuggestionsNet
import com.google.gson.Gson
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.lang.reflect.Type
import java.util.regex.Pattern

/**
 * Created by premnirmal on 3/3/16.
 */
private class StupidYahooWrapConverter(gson: Gson) : BaseConverter<SuggestionsNet>(gson) {

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

internal class YahooSuggestionsConverterFactory(private val gson: Gson,
                                                private val gsonConverterFactory: GsonConverterFactory) :
    Converter.Factory() {
  override fun responseBodyConverter(
      type: Type?,
      annotations: Array<out Annotation>?,
      retrofit: Retrofit?
  ): Converter<ResponseBody, *> {
    return StupidYahooWrapConverter(gson)
  }

  override fun requestBodyConverter(
      type: Type,
      parameterAnnotations: Array<out Annotation>,
      methodAnnotations: Array<out Annotation>,
      retrofit: Retrofit
  ): Converter<*, RequestBody>? {
    return gsonConverterFactory.requestBodyConverter(
        type, parameterAnnotations,
        methodAnnotations, retrofit
    )
  }

  override fun stringConverter(
      type: Type,
      annotations: Array<out Annotation>,
      retrofit: Retrofit
  ): Converter<*, String>? {
    return gsonConverterFactory.stringConverter(type, annotations, retrofit)
  }
}