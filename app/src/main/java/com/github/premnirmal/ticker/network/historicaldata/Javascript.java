package com.github.premnirmal.ticker.network.historicaldata;

import android.os.Parcelable;
import android.os.Parcel;


public class Javascript implements Parcelable{

    private static final String FIELD_INSTRUCTIONS_USED = "instructions-used";
    private static final String FIELD_EXECUTION_TIME = "execution-time";
    private static final String FIELD_EXECUTION_START_TIME = "execution-start-time";
    private static final String FIELD_EXECUTION_STOP_TIME = "execution-stop-time";
    private static final String FIELD_TABLE_NAME = "table-name";


    private int mInstructionsUsed;
    private int mExecutionTime;
    private int mExecutionStartTime;
    private int mExecutionStopTime;
    private String mTableName;


    public Javascript(){

    }

    public void setInstructionsUsed(int instructionsUsed) {
        mInstructionsUsed = instructionsUsed;
    }

    public int getInstructionsUsed() {
        return mInstructionsUsed;
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

    public void setTableName(String tableName) {
        mTableName = tableName;
    }

    public String getTableName() {
        return mTableName;
    }

    public Javascript(Parcel in) {
        mInstructionsUsed = in.readInt();
        mExecutionTime = in.readInt();
        mExecutionStartTime = in.readInt();
        mExecutionStopTime = in.readInt();
        mTableName = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Javascript> CREATOR = new Creator<Javascript>() {
        public Javascript createFromParcel(Parcel in) {
            return new Javascript(in);
        }

        public Javascript[] newArray(int size) {
        return new Javascript[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mInstructionsUsed);
        dest.writeInt(mExecutionTime);
        dest.writeInt(mExecutionStartTime);
        dest.writeInt(mExecutionStopTime);
        dest.writeString(mTableName);
    }


}