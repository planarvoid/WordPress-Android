package com.soundcloud.android.view.adapters;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public abstract class RecyclerViewAdapter<ItemT, VH extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<VH> implements ItemAdapter<ItemT> {

    protected static final int DEFAULT_VIEW_TYPE = 0;

    protected final List<ItemT> items;
    protected final SparseArray<CellRenderer<?>> cellRenderers;

    private View.OnClickListener onClickListener;
    private int backgroundResId = Consts.NOT_SET;

    protected RecyclerViewAdapter(CellRendererBinding<? extends ItemT>... cellRendererBindings) {
        this.items = new ArrayList<>(Consts.LIST_PAGE_SIZE);
        this.cellRenderers = new SparseArray<>(cellRendererBindings.length);
        for (CellRendererBinding<? extends ItemT> entity : cellRendererBindings) {
            this.cellRenderers.put(entity.itemViewType, entity.cellRenderer);
        }
    }

    protected RecyclerViewAdapter(CellRenderer<? extends ItemT> cellRenderer) {
        this.items = new ArrayList<>(Consts.LIST_PAGE_SIZE);
        this.cellRenderers = new SparseArray<>(1);
        this.cellRenderers.put(DEFAULT_VIEW_TYPE, cellRenderer);
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        final View itemView = cellRenderers.get(viewType).createItemView(parent);
        itemView.setOnClickListener(onClickListener);
        itemView.setBackgroundResource(getBackgroundResourceId(parent.getContext()));
        return createViewHolder(itemView);
    }

    private int getBackgroundResourceId(Context context) {
        // lazy init of backgroundResId to avoid unnecessary object creation
        if (backgroundResId == Consts.NOT_SET){
            TypedValue typedValue = new TypedValue();
            context.getTheme().resolveAttribute(R.attr.selectableItemBackground, typedValue, true);
            backgroundResId = typedValue.resourceId;
        }
        return backgroundResId;
    }

    public void setOnItemClickListener(View.OnClickListener itemClickListener) {
        this.onClickListener = itemClickListener;
    }

    protected abstract VH createViewHolder(View itemView);

    @Override
    public void onBindViewHolder(final VH holder, final int position) {
        cellRenderers.get(getItemViewType(position)).bindItemView(position, holder.itemView, (List) items);
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
