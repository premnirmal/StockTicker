package com.github.premnirmal.ticker.repo.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class FetchLogRow(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "created_at_ms") val createdAtMs: Long,
    @ColumnInfo(name = "source") val source: String,
    @ColumnInfo(name = "event") val event: String,
    @ColumnInfo(name = "detail") val detail: String,
)
