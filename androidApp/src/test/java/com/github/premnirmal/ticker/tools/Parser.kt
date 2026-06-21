package com.github.premnirmal.ticker.tools

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

class Parser {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
        coerceInputValues = true
        prettyPrint = true
    }

    inline fun <reified T> parseJsonFileType(fileName: String): T {
        val jsonFile = parseJsonFile(fileName)
        return json.decodeFromJsonElement(jsonFile)
    }

    fun parseJsonFile(resourceName: String): JsonElement {
        val jsonElement: JsonElement
        try {
            val `in` = javaClass.classLoader!!.getResourceAsStream(resourceName) ?: throw AssertionError(
                "Failed loading resource " + resourceName + " from " + Parser::class.java
            )
            val jsonString = readInput(`in`)
            jsonElement = json.parseToJsonElement(jsonString)
        } catch (ioe: IOException) {
            throw RuntimeException("Parse failed", ioe)
        }

        return jsonElement
    }

    @Throws(IOException::class)
    private fun readInput(`in`: InputStream): String {
        val baos = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        var length = `in`.read(buffer)
        while (length != -1) {
            baos.write(buffer, 0, length)
            length = `in`.read(buffer)
        }
        `in`.close()
        return baos.toString()
    }
}
