package com.github.premnirmal.ticker.network.historicaldata;

import android.os.Parcel;
import android.os.Parcelable;

import com.github.premnirmal.ticker.network.QueryCreator;
import com.google.gson.annotations.SerializedName;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;

public class Quote implements Parcelable, Comparable<Quote> {

  public static final DateTimeFormatter formatter = QueryCreator.formatter;

  public static final String FIELD_HIGH = "High";
  public static final String FIELD_OPEN = "Open";
  public static final String FIELD_SYMBOL = "Symbol";
  public static final String FIELD_ADJ_CLOSE = "Adj_Close";
  public static final String FIELD_CLOSE = "Close";
  public static final String FIELD_VOLUME = "Volume";
  public static final String FIELD_DATE = "Date";
  public static final String FIELD_LOW = "Low";

  @SerializedName(FIELD_HIGH) public double mHigh;
  @SerializedName(FIELD_OPEN) public double mOpen;
  @SerializedName(FIELD_SYMBOL) public String mSymbol;
  @SerializedName(FIELD_ADJ_CLOSE) public double mAdjClose;
  @SerializedName(FIELD_CLOSE) public double mClose;
  @SerializedName(FIELD_VOLUME) public int mVolume;
  @SerializedName(FIELD_DATE) public String mDate;
  @SerializedName(FIELD_LOW) public double mLow;

  public Quote() {

  }

  public DateTime getDate() {
    return formatter.parseDateTime(mDate);
  }

  public Quote(Parcel in) {
    mHigh = in.readDouble();
    mOpen = in.readDouble();
    mSymbol = in.readString();
    mAdjClose = in.readDouble();
    mClose = in.readDouble();
    mVolume = in.readInt();
    mDate = in.readString();
    mLow = in.readDouble();
  }

  @Override public int describeContents() {
    return 0;
  }

  public static final Parcelable.Creator<Quote> CREATOR = new Parcelable.Creator<Quote>() {
    public Quote createFromParcel(Parcel in) {
      return new Quote(in);
    }

    public Quote[] newArray(int size) {
      return new Quote[size];
    }
  };

  @Override public void writeToParcel(Parcel dest, int flags) {
    dest.writeDouble(mHigh);
    dest.writeDouble(mOpen);
    dest.writeString(mSymbol);
    dest.writeDouble(mAdjClose);
    dest.writeDouble(mClose);
    dest.writeInt(mVolume);
    dest.writeString(mDate);
    dest.writeDouble(mLow);
  }

  @Override public int compareTo(Quote another) {
    return mDate.compareTo(another.mDate);
  }
}