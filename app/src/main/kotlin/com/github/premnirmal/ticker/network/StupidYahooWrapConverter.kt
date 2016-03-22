package com.github.premnirmal.ticker.network

import retrofit.converter.ConversionException
import retrofit.mime.TypedInput
import java.io.IOException
import java.lang.reflect.Type
import java.util.regex.Pattern

/**
 * Created on 3/3/16.
 */
internal class StupidYahooWrapConverter : BaseConverter() {

    @Throws(ConversionException::class)
    override fun fromBody(body: TypedInput, type: Type): Any? {
        try {
            val bodyString = getString(body.`in`())
            val m = PATTERN_RESPONSE.matcher(bodyString)
            if (m.find()) {
                val suggestions = gson.fromJson(m.group(1), Suggestions::class.java)
                return suggestions
            }
            throw ConversionException("Invalid response")
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }

    }

    companion object {

        private val PATTERN_RESPONSE = Pattern.compile("YAHOO\\.Finance\\.SymbolSuggest\\.ssCallback\\((\\{.*?\\})\\)")
    }
}