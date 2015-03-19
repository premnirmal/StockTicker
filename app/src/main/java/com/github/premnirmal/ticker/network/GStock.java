package com.github.premnirmal.ticker.network;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by premnirmal on 3/17/15.
 */
public class GStock {

    @Expose
    public String id;
    @Expose
    public String t;
    @Expose
    public String e;
    @Expose
    public String l;
    @SerializedName("l_fix")
    @Expose
    public String lFix;
    @SerializedName("l_cur")
    @Expose
    public String lCur;
    @Expose
    public String s;
    @Expose
    public String ltt;
    @Expose
    public String lt;
    @SerializedName("lt_dts")
    @Expose
    public String ltDts;
    @Expose
    public String c;
    @SerializedName("c_fix")
    @Expose
    public String cFix;
    @Expose
    public String cp;
    @SerializedName("cp_fix")
    @Expose
    public String cpFix;
    @Expose
    public String ccol;
    @SerializedName("pcls_fix")
    @Expose
    public String pclsFix;
    @Expose
    public String el;
    @SerializedName("el_fix")
    @Expose
    public String elFix;
    @SerializedName("el_cur")
    @Expose
    public String elCur;
    @Expose
    public String elt;
    @Expose
    public String ec;
    @SerializedName("ec_fix")
    @Expose
    public String ecFix;
    @Expose
    public String ecp;
    @SerializedName("ecp_fix")
    @Expose
    public String ecpFix;
    @Expose
    public String eccol;
    @Expose
    public String div;
    @Expose
    public String yld;

 

}