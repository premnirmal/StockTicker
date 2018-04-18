package com.github.premnirmal.ticker.components

import android.content.Context
import android.net.ConnectivityManager


fun Context.isNetworkOnline(): Boolean {
  try {
    val connectivityManager = this.getSystemService(
        Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val i = connectivityManager.activeNetworkInfo ?: return false
    if (!i.isConnected) return false
    if (!i.isAvailable) return false
    return true
  } catch (e: Exception) {
    e.printStackTrace()
    return false
  }
}

fun Long.minutesInMs(): Long {
  return this * 60 * 1000
}