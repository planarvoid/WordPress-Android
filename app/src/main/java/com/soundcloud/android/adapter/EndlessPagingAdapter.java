package com.soundcloud.android.adapter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.List;

public abstract class EndlessPagingAdapter<T> extends BaseAdapter implements ItemAdapter<T> {

    private static final int PROGRESS_ITEM_VIEW_TYPE = 1;

    private final List<T> mItems;
    private final int mProgressItemLayoutResId;
    private boolean mIsLoading;


    public EndlessPagingAdapter(int pageSize, int progressItemLayoutResId) {
        mItems = new ArrayList<T>(pageSize);
        mProgressItemLayoutResId = progressItemLayoutResId;
    }

    @Override
    public int getCount() {
        return mIsLoading ? mItems.size() + 1 : mItems.size();
    }

    @Override
    public T getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public void addItem(T item) {
        mItems.add(item);
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (getItemViewType(position) == PROGRESS_ITEM_VIEW_TYPE) {
            return convertView != null ? convertView : View.inflate(parent.getContext(), mProgressItemLayoutResId, null);
        } else if (convertView == null) {
            convertView = createItemView(position, parent);
        }

        bindItemView(position, convertView);

        return convertView;
    }

    protected abstract void bindItemView(int position, View itemView);

    protected abstract View createItemView(int position, ViewGroup parent);

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return mIsLoading && position == mItems.size() ? PROGRESS_ITEM_VIEW_TYPE : super.getItemViewType(position);
    }

    public void setLoading(boolean loading) {
        mIsLoading = loading;
        notifyDataSetChanged();
    }

    public boolean shouldLoadNextPage(int firstVisibleItem, int visibleItemCount, int totalItemCount) {

        // if loading, subtract the loading item from total count
        int lookAheadSize = visibleItemCount * 2;
        int itemCount = mIsLoading ? totalItemCount - 1 : totalItemCount; // size without the loading spinner
        boolean lastItemReached = itemCount > 0 && (itemCount - lookAheadSize <= firstVisibleItem);

        return !mIsLoading && lastItemReached;
    }

}
