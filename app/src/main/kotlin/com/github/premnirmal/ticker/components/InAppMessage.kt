package com.github.premnirmal.ticker.components

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.github.premnirmal.tickerwidget.R
import com.google.android.material.snackbar.Snackbar

/**
 * Created by premnirmal on 2/26/16.
 */
object InAppMessage {

  private fun Activity.getRootView(): View =
    (this.findViewById<View>(android.R.id.content) as ViewGroup).getChildAt(0)

  fun showToast(context: Context, messageResId: Int) {
    Toast.makeText(context, messageResId, Toast.LENGTH_SHORT)
        .show()
  }

  fun showToast(context: Context, message: CharSequence) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT)
        .show()
  }

  fun showMessage(activity: Activity, messageResId: Int, error: Boolean = false) {
    showMessage(activity, activity.getString(messageResId), error)
  }

  fun showMessage(view: View, messageResId: Int, error: Boolean = false) {
    showMessage(view, view.resources.getString(messageResId), error)
  }

  fun showMessage(activity: Activity, message: CharSequence, error: Boolean = false) {
    val snackbar = createSnackbar(activity.getRootView(), message, error)
    snackbar.show()
  }

  fun showMessage(view: View, message: String, error: Boolean) {
    val snackbar = createSnackbar(view, message, error)
    snackbar.show()
  }

  private fun createSnackbar(view: View, message: CharSequence,
                             error: Boolean = false): Snackbar {
    val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG)
    val snackBarView = snackbar.view
    val params = snackBarView.layoutParams as ViewGroup.MarginLayoutParams
    val margin = snackBarView.context.resources.getDimensionPixelSize(R.dimen.snackbar_margin)
    params.setMargins(margin, margin, margin, margin)
    snackBarView.layoutParams = params
    if (error) {
      val bg = R.drawable.snackbar_bg_error
      snackBarView.background = ResourcesCompat.getDrawable(view.context.resources, bg, null)
      val text = snackBarView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
      text.setTextColor(ContextCompat.getColor(view.context, R.color.white))
    }
    return snackbar
  }
}