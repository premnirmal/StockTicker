package com.github.premnirmal.ticker.model

import android.os.Parcel
import android.os.Parcelable
import com.github.mikephil.charting.data.Entry
import com.github.premnirmal.ticker.network.data.HistoricalData.HistoricalValue
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import java.io.Serializable

class DataPoint : Entry, Serializable, Comparable<DataPoint> {

  constructor(x: Float, y: Float) : super(x, y)

  constructor(x: Float, y: Float, data: HistoricalValue) : super(x, y, data)

  constructor(source: Parcel) : super(source)

  val date: String
    get() = FMT.format(LocalDate.ofEpochDay(x.toLong()))

  override fun compareTo(other: DataPoint): Int = x.compareTo(other.x)

  companion object {

    private val FMT = DateTimeFormatter.ofPattern("MMMM d")
    private const val serialVersionUID = 42L

    val CREATOR: Parcelable.Creator<DataPoint> = object : Parcelable.Creator<DataPoint> {
      override fun createFromParcel(source: Parcel): DataPoint {
        return DataPoint(source)
      }

      override fun newArray(size: Int): Array<DataPoint?> {
        return arrayOfNulls(size)
      }
    }
  }
}