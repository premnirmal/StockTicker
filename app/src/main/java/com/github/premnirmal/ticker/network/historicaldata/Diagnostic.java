package com.github.premnirmal.ticker.network.historicaldata;

import java.util.ArrayList;
import android.os.Parcelable;
import java.util.List;
import android.os.Parcel;


public class Diagnostic implements Parcelable{

    private static final String FIELD_PUBLICLY_CALLABLE = "publiclyCallable";
    private static final String FIELD_SERVICE_TIME = "service-time";
    private static final String FIELD_JAVASCRIPT = "javascript";
    private static final String FIELD_CACHE = "cache";
    private static final String FIELD_QUERY = "query";
    private static final String FIELD_USER_TIME = "user-time";
    private static final String FIELD_BUILD_VERSION = "build-version";
    private static final String FIELD_URL = "url";


    private String mPubliclyCallable;
    private int mServiceTime;
    private Javascript mJavascript;
    private List<Cache> mCaches;
    private List<Query> mQueries;
    private int mUserTime;
    private String mBuildVersion;
    private List<Url> mUrls;


    public Diagnostic(){

    }

    public void setPubliclyCallable(String publiclyCallable) {
        mPubliclyCallable = publiclyCallable;
    }

    public String getPubliclyCallable() {
        return mPubliclyCallable;
    }

    public void setServiceTime(int serviceTime) {
        mServiceTime = serviceTime;
    }

    public int getServiceTime() {
        return mServiceTime;
    }

    public void setJavascript(Javascript javascript) {
        mJavascript = javascript;
    }

    public Javascript getJavascript() {
        return mJavascript;
    }

    public void setCaches(List<Cache> caches) {
        mCaches = caches;
    }

    public List<Cache> getCaches() {
        return mCaches;
    }

    public void setQueries(List<Query> queries) {
        mQueries = queries;
    }

    public List<Query> getQueries() {
        return mQueries;
    }

    public void setUserTime(int userTime) {
        mUserTime = userTime;
    }

    public int getUserTime() {
        return mUserTime;
    }

    public void setBuildVersion(String buildVersion) {
        mBuildVersion = buildVersion;
    }

    public String getBuildVersion() {
        return mBuildVersion;
    }

    public void setUrls(List<Url> urls) {
        mUrls = urls;
    }

    public List<Url> getUrls() {
        return mUrls;
    }

    public Diagnostic(Parcel in) {
        mPubliclyCallable = in.readString();
        mServiceTime = in.readInt();
        mJavascript = in.readParcelable(Javascript.class.getClassLoader());
        mCaches = new ArrayList<Cache>();
        in.readTypedList(mCaches, Cache.CREATOR);
        mQueries = new ArrayList<Query>();
        in.readTypedList(mQueries, Query.CREATOR);
        mUserTime = in.readInt();
        mBuildVersion = in.readString();
        mUrls = new ArrayList<Url>();
        in.readTypedList(mUrls, Url.CREATOR);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Diagnostic> CREATOR = new Creator<Diagnostic>() {
        public Diagnostic createFromParcel(Parcel in) {
            return new Diagnostic(in);
        }

        public Diagnostic[] newArray(int size) {
        return new Diagnostic[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mPubliclyCallable);
        dest.writeInt(mServiceTime);
        dest.writeParcelable(mJavascript, flags);
        dest.writeTypedList(mCaches);
        dest.writeTypedList(mQueries);
        dest.writeInt(mUserTime);
        dest.writeString(mBuildVersion);
        dest.writeTypedList(mUrls);
    }


}