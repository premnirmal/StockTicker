package com.github.premnirmal.ticker

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.DialogInterface.OnClickListener
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.github.premnirmal.ticker.components.AppClock.AppClockImpl
import com.github.premnirmal.tickerwidget.R
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.ZonedDateTime
import java.time.format.TextStyle.SHORT
import java.util.Date
import java.util.Locale

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = AppPreferences.PREFS_NAME)

@RequiresApi(33)
fun Context.hasNotificationPermission(): Boolean {
    return checkCallingOrSelfPermission(
        Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED
}

fun Activity.showDialog(
    message: String,
    cancelable: Boolean = true
): AlertDialog {
    return AlertDialog.Builder(this).setMessage(message).setCancelable(cancelable)
        .setNeutralButton(R.string.ok) { dialog: DialogInterface, _: Int -> dialog.dismiss() }.show()
}

fun Context.showDialog(
    message: String,
    cancelable: Boolean = true,
    listener: OnClickListener
) {
    AlertDialog.Builder(
        this
    ).setMessage(message).setCancelable(cancelable).setNeutralButton(R.string.ok, listener).show()
}

fun Context.showDialog(message: String): AlertDialog {
    return AlertDialog.Builder(this).setMessage(message).setCancelable(false)
        .setNeutralButton(R.string.ok) { dialog: DialogInterface, _: Int -> dialog.dismiss() }.show()
}

fun Context.isNetworkOnline(): Boolean {
    try {
        val connectivityManager = this.getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as ConnectivityManager
        val i = connectivityManager.activeNetworkInfo ?: return false
        if (!i.isConnected) return false
        if (!i.isAvailable) return false
        return true
    } catch (e: Exception) {
        e.printStackTrace()
        return false
    }
}

fun ZonedDateTime.createTimeString(): String {
    val fetched: String
    val fetchedDayOfWeek = dayOfWeek.value
    val today = AppClockImpl.todayZoned().dayOfWeek.value
    fetched = if (today == fetchedDayOfWeek) {
        AppPreferences.TIME_FORMATTER.format(this)
    } else {
        val day: String = DayOfWeek.from(this).getDisplayName(SHORT, Locale.getDefault())
        val timeStr: String = AppPreferences.TIME_FORMATTER.format(this)
        "$timeStr $day"
    }
    return fetched
}

fun Float.format(fractionDigits: Int = 2): String {
    return NumberFormat.getInstance(Locale.getDefault()).run {
        minimumFractionDigits = fractionDigits
        maximumFractionDigits = fractionDigits
        format(this@format)
    }
}

fun Long.format(): String {
    return NumberFormat.getInstance(Locale.getDefault()).format(this)
}

fun Long.formatDate(format: String): String {
    return SimpleDateFormat(format, Locale.getDefault()).format(Date(this))
}

fun Long.formatBigNumbers(context: Context): String {
    return when {
        this < 100_000 -> NumberFormat.getInstance(Locale.getDefault()).format(this)
        this < 1_000_000 -> context.getString(R.string.number_format_thousands, this.div(1000.0))
        this < 1_000_000_000 -> {
            context.getString(R.string.number_format_millions, this.div(1000000.0))
        }

        this < 1_000_000_000_000 -> {
            context.getString(R.string.number_format_billions, this.div(1000000000.0))
        }

        else -> {
            context.getString(R.string.number_format_trillions, this.div(1000000000000.0))
        }
    }
}
