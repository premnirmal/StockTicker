package com.github.premnirmal.ticker

import android.Manifest
import android.R.attr
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.DialogInterface.OnClickListener
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.github.premnirmal.ticker.components.AppClock.AppClockImpl
import com.github.premnirmal.tickerwidget.R
import com.robinhood.ticker.TickerView
import org.threeten.bp.DayOfWeek
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.TextStyle.SHORT
import java.math.RoundingMode
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Date
import java.util.Locale


val Number.toPx
  get() = TypedValue.applyDimension(
      TypedValue.COMPLEX_UNIT_DIP,
      this.toFloat(),
      Resources.getSystem().displayMetrics
  )

fun Drawable.toBitmap(): Bitmap {
  if (this is BitmapDrawable) {
    return bitmap
  }

  val width = if (bounds.isEmpty) intrinsicWidth else bounds.width()
  val height = if (bounds.isEmpty) intrinsicHeight else bounds.height()

  return Bitmap.createBitmap(width.nonZero(), height.nonZero(), Bitmap.Config.ARGB_8888)
      .also {
        val canvas = Canvas(it)
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)
      }
}

fun Int.nonZero() = if (this <= 0) 1 else this

fun Context.getStatusBarHeight(): Int {
  val result: Int
  val resourceId: Int = this.resources.getIdentifier("status_bar_height", "dimen", "android")
  result = if (resourceId > 0) {
    this.resources.getDimensionPixelSize(resourceId)
  } else {
    0
  }
  return result
}

fun Context.getActionBarHeight(): Int {
  val tv = TypedValue()
  val result = if (theme.resolveAttribute(attr.actionBarSize, tv, true)) {
    TypedValue.complexToDimensionPixelSize(tv.data, resources.displayMetrics)
  } else 0
  return result
}

fun Context.getNavigationBarHeight(): Int {
  val resourceId: Int = resources.getIdentifier(
      "navigation_bar_height",
      "dimen", "android"
  )
  return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
}



@RequiresApi(33) fun Context.hasNotificationPermission(): Boolean {
  return checkCallingOrSelfPermission(
          Manifest.permission.POST_NOTIFICATIONS
      ) == PackageManager.PERMISSION_GRANTED
}

fun Activity.dismissKeyboard() {
  val view = currentFocus
  if (view is TextView) {
    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(view.windowToken, 0)
  }
}

fun Fragment.dismissKeyboard() {
  requireActivity().dismissKeyboard()
}

fun EditText.showKeyboard() {
  requestFocus()
  val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
  imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}

fun Activity.showDialog(
  message: String,
  listener: OnClickListener,
  cancelable: Boolean = true
) {
  AlertDialog.Builder(this)
      .setMessage(message)
      .setCancelable(cancelable)
      .setNeutralButton(R.string.ok, listener)
      .show()
}

fun Activity.showDialog(message: String, cancelable: Boolean = true): AlertDialog {
  return AlertDialog.Builder(this)
      .setMessage(message)
      .setCancelable(cancelable)
      .setNeutralButton(R.string.ok) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
      .show()
}

fun Activity.showDialog(
  title: String,
  message: String,
  cancelable: Boolean = true
): AlertDialog {
  return AlertDialog.Builder(this)
      .setTitle(title)
      .setMessage(message)
      .setCancelable(cancelable)
      .setNeutralButton(R.string.ok) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
      .show()
}

fun Fragment.showDialog(
  message: String,
  listener: OnClickListener,
) {
  AlertDialog.Builder(requireContext())
      .setMessage(message)
      .setNeutralButton(getString(R.string.ok), listener)
      .show()
}

fun Fragment.showDialog(message: String, cancelable: Boolean = true): AlertDialog {
  return AlertDialog.Builder(requireContext())
      .setMessage(message)
      .setCancelable(cancelable)
      .setNeutralButton(R.string.ok) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
      .show()
}

fun Context.isNetworkOnline(): Boolean {
  try {
    val connectivityManager =
      this.getSystemService(
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
    val day: String = DayOfWeek.from(this)
        .getDisplayName(SHORT, Locale.getDefault())
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

fun formatNumber(price: Float, currencyCode: String): String {
  val currencyFormatter = NumberFormat.getCurrencyInstance().apply {
    currency = Currency.getInstance(currencyCode)
    maximumFractionDigits = 2
    roundingMode = RoundingMode.FLOOR
  }
  return currencyFormatter.format(price)
}

fun TickerView.formatChange(change: Float) {
  when {
    change > 0 -> {
      text = context.getString(R.string.quote_change_pos, change)
      textColor = ContextCompat.getColor(context, R.color.change_positive)
    }
    change < 0 -> {
      text = context.getString(R.string.quote_change_neg, change)
      textColor = ContextCompat.getColor(context, R.color.change_negative)
    }
    else -> {
      text = context.getString(R.string.quote_change_neg, change)
      textColor = ContextCompat.getColor(context, R.color.white)
    }
  }
}

fun TickerView.formatChangePercent(changePercent: Float) {
  when {
    changePercent > 0 -> {
      text = context.getString(R.string.quote_change_percent_pos, changePercent)
      textColor = ContextCompat.getColor(context, R.color.change_positive)
    }
    changePercent < 0 -> {
      text = context.getString(R.string.quote_change_percent_neg, changePercent)
      textColor = ContextCompat.getColor(context, R.color.change_negative)
    }
    else -> {
      text = context.getString(R.string.quote_change_percent_neg, changePercent)
      textColor = ContextCompat.getColor(context, R.color.white)
    }
  }
}

inline fun <T : ViewBinding> Activity.viewBinding(
  crossinline bindingInflater: (LayoutInflater) -> T) =
  lazy(LazyThreadSafetyMode.NONE) {
    bindingInflater.invoke(layoutInflater)
  }

inline fun <T : ViewBinding> Fragment.viewBinding(
  crossinline bindingInflater: (LayoutInflater) -> T) =
  lazy(LazyThreadSafetyMode.NONE) {
    bindingInflater.invoke(layoutInflater)
  }