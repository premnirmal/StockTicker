package com.github.premnirmal.ticker.settings

import com.github.premnirmal.ticker.network.data.Holding
import com.github.premnirmal.ticker.network.data.Position
import com.github.premnirmal.ticker.network.data.Quote
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
    fun widgetExport_roundTripsThroughJson() {
        val export = WidgetExport(
            name = "My Watchlist",
            autoSort = true,
            tickers = listOf("MSFT", "AAPL", "GOOG"),
        )

        val restored = serializer.deserializeWidget(serializer.serializeWidget(export))

        assertEquals(export, restored)
    }

    @Test
    fun widgetExport_preservesTickerOrder() {
        val tickers = listOf("TSLA", "AAPL", "MSFT", "AMZN")
        val restored = serializer.deserializeWidget(
            serializer.serializeWidget(
                WidgetExport(name = "w", autoSort = false, tickers = tickers)
            )
        )

        assertEquals(tickers, restored.tickers)
    }

    @Test
    fun widgetExport_serializesTypeAndVersion() {
        val text = serializer.serializeWidget(
            WidgetExport(name = "w", autoSort = false, tickers = listOf("AAPL"))
        )

        assertTrue(text.contains("\"type\":\"${WidgetExport.WIDGET_EXPORT_TYPE}\""))
        assertTrue(text.contains("\"version\":${WidgetExport.WIDGET_EXPORT_VERSION}"))
    }

    @Test
    fun parseWidgetImport_detectsWidgetExport() {
        val text = serializer.serializeWidget(
            WidgetExport(name = "w", autoSort = true, tickers = listOf("MSFT", "AAPL"))
        )

        assertEquals(
            WidgetImportFile.Widget(
                WidgetExport(name = "w", autoSort = true, tickers = listOf("MSFT", "AAPL"))
            ),
            serializer.parseWidgetImport(text)
        )
    }

    @Test
    fun parseWidgetImport_detectsLegacyTickerList() {
        assertEquals(
            WidgetImportFile.Tickers(listOf("AAPL", "MSFT")),
            serializer.parseWidgetImport("AAPL, MSFT, ")
        )
    }

    @Test
    fun parseWidgetImport_detectsPortfolioExportInsteadOfTickers() {
        val portfolio = serializer.serializePortfolio(
            listOf(Quote(symbol = "AAPL", name = "Apple Inc", lastTradePrice = 1.23f))
        )

        assertEquals(WidgetImportFile.Portfolio, serializer.parseWidgetImport(portfolio))
    }

    @Test
    fun parseWidgetImport_rejectsMalformedJson() {
        assertFailsWith<InvalidImportFileException> {
            serializer.parseWidgetImport("""{"not":"a widget export"}""")
        }
    }

    @Test
    fun parseWidgetImport_rejectsInvalidLegacyTicker() {
        assertFailsWith<InvalidImportFileException> {
            serializer.parseWidgetImport("AAPL, not a ticker!")
        }
    }

    @Test
    fun parseWidgetImport_handlesUtf8Bom() {
        val text = "\uFEFF" + serializer.serializeWidget(
            WidgetExport(name = "w", autoSort = false, tickers = listOf("AAPL"))
        )

        assertEquals(
            WidgetImportFile.Widget(
                WidgetExport(name = "w", autoSort = false, tickers = listOf("AAPL"))
            ),
            serializer.parseWidgetImport(text)
        )
    }

    @Test
    fun validateTickers_removesDuplicatesPreservingOrder() {
        assertEquals(
            listOf("AAPL", "MSFT"),
            serializer.validateTickers(listOf("AAPL", "MSFT", "AAPL"))
        )
    }

    @Test
    fun deserializeWidget_rejectsWrongType() {
        assertFailsWith<InvalidImportFileException> {
            serializer.deserializeWidget(
                """{"type":"portfolio","version":1,"name":"w","autoSort":false,"tickers":["AAPL"]}"""
            )
        }
    }

    @Test
    fun deserializeWidget_rejectsNewerVersion() {
        assertFailsWith<InvalidImportFileException> {
            serializer.deserializeWidget(
                """{"type":"widget","version":2,"name":"w","autoSort":false,"tickers":["AAPL"]}"""
            )
        }
    }

    @Test
    fun deserializeWidget_rejectsEmptyTickerList() {
        assertFailsWith<InvalidImportFileException> {
            serializer.deserializeWidget(
                """{"type":"widget","version":1,"name":"w","autoSort":false,"tickers":[]}"""
            )
        }
    }

    @Test
    fun deserializeWidget_rejectsJsonFragmentsAsTickers() {
        assertFailsWith<InvalidImportFileException> {
            serializer.deserializeWidget(
                """{"type":"widget","version":1,"name":"w","autoSort":false,"tickers":["AAPL","\"symbol\":\"MSFT\""]}"""
            )
        }
    }

    @Test
    fun deserializeWidget_acceptsRealWorldSymbols() {
        val tickers = listOf("BRK.B", "BTC-USD", "^GSPC", "EURUSD=X")
        val text = serializer.serializeWidget(
            WidgetExport(name = "w", autoSort = false, tickers = tickers)
        )

        assertEquals(tickers, serializer.deserializeWidget(text).tickers)
    }

    @Test
    fun deserializePortfolio_rejectsWidgetExport() {
        val text = serializer.serializeWidget(
            WidgetExport(name = "w", autoSort = false, tickers = listOf("AAPL"))
        )

        assertFailsWith<InvalidImportFileException> {
            serializer.deserializePortfolio(text)
        }
    }
}
