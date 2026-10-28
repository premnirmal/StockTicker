package com.github.premnirmal.ticker.settings

import com.github.premnirmal.ticker.network.data.Holding
import com.github.premnirmal.ticker.network.data.Position
import com.github.premnirmal.ticker.network.data.Quote
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PortfolioSerializerTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
        coerceInputValues = true
    }

    private val serializer = PortfolioSerializer(json)

    @Test
    fun serializeTickers_appendsSeparatorAfterEach() {
        assertEquals("AAPL, MSFT, GOOG, ", serializer.serializeTickers(listOf("AAPL", "MSFT", "GOOG")))
    }

    @Test
    fun serializeTickers_emptyListIsEmptyString() {
        assertEquals("", serializer.serializeTickers(emptyList()))
    }

    @Test
    fun parseTickers_stripsWhitespaceAndTrailingEmpty() {
        assertEquals(listOf("AAPL", "MSFT", "GOOG"), serializer.parseTickers("AAPL, MSFT, GOOG, "))
    }

    @Test
    fun parseTickers_roundTripsWithSerialize() {
        val tickers = listOf("AAPL", "MSFT", "GOOG")
        assertEquals(tickers, serializer.parseTickers(serializer.serializeTickers(tickers)))
    }

    @Test
    fun parseTickers_handlesNoTrailingSeparator() {
        assertEquals(listOf("AAPL", "MSFT"), serializer.parseTickers("AAPL,MSFT"))
    }

    @Test
    fun portfolio_roundTripsThroughJson() {
        val quote = Quote(symbol = "AAPL", name = "Apple Inc", lastTradePrice = 1.23f).apply {
            position = Position(
                symbol = "AAPL",
                holdings = mutableListOf(Holding("AAPL", shares = 2f, price = 100f))
            )
        }

        val text = serializer.serializePortfolio(listOf(quote))
        val restored = serializer.deserializePortfolio(text)

        assertEquals(1, restored.size)
        assertEquals("AAPL", restored[0].symbol)
        assertEquals("Apple Inc", restored[0].name)
        assertEquals(1.23f, restored[0].lastTradePrice)
        assertEquals(2f, restored[0].position?.totalShares())
    }

    @Test
    fun deserializePortfolio_emptyList() {
        assertTrue(serializer.deserializePortfolio("[]").isEmpty())
    }
}
