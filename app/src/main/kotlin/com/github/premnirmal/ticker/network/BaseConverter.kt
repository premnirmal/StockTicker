package com.github.premnirmal.ticker.network

import com.google.gson.GsonBuilder
import retrofit.converter.GsonConverter
import retrofit.mime.TypedOutput
import java.io.IOException
import java.io.InputStream

/**
 * Created on 3/3/16.
 */
internal abstract class BaseConverter : retrofit.converter.Converter {

  protected val gson = GsonBuilder().create()

  @Throws(IOException::class)
  protected fun getString(`is`: InputStream): String {
    var ch: Int
    val sb = StringBuilder()
    do {
      ch = `is`.read()
      sb.append(ch.toChar())
    } while (ch != -1)
    return sb.toString()
  }

  override fun toBody(`object`: Any): TypedOutput {
    return GsonConverter(gson).toBody(`object`)
  }
}