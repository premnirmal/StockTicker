package com.github.premnirmal.ticker.network.historicaldata;

import android.os.Parcelable;
import android.os.Parcel;


public class Query implements Parcelable{

    private static final String FIELD_COUNT = "count";
    private static final String FIELD_RESULTS = "results";
    private static final String FIELD_EXECUTION_TIME = "execution-time";
    private static final String FIELD_EXECUTION_START_TIME = "execution-start-time";
    private static final String FIELD_EXECUTION_STOP_TIME = "execution-stop-time";
    private static final String FIELD_PARAMS = "params";
    private static final String FIELD_DIAGNOSTICS = "diagnostics";
    private static final String FIELD_CREATED = "created";
    private static final String FIELD_CONTENT = "content";
    private static final String FIELD_LANG = "lang";


    private int mCount;
    private Result mResult;
    private int mExecutionTime;
    private int mExecutionStartTime;
    private int mExecutionStopTime;
    private String mParam;
    private Diagnostic mDiagnostic;
    private String mCreated;
    private String mContent;
    private String mLang;


    public Query(){

    }

    public void setCount(int count) {
        mCount = count;
    }

    public int getCount() {
        return mCount;
    }

    public void setResult(Result result) {
        mResult = result;
    }

    public Result getResult() {
        return mResult;
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

    public void setParam(String param) {
        mParam = param;
    }

    public String getParam() {
        return mParam;
    }

    public void setDiagnostic(Diagnostic diagnostic) {
        mDiagnostic = diagnostic;
    }

    public Diagnostic getDiagnostic() {
        return mDiagnostic;
    }

    public void setCreated(String created) {
        mCreated = created;
    }

    public String getCreated() {
        return mCreated;
    }

    public void setContent(String content) {
        mContent = content;
    }

    public String getContent() {
        return mContent;
    }

    public void setLang(String lang) {
        mLang = lang;
    }

    public String getLang() {
        return mLang;
    }

    public Query(Parcel in) {
        mCount = in.readInt();
        mResult = in.readParcelable(Result.class.getClassLoader());
        mExecutionTime = in.readInt();
        mExecutionStartTime = in.readInt();
        mExecutionStopTime = in.readInt();
        mParam = in.readString();
        mDiagnostic = in.readParcelable(Diagnostic.class.getClassLoader());
        mCreated = in.readString();
        mContent = in.readString();
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
        dest.writeInt(mExecutionTime);
        dest.writeInt(mExecutionStartTime);
        dest.writeInt(mExecutionStopTime);
        dest.writeString(mParam);
        dest.writeParcelable(mDiagnostic, flags);
        dest.writeString(mCreated);
        dest.writeString(mContent);
        dest.writeString(mLang);
    }


}