package com.github.premnirmal.ticker.network.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class Properties(
    val symbol: String,
    var notes: String = "",
    var alertAbove: Float = 0.0f,
    var alertBelow: Float = 0.0f,
    var id: Long? = null
) : Parcelable {
    fun isEmpty(): Boolean = this.notes.isNotEmpty() || this.alertAbove > 0.0f || this.alertBelow > 0.0f
}
