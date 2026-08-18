package com.github.premnirmal.ticker.settings

import com.github.premnirmal.ticker.network.data.Holding
import com.github.premnirmal.ticker.network.data.Movement
import com.github.premnirmal.ticker.network.data.MovementType
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

    @Test
    fun movementsSurviveARoundTrip() {
        val quote = Quote(symbol = "AAPL", name = "Apple")
        quote.movements = listOf(
            Movement("AAPL", MovementType.BUY, 20f, 150f, 1L),
            Movement("AAPL", MovementType.SELL, 5f, 168.20f, 2L),
        )
        val decoded = serializer.deserializePortfolio(serializer.serializePortfolio(listOf(quote)))
        assertEquals(2, decoded.single().movements.size)
        assertEquals(MovementType.SELL, decoded.single().movements[1].type)
    }

    @Test
    fun legacyExportWithHoldingsStillParses() {
        // pre-v10 file: position with holdings, no movements field
        val legacy = """[{"symbol":"AAPL","name":"Apple","lastTradePrice":100.0,""" +
            """"changeInPercent":0.0,"change":0.0,""" +
            """"position":{"symbol":"AAPL","holdings":[{"symbol":"AAPL","shares":10.0,"price":90.0,"id":1}]},""" +
            """"properties":null}]"""
        val decoded = serializer.deserializePortfolio(legacy)
        val quote = decoded.single()
        assertTrue(quote.movements.isEmpty())
        quote.ensureMovements()
        assertEquals(1, quote.movements.size)
        assertEquals(MovementType.BUY, quote.movements[0].type)
    }
}
