package com.github.premnirmal.ticker.tools

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.lang.reflect.Type

class Parser {

  val parser = JsonParser()
  val gson = Gson()

  fun <T> parseJsonFile(type: Type, fileName: String): T {
    val json = parseJsonFile(fileName)
    return gson.fromJson(json, type)
  }

  fun parseJsonFile(resourceName: String): JsonElement {
    val jsonElement: JsonElement
    try {
      val `in` = javaClass.classLoader!!.getResourceAsStream(resourceName) ?: throw AssertionError(
          "Failed loading resource " + resourceName + " from " + Parser::class.java)
      val jsonString = readInput(`in`)
      jsonElement = convertStringToJson(jsonString)
    } catch (ioe: IOException) {
      throw RuntimeException("Parse failed", ioe)
    }

    return jsonElement
  }

  @Throws(IOException::class) private fun readInput(`in`: InputStream): String {
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

  @Throws(IOException::class) private fun convertStringToJson(jsonString: String): JsonElement {
    return parser.parse(jsonString)
  }
}