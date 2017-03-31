package com.github.premnirmal.ticker.network.data.historicaldata

import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import com.google.gson.annotations.SerializedName

/**
 * Created by premnirmal on 3/30/17.
 */
class History : Parcelable {

  @SerializedName("quote") var quote: List<Quote>? = null

  constructor()

  constructor(`in`: Parcel) {
    `in`.readList(quote, Quote::class.java.classLoader)
  }

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) {
    dest.writeList(quote)
  }

  companion object {

    val CREATOR: Creator<History> = object : Creator<History> {
      override fun createFromParcel(`in`: Parcel): History {
        return History(`in`)
      }

      override fun newArray(size: Int): Array<History> {
        return newArray(size)
      }
    }
  }
}