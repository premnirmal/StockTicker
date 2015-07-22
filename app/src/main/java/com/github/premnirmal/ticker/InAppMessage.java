package com.github.premnirmal.ticker;

import android.app.Activity;
import android.content.Context;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.view.ViewGroup;

import com.github.premnirmal.tickerwidget.R;

/**
 * Created by premnirmal on 7/21/15.
 */
public final class InAppMessage {

    public static void showMessage(Activity activity, int messageResId) {
        if (activity == null) {
            return;
        }
        showMessage(activity, activity.getString(messageResId));
    }

    public static void showMessage(Activity activity, CharSequence message) {
        if (activity == null) {
            return;
        }
        final Snackbar snackbar = createSnackbar(getRootView(activity),message);
        snackbar.show();
    }

    public static void showMessage(Activity activity, CharSequence message, CharSequence actionText, View.OnClickListener actionClick) {
        if (activity == null) {
            return;
        }
        final Snackbar snackbar = createSnackbar(getRootView(activity),message);
        snackbar.setAction(actionText, actionClick);
        snackbar.show();
    }

    public static void showMessage(View view, CharSequence message) {
        if(view == null) {
            return;
        }
        final Snackbar snackbar = createSnackbar(view, message);
        snackbar.show();
    }

    private static Snackbar createSnackbar(View view, CharSequence message) {
        final Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);
        View snackBarView = snackbar.getView();
        snackBarView.setBackgroundColor(getSnackbarColor(view.getContext()));
        return snackbar;
    }

    private static int getSnackbarColor(Context context) {
        return context.getResources().getColor(R.color.color_primary);
    }

    private static View getRootView(Activity activity) {
        return ((ViewGroup) activity
                .findViewById(android.R.id.content)).getChildAt(0);
    }

}
