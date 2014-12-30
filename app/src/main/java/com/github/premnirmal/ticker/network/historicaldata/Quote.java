package com.github.premnirmal.ticker.network.historicaldata;

import android.os.Parcelable;
import android.os.Parcel;

import com.github.premnirmal.ticker.network.QueryCreator;
import com.jjoe64.graphview.GraphViewDataInterface;

import org.joda.time.format.DateTimeFormatter;


public class Quote implements Parcelable, GraphViewDataInterface {

    public static final DateTimeFormatter formatter = QueryCreator.formatter;

    public double high;
    public double open;
    public String symbol;
    public double adjClose;
    public double close;
    public int volume;
    public String date;
    public double low;

    public Quote() {
    }

    public Quote(Parcel in) {
        high = in.readDouble();
        open = in.readDouble();
        symbol = in.readString();
        adjClose = in.readDouble();
        close = in.readDouble();
        volume = in.readInt();
        date = in.readString();
        low = in.readDouble();
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
        dest.writeDouble(high);
        dest.writeDouble(open);
        dest.writeString(symbol);
        dest.writeDouble(adjClose);
        dest.writeDouble(close);
        dest.writeInt(volume);
        dest.writeString(date);
        dest.writeDouble(low);
    }

    @Override
    public double getX() {
        return formatter.parseDateTime(date).getMillis();
    }

    @Override
    public double getY() {
        return close;
    }
}