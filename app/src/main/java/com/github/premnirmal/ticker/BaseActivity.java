package com.github.premnirmal.ticker;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.app.ActionBarActivity;

import com.github.premnirmal.tickerwidget.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

/**
 * Created by premnirmal on 12/24/14.
 */
public abstract class BaseActivity extends ActionBarActivity {

    private final Random random = new Random();

    private static final List<Integer> colorResources = new ArrayList<Integer>() {
        {
            add(R.color.sea);
            add(R.color.turqoise);
            add(R.color.grass);
        }
    };

    protected int randomColor() {
        return getResources().getColor(colorResources.get(random.nextInt(colorResources.size())));
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onResume() {
        super.onResume();
        setRandomActionBarColor();
    }

    protected void setRandomActionBarColor() {
        final Drawable drawable = new ColorDrawable(randomColor());
        getSupportActionBar().setBackgroundDrawable(drawable);
    }



    protected final AlertDialog showDialog(String message) {
        return showDialog(message, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
    }

    protected final AlertDialog showDialog(String message, DialogInterface.OnClickListener listener) {
        return new AlertDialog.Builder(this)
                .setMessage(message)
                .setNeutralButton("OK", listener)
                .show();
    }
}
