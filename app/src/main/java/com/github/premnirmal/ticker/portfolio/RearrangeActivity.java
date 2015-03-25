package com.github.premnirmal.ticker.portfolio;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.View;
import android.widget.AdapterView;

import com.github.premnirmal.ticker.BaseActivity;
import com.github.premnirmal.ticker.Injector;
import com.github.premnirmal.ticker.model.IStocksProvider;
import com.github.premnirmal.tickerwidget.R;
import com.nhaarman.listviewanimations.itemmanipulation.DynamicListView;

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
        setTitle(R.string.rearrange);
        final DynamicListView listView = new DynamicListView(this);
        final int spacing = getResources().getDimensionPixelSize(R.dimen.list_spacing);
        listView.setPadding(spacing,spacing,spacing,0);
        listView.setDivider(new ColorDrawable(Color.TRANSPARENT));
        listView.setDividerHeight(spacing);
        final RearrangeAdapter adapter = new RearrangeAdapter(stocksProvider);
        listView.setAdapter(adapter);
        listView.enableDragAndDrop();
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                listView.startDragging(position);
                return true;
            }
        });
        setContentView(listView);
        showDialog(getString(R.string.drag_instructions));
    }

}
