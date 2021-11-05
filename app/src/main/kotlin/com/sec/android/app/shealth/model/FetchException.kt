package com.sec.android.app.shealth.model

import java.io.IOException

class FetchException(message: String, ex: Throwable? = null): IOException(message, ex)