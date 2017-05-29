package com.github.premnirmal.ticker.network.data

import com.google.gson.annotations.SerializedName

data class ErrorBody(@SerializedName("code") var code: Int = 0,
    @SerializedName("error") var message: String = "")