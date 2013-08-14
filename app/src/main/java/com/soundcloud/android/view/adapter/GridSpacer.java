package com.soundcloud.android.view.adapter;

import android.view.View;

public class GridSpacer {

    final private int mGridSpacingLeftRight;
    final private int mGridSpacingTopBottom;
    final private int mNumColumns;

    public GridSpacer(int gridSpacingLeftRight, int gridSpacingTopBottom, int numColumns) {
        this.mGridSpacingLeftRight = gridSpacingLeftRight;
        this.mGridSpacingTopBottom = gridSpacingTopBottom;
        this.mNumColumns = numColumns;
    }

    /**
     * This will configure the edges to have padding that is equivalent to the inner item spacing
     */
    public void configureItemPadding(View convertView, int position, int itemCount) {
        convertView.setPadding(
                position % mNumColumns == 0 ? mGridSpacingLeftRight : 0,
                position < mNumColumns ? mGridSpacingTopBottom : 0,
                position % mNumColumns == mNumColumns - 1 ? mGridSpacingLeftRight : 0,
                position >= itemCount - mNumColumns ? mGridSpacingTopBottom : 0
        );
    }
}
