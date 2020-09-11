package com.github.premnirmal.ticker.repo.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class PropertiesRow(
  @PrimaryKey(autoGenerate = true) var id: Long? = null,
  @ColumnInfo(name = "properties_quote_symbol") val quoteSymbol: String,
  @ColumnInfo(name = "notes") val notes: String = "",
  @ColumnInfo(name = "alert_above") val alertAbove: Float = 0.0f,
  @ColumnInfo(name = "alert_below") val alertBelow: Float = 0.0f
)