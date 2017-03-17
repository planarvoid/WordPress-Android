package com.soundcloud.android.view.collection;

import com.soundcloud.java.checks.Preconditions;

import android.graphics.Rect;
import android.support.annotation.Px;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/***
 * General purpose {@link android.support.v7.widget.RecyclerView.ItemDecoration} subclass that equally add space around elements in a {@link RecyclerView}-based grid.
 * Please note that this decorator works only in conjunction with {@link GridLayoutManager}.
 * <b>In this implementation only elements with span size equals to 1 are taken into account.
 * Elements with span size not equals to one are not touched by this decorator.</b>
 */
public class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {
    private final int itemMargin;
    private final int spanCount;

    /**
     * @param itemMargin The margin between items
     * @param spanCount  The number of columns in the grid. See {@link GridLayoutManager#getSpanCount()}
     */
    public GridSpacingItemDecoration(@Px int itemMargin, int spanCount) {
        Preconditions.checkArgument(itemMargin > 0);
        Preconditions.checkArgument(spanCount > 0);
        this.itemMargin = itemMargin;
        this.spanCount = spanCount;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        Preconditions.checkState(view.getLayoutParams() instanceof GridLayoutManager.LayoutParams);
        GridLayoutManager.LayoutParams layoutParams = (GridLayoutManager.LayoutParams) view.getLayoutParams();
        if (layoutParams.getSpanSize() == 1) {
            int column = layoutParams.getSpanIndex();
            outRect.left = itemMargin - column * itemMargin / spanCount;
            outRect.right = (column + 1) * itemMargin / spanCount;
            outRect.top = itemMargin;
            outRect.bottom = itemMargin;
        } else {
            outRect.setEmpty();
        }
    }
}
