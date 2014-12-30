package com.github.premnirmal.ticker.network.historicaldata;

import android.os.Parcelable;
import android.os.Parcel;


public class Url implements Parcelable {

    private static final String FIELD_EXECUTION_TIME = "execution-time";
    private static final String FIELD_EXECUTION_START_TIME = "execution-start-time";
    private static final String FIELD_EXECUTION_STOP_TIME = "execution-stop-time";
    private static final String FIELD_CONTENT = "content";


    private int mExecutionTime;
    private int mExecutionStartTime;
    private int mExecutionStopTime;
    private String mContent;


    public Url() {

    }

    public void setExecutionTime(int executionTime) {
        mExecutionTime = executionTime;
    }

    public int getExecutionTime() {
        return mExecutionTime;
    }

    public void setExecutionStartTime(int executionStartTime) {
        mExecutionStartTime = executionStartTime;
    }

    public int getExecutionStartTime() {
        return mExecutionStartTime;
    }

    public void setExecutionStopTime(int executionStopTime) {
        mExecutionStopTime = executionStopTime;
    }

    public int getExecutionStopTime() {
        return mExecutionStopTime;
    }

    public void setContent(String content) {
        mContent = content;
    }

    public String getContent() {
        return mContent;
    }

    public Url(Parcel in) {
        mExecutionTime = in.readInt();
        mExecutionStartTime = in.readInt();
        mExecutionStopTime = in.readInt();
        mContent = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Url> CREATOR = new Creator<Url>() {
        public Url createFromParcel(Parcel in) {
            return new Url(in);
        }

        public Url[] newArray(int size) {
            return new Url[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mExecutionTime);
        dest.writeInt(mExecutionStartTime);
        dest.writeInt(mExecutionStopTime);
        dest.writeString(mContent);
    }


}