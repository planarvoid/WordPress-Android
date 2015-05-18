package com.soundcloud.android.view.adapters;

import com.soundcloud.android.Consts;

import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public abstract class RecyclerViewAdapter<ItemT, VH extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<VH> implements ReactiveItemAdapter<ItemT> {

    protected static final int DEFAULT_VIEW_TYPE = 0;

    protected final List<ItemT> items;
    protected final SparseArray<CellPresenter<ItemT>> cellPresenters;

    @SafeVarargs
    protected RecyclerViewAdapter(CellPresenterBinding<ItemT>... cellPresenterBindings) {
        this.items = new ArrayList<>(Consts.LIST_PAGE_SIZE);
        this.cellPresenters = new SparseArray<>(cellPresenterBindings.length);
        for (CellPresenterBinding<ItemT> entity : cellPresenterBindings) {
            this.cellPresenters.put(entity.itemViewType, entity.cellPresenter);
        }
    }

    protected RecyclerViewAdapter(CellPresenter<ItemT> cellPresenter) {
        this.items = new ArrayList<>(Consts.LIST_PAGE_SIZE);
        this.cellPresenters = new SparseArray<>(1);
        this.cellPresenters.put(DEFAULT_VIEW_TYPE, cellPresenter);
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        return createViewHolder(cellPresenters.get(viewType).createItemView(parent));
    }

    protected abstract VH createViewHolder(View itemView);

    @Override
    public void onBindViewHolder(VH holder, int position) {
        cellPresenters.get(getItemViewType(position)).bindItemView(position, holder.itemView, items);
    }

    @Override
    public void addItem(ItemT item) {
        items.add(item);
    }

    @Override
    public void clear() {
        items.clear();
    }

    @Override
    public int getItemCount() {
        return items.size();
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
