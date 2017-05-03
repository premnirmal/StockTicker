package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.network.data.ErrorBody

class RobindahoodException(val errorBody: ErrorBody, cause: Exception)
        : java.lang.Exception(errorBody.message, cause)