package com.github.premnirmal.ticker.components

import android.app.Activity
import android.content.Context
import android.support.design.widget.Snackbar
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.github.premnirmal.tickerwidget.R

/**
 * Created by premnirmal on 2/26/16.
 */
object InAppMessage {

  fun showToast(context: Context, messageResId: Int) {
    Toast.makeText(context, messageResId, Toast.LENGTH_SHORT).show()
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
    showMessage(getRootView(activity), message)
  }

  fun showMessage(activity: Activity?, message: CharSequence, actionText: CharSequence,
      actionClick: View.OnClickListener) {
    if (activity == null) {
      return
    }
    val snackbar = createSnackbar(getRootView(activity), message)
    snackbar.setAction(actionText, actionClick)
    snackbar.show()
  }

  fun showMessage(view: View?, message: CharSequence) {
    if (view == null) {
      return
    }
    val snackbar = createSnackbar(view, message)
    snackbar.show()
  }

  private fun createSnackbar(view: View, message: CharSequence): Snackbar {
    val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG)
    val snackBarView = snackbar.view
    snackBarView.setBackgroundColor(getSnackbarColor(view.context))
    return snackbar
  }

  private fun getSnackbarColor(context: Context): Int {
    return context.resources.getColor(R.color.color_primary)
  }

  private fun getRootView(activity: Activity): View {
    return (activity.findViewById(android.R.id.content) as ViewGroup).getChildAt(0)
  }
}