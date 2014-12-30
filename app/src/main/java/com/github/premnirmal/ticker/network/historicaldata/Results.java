package com.github.premnirmal.ticker.network.historicaldata;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

/**
 * Created by premnirmal on 12/30/14.
 */
public class Results implements Parcelable {

    public List<Quote> quote;

    public Results() {

    }

    public Results(Parcel in) {
        in.readList(quote, Quote.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Results> CREATOR = new Creator<Results>() {
        public Results createFromParcel(Parcel in) {
            return new Results(in);
        }

        public Results[] newArray(int size) {
            return new Results[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeList(quote);
    }
}
