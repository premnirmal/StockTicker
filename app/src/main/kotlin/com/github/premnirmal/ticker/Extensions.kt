package com.github.premnirmal.ticker

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.DialogInterface.OnClickListener
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment

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
  listener: OnClickListener
) {
  AlertDialog.Builder(this)
      .setMessage(message)
      .setNeutralButton("OK", listener)
      .show()
}

fun Activity.showDialog(message: String): AlertDialog {
  return AlertDialog.Builder(this)
      .setMessage(message)
      .setCancelable(false)
      .setNeutralButton("OK") { dialog: DialogInterface, _: Int -> dialog.dismiss() }
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
      .setNeutralButton("OK") { dialog: DialogInterface, _: Int -> dialog.dismiss() }
      .show()
}

fun Fragment.showDialog(
  message: String,
  listener: OnClickListener
) {
  AlertDialog.Builder(context!!)
      .setMessage(message)
      .setNeutralButton("OK", listener)
      .show()
}

fun Fragment.showDialog(message: String): AlertDialog {
  return AlertDialog.Builder(context!!)
      .setMessage(message)
      .setCancelable(false)
      .setNeutralButton("OK") { dialog: DialogInterface, _: Int -> dialog.dismiss() }
      .show()
}

fun Fragment.hideKeyboard(view: View) {
  val imm = activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
  imm.hideSoftInputFromWindow(view.windowToken, 0)
}