package com.soundcloud.android.collections;

import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;

/**
 * A better version of Android's ArrayAdapter. It also works on ArrayLists internally, but does not hold references
 * to Context and so is safe to be used in retained fragments. it also supports view recycling via
 * {@link #createItemView(int, android.view.ViewGroup)} and {@link #bindItemView(int, android.view.View)}.
 *
 * Keep this class lean and clean: it provides basic adapter functionality around a list of parcelables, that's it.
 */
public abstract class ItemAdapter<ItemT extends Parcelable> extends BaseAdapter {

    protected static final String EXTRA_KEY_ITEMS = "adapter.items";

    protected ArrayList<ItemT> mItems;

    protected ItemAdapter(int initalDataSize) {
        mItems = new ArrayList<ItemT>(initalDataSize);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public ItemT getItem(int location) {
        return mItems.get(location);
    }

    public void addItem(ItemT item) {
        mItems.add(item);
    }

    public void clear() {
        mItems.clear();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = createItemView(position, parent);
        }
        bindItemView(position, convertView);
        return convertView;
    }

    /**
     * Saves this adapter's state to the given bundle. Always pair this with a call to
     * {@link #restoreInstanceState(android.os.Bundle)}
     */
    public void saveInstanceState(Bundle bundle) {
        bundle.putParcelableArrayList(EXTRA_KEY_ITEMS, mItems);
    }

    /**
     * Restores this adapter's state from the given bundle. Always pair this with a call to
     * {@link #saveInstanceState(android.os.Bundle)}
     */
    public void restoreInstanceState(Bundle bundle) {
        mItems = bundle.getParcelableArrayList(EXTRA_KEY_ITEMS);
    }

    protected abstract View createItemView(int position, ViewGroup parent);
    protected abstract void bindItemView(int position, View itemView);

}
