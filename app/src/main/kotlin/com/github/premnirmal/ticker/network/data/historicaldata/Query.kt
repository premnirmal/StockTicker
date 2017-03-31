package com.github.premnirmal.ticker.network.data.historicaldata

import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import com.google.gson.annotations.SerializedName

/**
 * Created by premnirmal on 3/30/17.
 */
class Query : Parcelable {

  companion object {

    const val FIELD_COUNT = "count"
    const val FIELD_RESULTS = "results"
    const val FIELD_CREATED = "created"
    const val FIELD_LANG = "lang"

    val CREATOR: Creator<Query> = object : Creator<Query> {
      override fun createFromParcel(`in`: Parcel): Query {
        return Query(`in`)
      }

      override fun newArray(size: Int): Array<Query> {
        return newArray(size)
      }
    }
  }

  @SerializedName(FIELD_COUNT) var mCount: Int = 0
  @SerializedName(FIELD_RESULTS) var mResult: History? = null
  @SerializedName(FIELD_CREATED) var mCreated: String? = null
  @SerializedName(FIELD_LANG) var mLang: String? = null

  constructor()

  constructor(`in`: Parcel) {
    mCount = `in`.readInt()
    mResult = `in`.readParcelable<History>(History::class.java.classLoader)
    mCreated = `in`.readString()
    mLang = `in`.readString()
  }

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) {
    dest.writeInt(mCount)
    dest.writeParcelable(mResult, flags)
    dest.writeString(mCreated)
    dest.writeString(mLang)
  }
}