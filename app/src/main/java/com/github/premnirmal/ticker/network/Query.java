package com.github.premnirmal.ticker.network;

import android.os.Parcel;
import android.os.Parcelable;

import com.github.premnirmal.ticker.network.historicaldata.Results;


public class Query implements Parcelable{

    public int count;
    public Results results;
    public int executionTime;
    public int executionStartTime;
    public int executionStopTime;
    public String param;
    public String created;
    public String content;
    public String lang;

    public Query(){

    }

    public Query(Parcel in) {
        count = in.readInt();
        results = in.readParcelable(Results.class.getClassLoader());
        executionTime = in.readInt();
        executionStartTime = in.readInt();
        executionStopTime = in.readInt();
        param = in.readString();
        created = in.readString();
        content = in.readString();
        lang = in.readString();
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
        dest.writeInt(count);
        dest.writeParcelable(results, flags);
        dest.writeInt(executionTime);
        dest.writeInt(executionStartTime);
        dest.writeInt(executionStopTime);
        dest.writeString(param);
        dest.writeString(created);
        dest.writeString(content);
        dest.writeString(lang);
    }


}