package com.github.premnirmal.ticker.network

import com.google.gson.reflect.TypeToken
import retrofit.converter.ConversionException
import retrofit.mime.TypedInput
import java.io.IOException
import java.lang.reflect.Type
import java.util.*

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
                responseString = bodyString.substring(2, bodyString.length)
            } else {
                responseString = bodyString
            }
            val collectionType = object : TypeToken<List<GStock>>() {

            }.type
            val stocks = gson.fromJson<List<GStock>>(responseString, collectionType)
            return stocks
        } catch (e: IOException) {
            e.printStackTrace()
            return ArrayList<GStock>()
        }

    }
}