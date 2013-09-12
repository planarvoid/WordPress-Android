package com.soundcloud.android.view.adapter;

import com.soundcloud.android.R;

import android.view.View;

public class GridSpacer {
    private final int mSpacingOutsideTopBottomResId;
    private final int mSpacingOutsideLeftRightResId;
    private final int mNumColumnsResId;

    public GridSpacer() {
        this(R.integer.suggested_user_grid_num_columns,
                R.dimen.explore_suggested_track_item_spacing_outside_left_right,
                R.dimen.explore_suggested_track_item_spacing_outside_top_bottom);
    }

    public GridSpacer(int numColumnsResId, int spacingOutsideLeftRightResId, int spacingOutsideTopBottomResId) {
        mNumColumnsResId = numColumnsResId;
        mSpacingOutsideLeftRightResId = spacingOutsideLeftRightResId;
        mSpacingOutsideTopBottomResId = spacingOutsideTopBottomResId;
    }

    /**
     * This will configure the edges to have padding that is equivalent to the inner item spacing
     */
    public void configureItemPadding(View convertView, int position, int itemCount) {

        final int numColumns = convertView.getResources().getInteger(mNumColumnsResId);
        final int spacingLeftRight = convertView.getResources().getDimensionPixelSize(mSpacingOutsideLeftRightResId);
        final int spacingTopBottom = convertView.getResources().getDimensionPixelSize(mSpacingOutsideTopBottomResId);

        convertView.setPadding(
                position % numColumns == 0 ? spacingLeftRight : 0,
                position < numColumns ? spacingTopBottom : 0,
                position % numColumns == numColumns - 1 ? spacingLeftRight : 0,
                position >= itemCount - numColumns ? spacingTopBottom : 0
        );
    }
}
