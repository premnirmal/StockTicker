package com.github.premnirmal.ticker.network.historicaldata;

import android.os.Parcelable;
import android.os.Parcel;


public class Cache implements Parcelable{

    private static final String FIELD_TYPE = "type";
    private static final String FIELD_EXECUTION_TIME = "execution-time";
    private static final String FIELD_EXECUTION_START_TIME = "execution-start-time";
    private static final String FIELD_EXECUTION_STOP_TIME = "execution-stop-time";
    private static final String FIELD_CONTENT = "content";
    private static final String FIELD_METHOD = "method";


    private String mType;
    private int mExecutionTime;
    private int mExecutionStartTime;
    private int mExecutionStopTime;
    private String mContent;
    private String mMethod;


    public Cache(){

    }

    public void setType(String type) {
        mType = type;
    }

    public String getType() {
        return mType;
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

    public void setMethod(String method) {
        mMethod = method;
    }

    public String getMethod() {
        return mMethod;
    }

    public Cache(Parcel in) {
        mType = in.readString();
        mExecutionTime = in.readInt();
        mExecutionStartTime = in.readInt();
        mExecutionStopTime = in.readInt();
        mContent = in.readString();
        mMethod = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Cache> CREATOR = new Creator<Cache>() {
        public Cache createFromParcel(Parcel in) {
            return new Cache(in);
        }

        public Cache[] newArray(int size) {
        return new Cache[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mType);
        dest.writeInt(mExecutionTime);
        dest.writeInt(mExecutionStartTime);
        dest.writeInt(mExecutionStopTime);
        dest.writeString(mContent);
        dest.writeString(mMethod);
    }


}