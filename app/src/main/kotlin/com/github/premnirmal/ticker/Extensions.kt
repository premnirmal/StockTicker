package com.github.premnirmal.ticker

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.DialogInterface.OnClickListener
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import androidx.fragment.app.Fragment
import com.github.premnirmal.tickerwidget.R

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

fun Activity.showDialog(message: String): AlertDialog {
  return AlertDialog.Builder(this)
      .setMessage(message)
      .setCancelable(false)
      .setNeutralButton(R.string.ok) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
      .show()
}

fun Activity.showDialog(
  title: String,
  message: String
): AlertDialog {
  return AlertDialog.Builder(this)
      .setTitle(title)
      .setMessage(message)
      .setCancelable(false)
      .setNeutralButton(R.string.ok) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
      .show()
}

fun Fragment.showDialog(
  message: String,
  listener: OnClickListener
) {
  AlertDialog.Builder(requireContext())
      .setMessage(message)
      .setNeutralButton(getString(R.string.ok), listener)
      .show()
}

fun Fragment.showDialog(message: String): AlertDialog {
  return AlertDialog.Builder(requireContext())
      .setMessage(message)
      .setCancelable(false)
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

fun Long.minutesInMs(): Long {
  return this * 60 * 1000
}