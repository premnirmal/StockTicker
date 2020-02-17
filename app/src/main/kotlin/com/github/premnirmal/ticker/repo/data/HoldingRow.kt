package com.github.premnirmal.ticker.repo.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class HoldingRow(
    @PrimaryKey(autoGenerate = true) var id: Long? = null,
    @ColumnInfo(name = "quote_symbol") val quoteSymbol: String,
    @ColumnInfo(name = "shares") val shares: Float = 0.0f,
    @ColumnInfo(name = "price") val price: Float = 0.0f
)