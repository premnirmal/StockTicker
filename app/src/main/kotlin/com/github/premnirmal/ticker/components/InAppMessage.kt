package com.github.premnirmal.ticker.components

import android.app.Activity
import android.content.Context
import com.google.android.material.snackbar.Snackbar
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.github.premnirmal.tickerwidget.R

/**
 * Created by premnirmal on 2/26/16.
 */
object InAppMessage {

  private fun Activity.getRootView(): View =
      (this.findViewById<View>(android.R.id.content) as ViewGroup).getChildAt(0)

  fun showToast(context: Context, messageResId: Int) {
    showToast(context, context.getString(messageResId))
  }

  fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
  }

  fun showMessage(activity: Activity?, messageResId: Int) {
    if (activity == null) {
      return
    }
    showMessage(activity, activity.getString(messageResId))
  }

  fun showMessage(activity: Activity?, message: CharSequence) {
    if (activity == null) {
      return
    }
    val snackbar = createSnackbar(activity.getRootView(), message)
    snackbar.show()
  }

  fun showMessage(activity: Activity?, message: CharSequence, actionText: CharSequence,
      actionClick: View.OnClickListener) {
    if (activity == null) {
      return
    }
    val snackbar = createSnackbar(activity.getRootView(), message)
    snackbar.setAction(actionText, actionClick)
    snackbar.show()
  }

  private fun createSnackbar(view: View, message: CharSequence): Snackbar {
    val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG)
    val snackBarView = snackbar.view
    snackBarView.setBackgroundColor(getSnackbarColor(view.context))
    return snackbar
  }

  private fun getSnackbarColor(context: Context): Int =
      context.resources.getColor(R.color.color_primary)
}