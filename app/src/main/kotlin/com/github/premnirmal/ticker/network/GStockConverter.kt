package com.github.premnirmal.ticker.network

import retrofit.converter.ConversionException
import retrofit.mime.TypedInput
import java.io.IOException
import java.lang.reflect.Type

/**
 * Created on 3/3/16.
 */
internal class GStockConverter : BaseConverter() {

    @Throws(ConversionException::class)
    override fun fromBody(body: TypedInput, type: Type): Any {
        try {
            val bodyString = getString(body.`in`()).replace("\n".toRegex(), "")
            val responseString: String
            if (bodyString.startsWith("//")) {
                responseString = bodyString.substring(bodyString.indexOf('['), bodyString.lastIndexOf(']') + 1)
            } else {
                responseString = bodyString
            }
            val stocks = gson.fromJson<List<GStock>>(responseString, type)
            return stocks
        } catch (e: IOException) {
            e.printStackTrace()
            throw ConversionException(e)
        }

    }
}