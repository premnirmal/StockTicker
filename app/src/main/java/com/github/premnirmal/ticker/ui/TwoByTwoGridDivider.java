package com.github.premnirmal.ticker.ui;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Created by premnirmal on 3/8/15.
 */
public class TwoByTwoGridDivider extends RecyclerView.ItemDecoration {

    private final int space;

    public TwoByTwoGridDivider(int space) {
        this.space = space;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        final int position = parent.getChildPosition(view);
        if (position % 2 == 0) {
            outRect.left = space;
            outRect.right = space / 2;
        } else {
            outRect.left = space / 2;
            outRect.right = space;
        }
        outRect.bottom = space;
        if (position == 0 || position == 1) {
            outRect.top = space;
        } else {
            outRect.top = 0;
        }
    }
}