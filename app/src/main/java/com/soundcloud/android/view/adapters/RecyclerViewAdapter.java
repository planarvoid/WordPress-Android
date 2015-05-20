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
    protected final SparseArray<CellPresenter<?>> cellPresenters;

    private View.OnClickListener onClickListener;

    @SafeVarargs
    protected RecyclerViewAdapter(CellPresenterBinding<? extends ItemT>... cellPresenterBindings) {
        this.items = new ArrayList<>(Consts.LIST_PAGE_SIZE);
        this.cellPresenters = new SparseArray<>(cellPresenterBindings.length);
        for (CellPresenterBinding<? extends ItemT> entity : cellPresenterBindings) {
            this.cellPresenters.put(entity.itemViewType, entity.cellPresenter);
        }
    }

    protected RecyclerViewAdapter(CellPresenter<? extends ItemT> cellPresenter) {
        this.items = new ArrayList<>(Consts.LIST_PAGE_SIZE);
        this.cellPresenters = new SparseArray<>(1);
        this.cellPresenters.put(DEFAULT_VIEW_TYPE, cellPresenter);
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        final View itemView = cellPresenters.get(viewType).createItemView(parent);
        itemView.setOnClickListener(onClickListener);
        return createViewHolder(itemView);
    }

    public void setOnItemClickListener(View.OnClickListener itemClickListener) {
        this.onClickListener = itemClickListener;
    }

    protected abstract VH createViewHolder(View itemView);

    @Override
    public void onBindViewHolder(final VH holder, final int position) {
        cellPresenters.get(getItemViewType(position)).bindItemView(position, holder.itemView, (List) items);
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

    public ItemT getItem(int position) {
        return items.get(position);
    }

    public List<ItemT> getItems() {
        return items;
    }

    @Override
    public void removeItem(int position) {
        items.remove(position);
    }

    @Override
    public void onCompleted() {
        // no-op by default
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
