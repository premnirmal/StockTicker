package com.github.premnirmal.ticker.network.data.historicaldata

import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import com.github.premnirmal.ticker.network.data.QueryCreator
import com.google.gson.annotations.SerializedName
import org.threeten.bp.temporal.TemporalAccessor

/**
 * Created by premnirmal on 3/30/17.
 */
class HistoryQuote : Parcelable, Comparable<HistoryQuote> {

  companion object {

    val formatter = QueryCreator.formatter

    const val FIELD_HIGH = "High"
    const val FIELD_OPEN = "Open"
    const val FIELD_SYMBOL = "Symbol"
    const val FIELD_ADJ_CLOSE = "Adj_Close"
    const val FIELD_CLOSE = "Close"
    const val FIELD_VOLUME = "Volume"
    const val FIELD_DATE = "Date"
    const val FIELD_LOW = "Low"

    val CREATOR: Creator<HistoryQuote> = object : Creator<HistoryQuote> {
      override fun createFromParcel(`in`: Parcel): HistoryQuote {
        return HistoryQuote(`in`)
      }

      override fun newArray(size: Int): Array<HistoryQuote> {
        return newArray(size)
      }
    }
  }

  @SerializedName(FIELD_HIGH) var mHigh: Double = 0.toDouble()
  @SerializedName(FIELD_OPEN) var mOpen: Double = 0.toDouble()
  @SerializedName(FIELD_SYMBOL) var mSymbol: String = ""
  @SerializedName(FIELD_ADJ_CLOSE) var mAdjClose: Double = 0.toDouble()
  @SerializedName(FIELD_CLOSE) var mClose: Double = 0.toDouble()
  @SerializedName(FIELD_VOLUME) var mVolume: Int = 0
  @SerializedName(FIELD_DATE) var mDate: String = ""
  @SerializedName(FIELD_LOW) var mLow: Double = 0.toDouble()

  constructor()

  val date: TemporalAccessor
    get() = formatter.parse(mDate)

  constructor(`in`: Parcel) {
    mHigh = `in`.readDouble()
    mOpen = `in`.readDouble()
    mSymbol = `in`.readString()
    mAdjClose = `in`.readDouble()
    mClose = `in`.readDouble()
    mVolume = `in`.readInt()
    mDate = `in`.readString()
    mLow = `in`.readDouble()
  }

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) {
    dest.writeDouble(mHigh)
    dest.writeDouble(mOpen)
    dest.writeString(mSymbol)
    dest.writeDouble(mAdjClose)
    dest.writeDouble(mClose)
    dest.writeInt(mVolume)
    dest.writeString(mDate)
    dest.writeDouble(mLow)
  }

  override fun compareTo(other: HistoryQuote): Int {
    return mDate.compareTo(other.mDate)
  }

}