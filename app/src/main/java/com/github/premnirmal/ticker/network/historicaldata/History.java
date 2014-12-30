package com.github.premnirmal.ticker.network.historicaldata;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

/**
 * Created by premnirmal on 12/30/14.
 */
public class History implements Parcelable {

    public List<Quote> quote;

    public History() {

    }

    public History(Parcel in) {
        in.readList(quote, Quote.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<History> CREATOR = new Creator<History>() {
        public History createFromParcel(Parcel in) {
            return new History(in);
        }

        public History[] newArray(int size) {
            return new History[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeList(quote);
    }
}
