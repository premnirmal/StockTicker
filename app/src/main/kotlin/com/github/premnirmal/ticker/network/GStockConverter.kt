package com.github.premnirmal.ticker.network

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.ResponseBody
import java.io.IOException
import java.lang.reflect.Type
import java.util.ArrayList

/**
 * Created on 3/3/16.
 */
internal class GStockConverter(gson: Gson) : BaseConverter<List<GStock>>(gson) {

  override fun convert(value: ResponseBody?): List<GStock> {
    try {
      val bodyString = getString(value!!.byteStream()).replace("\n".toRegex(), "")
      val responseString: String
      if (bodyString.startsWith("//")) {
        responseString = bodyString.substring(bodyString.indexOf('['),
            bodyString.lastIndexOf(']') + 1)
      } else {
        responseString = bodyString
      }
      val type = object : TypeToken<ArrayList<GStock>>() {}.type
      val stocks = gson.fromJson<List<GStock>>(responseString, type)
      return stocks
    } catch (e: IOException) {
      e.printStackTrace()
      throw error(e)
    }
  }
}