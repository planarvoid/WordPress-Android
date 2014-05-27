package com.soundcloud.android.view.adapters;

import com.soundcloud.android.Consts;

import android.os.Bundle;
import android.os.Parcelable;
import android.util.SparseArray;
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
public class ItemAdapter<ItemT extends Parcelable> extends BaseAdapter {

    protected static final String EXTRA_KEY_ITEMS = "adapter.items";

    protected ArrayList<ItemT> items;
    protected final SparseArray<CellPresenter<ItemT>> cellPresenters;

    public ItemAdapter(CellPresenter<ItemT>... cellPresenters) {
        this.items = new ArrayList<ItemT>(Consts.LIST_PAGE_SIZE);
        this.cellPresenters = new SparseArray<CellPresenter<ItemT>>(cellPresenters.length);
        for (CellPresenter<ItemT> presenter : cellPresenters) {
            this.cellPresenters.put(presenter.getItemViewType(), presenter);
        }
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
        final CellPresenter<ItemT> presenter = getItemPresenter(getItemViewType(position));

        View itemView;
        if (convertView == null) {
            itemView = presenter.createItemView(position, parent);
        } else {
            itemView = convertView;
        }
        presenter.bindItemView(position, itemView, items);
        return itemView;
    }

    private CellPresenter<ItemT> getItemPresenter(int itemViewType) {
        CellPresenter<ItemT> presenter = cellPresenters.get(itemViewType);
        if (presenter == null) {
            throw new IllegalStateException("No presenter registered for item view type : " + itemViewType);
        }
        return presenter;
    }

    @Override
    public int getItemViewType(int position) {
        // we fix this to our default view type constant since adapters and presenters use our constant, not the value
        // Android provides by default (and which doesn't have a symbolic name)
        return CellPresenter.DEFAULT_ITEM_VIEW_TYPE;
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
