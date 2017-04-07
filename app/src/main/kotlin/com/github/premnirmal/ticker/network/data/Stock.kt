package com.github.premnirmal.ticker.network.data

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * Created by premnirmal on 3/30/17.
 */
class Stock : Comparable<Stock>, Serializable {

  companion object {
    private val serialVersionUID = -4235355L

    const val GDAXI_TICKER = "^GDAXI"
    const val GSPC_TICKER = "^GSPC"
    const val XAU_TICKER = "XAU"
  }

  @SerializedName("Symbol") var symbol: String = ""
  @SerializedName("Name") var name = ""
  @SerializedName("LastTradePriceOnly") var lastTradePrice: Float = 0.toFloat()
  @SerializedName("ChangeinPercent") var changeinPercent = ""
  @SerializedName("Change") var change = ""
  @SerializedName("StockExchange") var stockExchange = ""

  // Add Position fields
  var isPosition: Boolean = false
  var positionPrice: Float = 0.toFloat()
  var positionShares: Int = 0

  internal fun getChangeFromPercentString(percentString: String?): Double {
    if (percentString == null || percentString.isEmpty()) {
      return -1000000.0
    }
    try {
      return java.lang.Double.valueOf(percentString.replace('%', '\u0000').trim { it <= ' ' })!!
    } catch (t: Throwable) {
      return -1000000.0
    }

  }

  override fun equals(other: Any?): Boolean {
    if (other is Stock) {
      return symbol.equals(other.symbol, ignoreCase = true)
    } else if (other is String) {
      return other.equals(symbol, ignoreCase = true)
    }
    return false
  }

  override fun hashCode(): Int {
    return symbol.hashCode()
  }

  override fun toString(): String {
    return symbol
  }

  fun isIndex(): Boolean {
    return symbol.startsWith("^") || symbol.contains("=")
  }

  override operator fun compareTo(other: Stock): Int {
    return java.lang.Double.compare(getChangeFromPercentString(other.changeinPercent),
        getChangeFromPercentString(changeinPercent))
  }
}