package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.network.data.ErrorBody

class RobindahoodException(val errorBody: ErrorBody, cause: Exception, val code: Int)
        : java.lang.Exception(errorBody.message, cause)