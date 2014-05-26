package com.soundcloud.android.view.adapters;

import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * A better version of Android's ArrayAdapter. It also works on ArrayLists internally, but does not hold references
 * to Context and so is safe to be used in retained fragments. It forwards cell rendering to the given
 * {@link com.soundcloud.android.view.adapters.CellPresenter}
 *
 * Keep this class lean and clean: it provides basic adapter functionality around a list of parcelables, that's it.
 */
public class ItemAdapter<ItemT extends Parcelable, ViewT extends View> extends BaseAdapter {

    public static final int DEFAULT_ITEM_VIEW_TYPE = 0;

    protected static final String EXTRA_KEY_ITEMS = "adapter.items";

    protected ArrayList<ItemT> items;
    protected CellPresenter<ItemT, ViewT> cellPresenter;

    public ItemAdapter(CellPresenter<ItemT, ViewT> cellPresenter, int initalDataSize) {
        this.cellPresenter = cellPresenter;
        this.items = new ArrayList<ItemT>(initalDataSize);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public ItemT getItem(int location) {
        return items.get(location);
    }

    public List<ItemT> getItems() {
        return items;
    }

    public void addItem(ItemT item) {
        items.add(item);
    }

    public void clear() {
        items.clear();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewT itemView;
        final int itemViewType = getItemViewType(position);
        if (convertView == null) {
            itemView = cellPresenter.createItemView(position, parent, itemViewType);
        } else {
            itemView = (ViewT) convertView;
        }
        cellPresenter.bindItemView(position, itemView, items);
        return itemView;
    }

    /**
     * Saves this adapter's state to the given bundle. Always pair this with a call to
     * {@link #restoreInstanceState(android.os.Bundle)}
     */
    public void saveInstanceState(Bundle bundle) {
        bundle.putParcelableArrayList(EXTRA_KEY_ITEMS, items);
    }

    /**
     * Restores this adapter's state from the given bundle. Always pair this with a call to
     * {@link #saveInstanceState(android.os.Bundle)}
     */
    public void restoreInstanceState(Bundle bundle) {
        items = bundle.getParcelableArrayList(EXTRA_KEY_ITEMS);
    }
}
