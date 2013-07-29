package com.soundcloud.android.adapter;

import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public abstract class SpacedGridAdapter extends BaseAdapter {

    private int mGridSpacingLeftRight = Integer.MIN_VALUE;
    private int mGridSpacingTopBottom = Integer.MIN_VALUE;
    private int mNumColumns = Integer.MIN_VALUE;

    @Override
    final public View getView(int position, View convertView, ViewGroup parent) {
        View view = getGridItem(position, convertView, parent);
        configureItemPadding(view, position);
        return view;
    }

    protected abstract View getGridItem(int position, View convertView, ViewGroup parent);
    protected abstract int getNumColumns(Resources resources);
    protected abstract int getItemSpacingTopBottom(Resources resources);
    protected abstract int getItemSpacingLeftRight(Resources resources);

    /**
     * This will configure the edges to have padding that is equivalent to the inner item spacing
     */
    private void configureItemPadding(View convertView, int position) {
        initResourceValues(convertView.getResources());
        convertView.setPadding(
                position % mNumColumns == 0 ? mGridSpacingLeftRight : 0,
                position < mNumColumns ? mGridSpacingTopBottom : 0,
                position % mNumColumns == mNumColumns - 1 ? mGridSpacingLeftRight : 0,
                position >= getCount() - mNumColumns ? mGridSpacingTopBottom : 0
        );
    }

    private void initResourceValues(Resources resources) {
        if (mGridSpacingLeftRight == Integer.MIN_VALUE){
            mGridSpacingLeftRight = getItemSpacingLeftRight(resources);
        }
        if (mGridSpacingTopBottom == Integer.MIN_VALUE){
            mGridSpacingTopBottom = getItemSpacingTopBottom(resources);
        }
        if (mNumColumns == Integer.MIN_VALUE){
            mNumColumns = getNumColumns(resources);
        }
    }
}
