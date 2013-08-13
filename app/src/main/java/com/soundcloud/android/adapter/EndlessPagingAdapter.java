package com.soundcloud.android.adapter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.List;

public abstract class EndlessPagingAdapter<T> extends ScAdapter<T> {

    private static final int PROGRESS_ITEM_VIEW_TYPE = 1;
    private final int mProgressItemLayoutResId;
    private boolean mDisplayProgressItem;

    public EndlessPagingAdapter(int pageSize, int progressItemLayoutResId) {
        super(pageSize);
        mProgressItemLayoutResId = progressItemLayoutResId;
    }

    @Override
    public int getCount() {
        return mDisplayProgressItem ? mItems.size() + 1 : mItems.size();
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (getItemViewType(position) == PROGRESS_ITEM_VIEW_TYPE) {
            return convertView != null ? convertView : View.inflate(parent.getContext(), mProgressItemLayoutResId, null);
        } else {
            return super.getView(position, convertView, parent);
        }
    }


    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return mDisplayProgressItem && position == mItems.size() ? PROGRESS_ITEM_VIEW_TYPE : super.getItemViewType(position);
    }

    public void setDisplayProgressItem(boolean showProgressItem) {
        mDisplayProgressItem = showProgressItem;
        notifyDataSetChanged();
    }

    public boolean isDisplayProgressItem() {
        return mDisplayProgressItem;
    }
}
