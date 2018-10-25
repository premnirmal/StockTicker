package com.github.premnirmal.ticker

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.DialogInterface.OnClickListener
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.DisplayMetrics
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.View
import android.view.ViewConfiguration
import com.github.premnirmal.ticker.portfolio.search.TickerSelectorActivity

fun Drawable.toBitmap(): Bitmap {
  if (this is BitmapDrawable) {
    return bitmap
  }

  val width = if (bounds.isEmpty) intrinsicWidth else bounds.width()
  val height = if (bounds.isEmpty) intrinsicHeight else bounds.height()

  return Bitmap.createBitmap(width.nonZero(), height.nonZero(), Bitmap.Config.ARGB_8888).also {
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


const val EXTRA_CENTER_X = "centerX"
const val EXTRA_CENTER_Y = "centerY"

fun Activity.openTickerSelector(v: View, widgetId: Int) {
  val intent = TickerSelectorActivity.launchIntent(this, widgetId)
  val rect = Rect()
  v.getGlobalVisibleRect(rect)
  val centerX = (rect.right - ((rect.right - rect.left) / 2))
  val centerY = (rect.bottom - ((rect.bottom - rect.top) / 2))
  intent.putExtra(EXTRA_CENTER_X, centerX)
  intent.putExtra(EXTRA_CENTER_Y, centerY)
  startActivity(intent)
}

fun Activity.showDialog(message: String, listener: OnClickListener) {
  AlertDialog.Builder(this).setMessage(message).setNeutralButton("OK", listener).show()
}

fun Activity.showDialog(message: String): AlertDialog {
  return AlertDialog.Builder(this).setMessage(message).setCancelable(false)
      .setNeutralButton("OK") { dialog: DialogInterface, _: Int -> dialog.dismiss() }.show()
}

fun Activity.showDialog(title: String, message: String): AlertDialog {
  return AlertDialog.Builder(this).setTitle(title).setMessage(message).setCancelable(false)
      .setNeutralButton("OK") { dialog: DialogInterface, _: Int -> dialog.dismiss() }.show()
}

fun Activity.showDialog(message: String, cancelable: Boolean,
  positiveOnClick: DialogInterface.OnClickListener,
  negativeOnClick: DialogInterface.OnClickListener): AlertDialog {
  return AlertDialog.Builder(this).setMessage(message).setCancelable(
      cancelable).setPositiveButton(
      "YES", positiveOnClick).setNegativeButton("NO", negativeOnClick).show()
}

fun Activity.hasNavBar(): Boolean {
  val hasSoftwareKeys: Boolean
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
    val display = windowManager.defaultDisplay
    val realDisplayMetrics = DisplayMetrics()
    display.getRealMetrics(realDisplayMetrics)

    val realHeight = realDisplayMetrics.heightPixels
    val realWidth = realDisplayMetrics.widthPixels

    val displayMetrics = DisplayMetrics()
    display.getMetrics(displayMetrics)

    val displayHeight = displayMetrics.heightPixels
    val displayWidth = displayMetrics.widthPixels

    hasSoftwareKeys = realWidth - displayWidth > 0 || realHeight - displayHeight > 0
  } else {
    val hasMenuKey = ViewConfiguration.get(this).hasPermanentMenuKey()
    val hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK)
    hasSoftwareKeys = !hasMenuKey && !hasBackKey
  }
  return hasSoftwareKeys
}