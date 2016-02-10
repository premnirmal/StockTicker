package com.github.premnirmal.ticker.ui;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Created by premnirmal on 2/10/16.
 */
public class SpacingDecoration extends RecyclerView.ItemDecoration {

    private final int spacing;

    public SpacingDecoration(int spacing) {
        this.spacing = spacing;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        outRect.bottom = spacing;
        outRect.top = spacing;
        outRect.left = spacing;
        outRect.right = spacing;
    }
}