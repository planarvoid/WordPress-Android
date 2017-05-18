package com.soundcloud.android.presentation;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

class InsetDividerDecoration extends RecyclerView.ItemDecoration {

    private int leftRightInset;
    private int topBottomInset;

    public InsetDividerDecoration(int leftRightInset, int topBottomInset) {
        this.leftRightInset = leftRightInset;
        this.topBottomInset = topBottomInset;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        //We can supply forced insets for each item view here in the Rect
        outRect.set(leftRightInset, topBottomInset, leftRightInset, topBottomInset);
    }
}
