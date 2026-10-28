package com.github.premnirmal.ticker

import android.os.Build

internal actual fun supportsSystemNightMode(): Boolean {
    return (
        Build.VERSION.SDK_INT > Build.VERSION_CODES.P ||
            Build.VERSION.SDK_INT == Build.VERSION_CODES.P && "xiaomi".equals(Build.MANUFACTURER, ignoreCase = true) ||
            Build.VERSION.SDK_INT == Build.VERSION_CODES.P && "samsung".equals(Build.MANUFACTURER, ignoreCase = true)
        )
}
