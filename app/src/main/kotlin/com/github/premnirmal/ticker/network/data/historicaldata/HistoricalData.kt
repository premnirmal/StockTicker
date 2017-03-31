package com.github.premnirmal.ticker.network.data.historicaldata

import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator

/**
 * Created by premnirmal on 3/30/17.
 */
class HistoricalData : Parcelable {

  var query: Query? = null

  constructor()

  constructor(`in`: Parcel) {
    query = `in`.readParcelable<Query>(Query::class.java.classLoader)
  }

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) {
    dest.writeParcelable(query, flags)
  }

  companion object {

    val CREATOR: Creator<HistoricalData> = object : Creator<HistoricalData> {
      override fun createFromParcel(`in`: Parcel): HistoricalData {
        return HistoricalData(`in`)
      }

      override fun newArray(size: Int): Array<HistoricalData> {
        return newArray(size)
      }
    }
  }
}