package com.github.premnirmal.ticker

import android.app.Activity
import android.content.Context
import android.support.design.widget.Snackbar
import android.view.View
import android.view.ViewGroup
import com.github.premnirmal.tickerwidget.R

/**
 * Created by premnirmal on 2/26/16.
 */
object InAppMessage {
    @JvmStatic fun showMessage(activity: Activity?, messageResId: Int) {
        if (activity == null) {
            return
        }
        showMessage(activity, activity.getString(messageResId))
    }

    @JvmStatic fun showMessage(activity: Activity?, message: CharSequence) {
        if (activity == null) {
            return
        }
        val snackbar = createSnackbar(getRootView(activity), message)
        snackbar.show()
    }

    @JvmStatic fun showMessage(activity: Activity?, message: CharSequence, actionText: CharSequence, actionClick: View.OnClickListener) {
        if (activity == null) {
            return
        }
        val snackbar = createSnackbar(getRootView(activity), message)
        snackbar.setAction(actionText, actionClick)
        snackbar.show()
    }

    @JvmStatic fun showMessage(view: View?, message: CharSequence) {
        if (view == null) {
            return
        }
        val snackbar = createSnackbar(view, message)
        snackbar.show()
    }

    @JvmStatic private fun createSnackbar(view: View, message: CharSequence): Snackbar {
        val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG)
        val snackBarView = snackbar.view
        snackBarView.setBackgroundColor(getSnackbarColor(view.context))
        return snackbar
    }

    @JvmStatic private fun getSnackbarColor(context: Context): Int {
        return context.resources.getColor(R.color.color_primary)
    }

    @JvmStatic private fun getRootView(activity: Activity): View {
        return (activity.findViewById(android.R.id.content) as ViewGroup).getChildAt(0)
    }
}