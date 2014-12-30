package com.github.premnirmal.ticker.network.historicaldata;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;


public class Query implements Parcelable{

    public static final String FIELD_COUNT = "count";
    public static final String FIELD_RESULTS = "results";
    public static final String FIELD_CREATED = "created";
    public static final String FIELD_LANG = "lang";


    @SerializedName(FIELD_COUNT)
    public int mCount;
    @SerializedName(FIELD_RESULTS)
    public History mResult;
    @SerializedName(FIELD_CREATED)
    public String mCreated;
    @SerializedName(FIELD_LANG)
    public String mLang;


    public Query(){

    }

    public Query(Parcel in) {
        mCount = in.readInt();
        mResult = in.readParcelable(History.class.getClassLoader());
        mCreated = in.readString();
        mLang = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Query> CREATOR = new Creator<Query>() {
        public Query createFromParcel(Parcel in) {
            return new Query(in);
        }

        public Query[] newArray(int size) {
        return new Query[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mCount);
        dest.writeParcelable(mResult, flags);
        dest.writeString(mCreated);
        dest.writeString(mLang);
    }


}