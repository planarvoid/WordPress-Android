package com.soundcloud.android.collection;

import android.graphics.Rect;
import android.support.v7.widget.GridLayoutManager.LayoutParams;
import android.support.v7.widget.RecyclerView;
import android.view.View;

class CollectionItemDecoration extends RecyclerView.ItemDecoration {

    private final int spacing;

    CollectionItemDecoration(int spacing) {
        this.spacing = spacing;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();

        if (layoutParams.getSpanSize() == 1) {
            outRect.left = 0;
            outRect.right = spacing;
            outRect.bottom = spacing;
            outRect.top = spacing;
        } else {
            outRect.left = -spacing;
            outRect.right = 0;
            outRect.bottom = 0;
            outRect.top = 0;
        }
    }
}
