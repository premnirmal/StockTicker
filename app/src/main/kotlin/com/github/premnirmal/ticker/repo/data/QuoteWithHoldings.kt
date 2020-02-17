package com.github.premnirmal.ticker.repo.data

import androidx.room.Embedded
import androidx.room.Relation

data class QuoteWithHoldings(
    @Embedded
    val quote: QuoteRow,
    @Relation(parentColumn = "symbol",
        entityColumn = "quote_symbol")
    val holdings: List<HoldingRow>
)