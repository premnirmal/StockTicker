package com.github.premnirmal.ticker;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by premnirmal on 12/24/14.
 */
public abstract class BaseActivity extends ActionBarActivity {

    private final Random random = new Random();

    private static final List<Integer> colorResources = new ArrayList<Integer>() {
        {
            add(R.color.sea);
            add(R.color.maroon);
            add(R.color.turqoise);
            add(R.color.grass);
            add(R.color.dark);
        }
    };

    protected int randomColor() {
        return getResources().getColor(colorResources.get(random.nextInt(colorResources.size())));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_AppCompat);
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
}
