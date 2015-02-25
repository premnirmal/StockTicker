package com.github.premnirmal.ticker;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v7.app.ActionBarActivity;

import com.github.premnirmal.tickerwidget.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import rx.Observable;
import rx.android.app.AppObservable;
import rx.android.lifecycle.LifecycleEvent;
import rx.android.lifecycle.LifecycleObservable;
import rx.subjects.BehaviorSubject;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

/**
 * Created by premnirmal on 12/24/14.
 */
public abstract class BaseActivity extends ActionBarActivity {

    private static final List<Integer> colorResources = new ArrayList<Integer>() {
        {
            add(R.color.sea);
            add(R.color.turqoise);
            add(R.color.grass);
        }
    };

    private final Random random = new Random();

    private final BehaviorSubject<LifecycleEvent> lifecycleSubject = BehaviorSubject.create();

    private Observable<LifecycleEvent> lifecycle() {
        return lifecycleSubject.asObservable();
    }

    protected int randomColor() {
        return getResources().getColor(colorResources.get(random.nextInt(colorResources.size())));
    }

    /**
     * Using this to automatically unsubscribe from observables on lifecycle events
     * @param observable
     * @param <T>
     * @return
     */
    protected <T> Observable<T> bind(Observable<T> observable) {
        Observable<T> boundObservable = AppObservable.bindActivity(this, observable);
        return LifecycleObservable.bindActivityLifecycle(lifecycle(), boundObservable);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    public void onCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        lifecycleSubject.onNext(LifecycleEvent.CREATE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        lifecycleSubject.onNext(LifecycleEvent.START);
    }

    @Override
    protected void onStop() {
        super.onStop();
        lifecycleSubject.onNext(LifecycleEvent.STOP);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        lifecycleSubject.onNext(LifecycleEvent.DESTROY);
    }

    @Override
    protected void onResume() {
        super.onResume();
        lifecycleSubject.onNext(LifecycleEvent.RESUME);
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
