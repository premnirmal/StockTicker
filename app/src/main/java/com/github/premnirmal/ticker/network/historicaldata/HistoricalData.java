package com.github.premnirmal.ticker.network.historicaldata;

import android.os.Parcelable;
import android.os.Parcel;


public class HistoricalData implements Parcelable{

    private static final String FIELD_QUERY = "query";


    private Query mQuery;


    public HistoricalData(){

    }

    public void setQuery(Query query) {
        mQuery = query;
    }

    public Query getQuery() {
        return mQuery;
    }

    public HistoricalData(Parcel in) {
        mQuery = in.readParcelable(Query.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<HistoricalData> CREATOR = new Creator<HistoricalData>() {
        public HistoricalData createFromParcel(Parcel in) {
            return new HistoricalData(in);
        }

        public HistoricalData[] newArray(int size) {
        return new HistoricalData[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mQuery, flags);
    }


}