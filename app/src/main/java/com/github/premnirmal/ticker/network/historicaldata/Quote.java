package com.github.premnirmal.ticker.network.historicaldata;

import android.os.Parcelable;
import android.os.Parcel;


public class Quote implements Parcelable{

    private static final String FIELD_HIGH = "High";
    private static final String FIELD_OPEN = "Open";
    private static final String FIELD_SYMBOL = "Symbol";
    private static final String FIELD_ADJ_CLOSE = "Adj_Close";
    private static final String FIELD_CLOSE = "Close";
    private static final String FIELD_VOLUME = "Volume";
    private static final String FIELD_DATE = "Date";
    private static final String FIELD_LOW = "Low";


    private double mHigh;
    private double mOpen;
    private String mSymbol;
    private double mAdjClose;
    private double mClose;
    private int mVolume;
    private String mDate;
    private double mLow;


    public Quote(){

    }

    public void setHigh(double high) {
        mHigh = high;
    }

    public double getHigh() {
        return mHigh;
    }

    public void setOpen(double open) {
        mOpen = open;
    }

    public double getOpen() {
        return mOpen;
    }

    public void setSymbol(String symbol) {
        mSymbol = symbol;
    }

    public String getSymbol() {
        return mSymbol;
    }

    public void setAdjClose(double adjClose) {
        mAdjClose = adjClose;
    }

    public double getAdjClose() {
        return mAdjClose;
    }

    public void setClose(double close) {
        mClose = close;
    }

    public double getClose() {
        return mClose;
    }

    public void setVolume(int volume) {
        mVolume = volume;
    }

    public int getVolume() {
        return mVolume;
    }

    public void setDate(String date) {
        mDate = date;
    }

    public String getDate() {
        return mDate;
    }

    public void setLow(double low) {
        mLow = low;
    }

    public double getLow() {
        return mLow;
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

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Quote> CREATOR = new Creator<Quote>() {
        public Quote createFromParcel(Parcel in) {
            return new Quote(in);
        }

        public Quote[] newArray(int size) {
        return new Quote[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(mHigh);
        dest.writeDouble(mOpen);
        dest.writeString(mSymbol);
        dest.writeDouble(mAdjClose);
        dest.writeDouble(mClose);
        dest.writeInt(mVolume);
        dest.writeString(mDate);
        dest.writeDouble(mLow);
    }


}