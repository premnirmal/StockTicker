package com.github.premnirmal.ticker.portfolio;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.github.premnirmal.ticker.BaseActivity;
import com.github.premnirmal.ticker.Injector;
import com.github.premnirmal.ticker.model.IStocksProvider;
import com.github.premnirmal.ticker.ui.TwoByTwoGridDivider;
import com.github.premnirmal.tickerwidget.R;

import javax.inject.Inject;

/**
 * Created by premnirmal on 3/7/15.
 */
public class RearrangeActivity extends BaseActivity {

    @Inject
    IStocksProvider stocksProvider;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Injector.inject(this);
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        final RecyclerView recyclerView = new RecyclerView(this);
        recyclerView.setAdapter(new RearrangeAdapter(recyclerView, stocksProvider));
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new TwoByTwoGridDivider((int) getResources().getDimension(R.dimen.list_spacing)));
        setContentView(recyclerView);
    }

}
