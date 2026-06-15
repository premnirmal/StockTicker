package com.github.premnirmal.ticker.network

import kotlin.test.Test
import kotlin.test.assertEquals

class MostActiveParsingTest {

    @Test
    fun parsesSymbolsFromFinStreamerTags() {
        val html = """
            <html><body>
            <fin-streamer class="fw(600)" data-symbol="AAPL">Apple</fin-streamer>
            <fin-streamer data-symbol="MSFT" class="fw(600)">Microsoft</fin-streamer>
            <fin-streamer class='fw(600)' data-symbol='GOOG'>Alphabet</fin-streamer>
            <fin-streamer class="C(blue)" data-symbol="IGNORED">ignored class</fin-streamer>
            <fin-streamer class="fw(600)">no symbol</fin-streamer>
            <fin-streamer class="fw(600)" data-symbol="AAPL">dup</fin-streamer>
            </body></html>
        """.trimIndent()
        val symbols = parseMostActiveSymbols(html)
        assertEquals(listOf("AAPL", "MSFT", "GOOG"), symbols)
    }

    @Test
    fun returnsEmptyWhenNoMatches() {
        assertEquals(emptyList(), parseMostActiveSymbols("<html><body>nothing here</body></html>"))
    }
}
