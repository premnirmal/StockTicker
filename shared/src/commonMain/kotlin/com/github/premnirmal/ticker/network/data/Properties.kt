package com.github.premnirmal.ticker.network.data

import com.github.premnirmal.shared.CommonParcelable
import com.github.premnirmal.shared.CommonParcelize
import kotlinx.serialization.Serializable

@CommonParcelize
@Serializable
data class Properties(
    val symbol: String,
    var notes: String = "",
    var displayname: String = "",
    var alertAbove: Float = 0.0f,
    var alertBelow: Float = 0.0f,
    var id: Long? = null
) : CommonParcelable {
    fun isEmpty(): Boolean = this.notes.isNotEmpty() || this.alertAbove > 0.0f || this.alertBelow > 0.0f
}
