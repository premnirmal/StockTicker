package com.github.premnirmal.ticker.network.data

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * Created by premnirmal on 3/30/17.
 */
class GStock() {

  @Expose var id: String = ""
  @Expose var t: String = ""
  @Expose var e: String = ""
  @Expose var l: String = ""
  @SerializedName("l_fix") @Expose var lFix: String = ""
  @SerializedName("l_cur") @Expose var lCur: String = ""
  @Expose var s: String = ""
  @Expose var ltt: String = ""
  @Expose var lt: String = ""
  @SerializedName("lt_dts") @Expose var ltDts: String = ""
  @Expose var c: String = ""
  @SerializedName("c_fix") @Expose var cFix: String = ""
  @Expose var cp: String = ""
  @SerializedName("cp_fix") @Expose var cpFix: String = ""
  @Expose var ccol: String = ""
  @SerializedName("pcls_fix") @Expose var pclsFix: String = ""
  @Expose var el: String = ""
  @SerializedName("el_fix") @Expose var elFix: String = ""
  @SerializedName("el_cur") @Expose var elCur: String = ""
  @Expose var elt: String = ""
  @Expose var ec: String = ""
  @SerializedName("ec_fix") @Expose var ecFix: String = ""
  @Expose var ecp: String = ""
  @SerializedName("ecp_fix") @Expose var ecpFix: String = ""
  @Expose var eccol: String = ""
  @Expose var div: String = ""
  @Expose var yld: String = ""

}