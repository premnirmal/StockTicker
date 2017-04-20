package com.github.premnirmal.ticker.model

import android.os.Parcel
import android.os.Parcelable
import com.github.mikephil.charting.data.Entry
import com.github.premnirmal.ticker.network.data.historicaldata.HistoryQuote
import java.io.Serializable

/**
 * Created by premnirmal on 2/28/16.
 */
class SerializableDataPoint : Entry, Serializable {

  constructor(y: Float, x: Int) : super(y, x)

  constructor(y: Float, x: Int, data: HistoryQuote) : super(y, x, data)

  constructor(source: Parcel) : super(source)

  fun getQuote(): HistoryQuote {
    return (data as HistoryQuote)
  }

  companion object {

    private val serialVersionUID = 42L

    val CREATOR: Parcelable.Creator<SerializableDataPoint> = object : Parcelable.Creator<SerializableDataPoint> {
      override fun createFromParcel(source: Parcel): SerializableDataPoint {
        return SerializableDataPoint(source)
      }

      override fun newArray(size: Int): Array<SerializableDataPoint?> {
        return arrayOfNulls(size)
      }
    }
  }
}