package com.github.premnirmal.ticker.repo.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One BUY/SELL ledger entry for a symbol. Replay order is `id` ascending — the ledger
 * deliberately stores no dates.
 */
@Entity
data class MovementRow(
    @PrimaryKey(autoGenerate = true) var id: Long? = null,
    @ColumnInfo(name = "quote_symbol") val quoteSymbol: String,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "shares") val shares: Float = 0.0f,
    @ColumnInfo(name = "price") val price: Float = 0.0f
)
