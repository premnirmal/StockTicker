package com.github.premnirmal.ticker.network.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifies that the DTOs migrated into commonMain have working serializers generated on
 * every Kotlin Multiplatform target by round-tripping them through JSON.
 */
class DtoSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun roundTripsTrendingResult() {
        val original = TrendingResult(
            count = 1,
            pages = 1,
            currentPage = 1,
            results = listOf(
                Trending(
                    rank = 1,
                    mentions = 10,
                    mentions24hAgo = 5,
                    upvotes = 7,
                    ticker = "AAPL",
                    name = "Apple"
                )
            )
        )

        val decoded = json.decodeFromString<TrendingResult>(json.encodeToString(original))

        assertEquals(original, decoded)
        assertEquals("AAPL", decoded.results.first().ticker)
    }

    @Test
    fun roundTripsRepoCommit() {
        val original = Committer(name = "Prem", email = "prem@example.com")

        val decoded = json.decodeFromString<Committer>(json.encodeToString(original))

        assertEquals(original, decoded)
    }

    @Test
    fun roundTripsSuggestionsNet() {
        val suggestion = SuggestionsNet.SuggestionNet(symbol = "GOOG").apply {
            name = "Alphabet"
            longName = "Alphabet Inc."
            exch = "NMS"
            type = "EQUITY"
        }
        val original = SuggestionsNet(count = 1, result = listOf(suggestion))

        val decoded = json.decodeFromString<SuggestionsNet>(json.encodeToString(original))

        assertEquals("GOOG", decoded.result?.first()?.symbol)
        assertEquals("Alphabet", decoded.result?.first()?.name)
    }
}
