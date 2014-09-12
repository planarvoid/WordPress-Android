package com.soundcloud.android.view.adapters;

import com.soundcloud.android.Consts;

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
public class ItemAdapter<ItemT> extends BaseAdapter {
    protected static final int DEFAULT_VIEW_TYPE = 0;

    protected ArrayList<ItemT> items;
    protected final SparseArray<CellPresenter<ItemT>> cellPresenters;

    public ItemAdapter(CellPresenterEntity<ItemT>... cellPresenterEntities) {
        this.items = new ArrayList<>(Consts.LIST_PAGE_SIZE);
        this.cellPresenters = new SparseArray<>(cellPresenterEntities.length);
        for (CellPresenterEntity<ItemT> entity : cellPresenterEntities) {
            this.cellPresenters.put(entity.itemViewType, entity.cellPresenter);
        }
    }

    public ItemAdapter(CellPresenter<ItemT> cellPresenter) {
        this.items = new ArrayList<>(Consts.LIST_PAGE_SIZE);
        this.cellPresenters = new SparseArray<>(1);
        this.cellPresenters.put(DEFAULT_VIEW_TYPE, cellPresenter);
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
        final CellPresenter<ItemT> presenter = cellPresenters.get(getItemViewType(position));
        View itemView = convertView;
        if (itemView == null) {
            itemView = presenter.createItemView(position, parent);
        }
        presenter.bindItemView(position, itemView, items);
        return itemView;
    }

    public static class CellPresenterEntity<ItemT>  {
        private final int itemViewType;
        private final CellPresenter<ItemT> cellPresenter;

        public CellPresenterEntity(int itemViewType, CellPresenter<ItemT> cellPresenter) {
            this.itemViewType = itemViewType;
            this.cellPresenter = cellPresenter;
        }
    }
}
