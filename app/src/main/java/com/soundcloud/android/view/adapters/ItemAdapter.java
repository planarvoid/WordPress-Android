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
 * Keep this class lean and clean: it provides basic adapter functionality around a list of items, that's it.
 */
public class ItemAdapter<ItemT> extends BaseAdapter implements ReactiveItemAdapter<ItemT> {
    protected static final int DEFAULT_VIEW_TYPE = 0;

    protected final List<ItemT> items;
    protected final SparseArray<CellPresenter<?>> cellPresenters;

    public ItemAdapter(CellPresenterBinding<? extends ItemT>... cellPresenterBindings) {
        this.items = new ArrayList<>(Consts.LIST_PAGE_SIZE);
        this.cellPresenters = new SparseArray<>(cellPresenterBindings.length);
        for (CellPresenterBinding<?> entity : cellPresenterBindings) {
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
    public int getItemCount() {
        return items.size();
    }

    @Override
    public int getCount() {
        return getItemCount();
    }

    @Override
    public ItemT getItem(int location) {
        return items.get(location);
    }

    public List<ItemT> getItems() {
        return items;
    }

    public void prependItem(ItemT item) {
        items.add(0, item);
    }

    @Override
    public void addItem(ItemT item) {
        items.add(item);
    }

    public void removeItem(int position) {
        items.remove(position);
    }

    @Override
    public void clear() {
        items.clear();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final CellPresenter<?> presenter = cellPresenters.get(getItemViewType(position));
        View itemView = convertView;
        if (itemView == null) {
            itemView = presenter.createItemView(parent);
        }
        presenter.bindItemView(position, itemView, (List) items);
        return itemView;
    }

    @Override
    public void onCompleted() {
    }

    @Override
    public void onError(Throwable e) {
        e.printStackTrace();
    }

    @Override
    public void onNext(Iterable<ItemT> items) {
        for (ItemT item : items) {
            addItem(item);
        }
        notifyDataSetChanged();
    }

}
