package com.github.premnirmal.ticker.network.historicaldata;

import java.util.ArrayList;
import android.os.Parcelable;
import java.util.List;
import android.os.Parcel;


public class Result implements Parcelable{

    private static final String FIELD_QUOTE = "quote";


    private List<Quote> mQuotes;


    public Result(){

    }

    public void setQuotes(List<Quote> quotes) {
        mQuotes = quotes;
    }

    public List<Quote> getQuotes() {
        return mQuotes;
    }

    public Result(Parcel in) {
        mQuotes = new ArrayList<Quote>();
        in.readTypedList(mQuotes, Quote.CREATOR);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Result> CREATOR = new Creator<Result>() {
        public Result createFromParcel(Parcel in) {
            return new Result(in);
        }

        public Result[] newArray(int size) {
        return new Result[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(mQuotes);
    }


}