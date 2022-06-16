package com.github.premnirmal.ticker.network.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Properties(
  val symbol: String,
  var notes: String = "",
  var alertAbove: Float = 0.0f,
  var alertBelow: Float = 0.0f,
  var id: Long? = null
) : Parcelable {
  fun isEmpty(): Boolean = !this.notes.isNullOrEmpty() || this.alertAbove > 0.0f || this.alertBelow > 0.0f
}