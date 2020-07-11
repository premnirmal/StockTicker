package com.github.premnirmal.ticker.network.data

import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator

data class Properties(
  val symbol: String,
  var notes: String = "",
  var alertAbove: Float = 0.0f,
  var alertBelow: Float = 0.0f,
  var id: Long? = null
) : Parcelable {
  constructor(parcel: Parcel) : this(
      parcel.readString()!!,
      parcel.readString()!!,
      parcel.readFloat(),
      parcel.readFloat(),
      parcel.readLong()
  )

  override fun writeToParcel(
    parcel: Parcel,
    flags: Int
  ) {
    parcel.writeString(symbol)
    parcel.writeString(notes)
    parcel.writeFloat(alertAbove)
    parcel.writeFloat(alertBelow)
    id?.let { parcel.writeLong(it) }
  }

  override fun describeContents(): Int {
    return 0
  }

  companion object {
    @JvmField
    val CREATOR = object : Creator<Properties> {
      override fun createFromParcel(parcel: Parcel): Properties {
        return Properties(parcel)
      }

      override fun newArray(size: Int): Array<Properties?> {
        return arrayOfNulls(size)
      }
    }
  }

  fun isEmpty(): Boolean = !this.notes.isNullOrEmpty() || this.alertAbove > 0.0f || this.alertBelow > 0.0f
}