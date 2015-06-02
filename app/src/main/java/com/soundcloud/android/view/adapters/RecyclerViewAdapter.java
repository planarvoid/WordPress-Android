package com.soundcloud.android.view.adapters;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public abstract class RecyclerViewAdapter<ItemT, VH extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<VH> implements ItemAdapter<ItemT> {

    protected final List<ItemT> items;
    protected final SparseArray<CellRenderer<?>> cellRenderers;

    protected enum AppendState {
        IDLE, LOADING, ERROR
    }

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
        this.cellRenderers.put(ViewTypes.DEFAULT_VIEW_TYPE, cellRenderer);
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == ViewTypes.HEADER_VIEW_TYPE) {
            return createViewHolder(getHeaderCellRenderer().createView(parent.getContext()));

        } else {
            final View itemView = cellRenderers.get(viewType).createItemView(parent);
            itemView.setOnClickListener(onClickListener);
            itemView.setBackgroundResource(getBackgroundResourceId(parent.getContext()));
            return createViewHolder(itemView);
        }
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

    private boolean usesHeader(){
        return getHeaderCellRenderer() != null;
    }

    @Nullable
    protected HeaderCellRenderer getHeaderCellRenderer(){
        return null;
    }

    @Override
    public void onBindViewHolder(final VH holder, final int position) {
        final int itemViewType = getItemViewType(position);
        if (itemViewType != ViewTypes.HEADER_VIEW_TYPE) {
            cellRenderers.get(getBasicItemViewType(position)).bindItemView(adjustPosition(position), holder.itemView, (List) items);
        }
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
    public int getItemViewType(int position) {
        if (position == 0 && usesHeader()) {
            return ViewTypes.HEADER_VIEW_TYPE;
        } else {
            return getBasicItemViewType(position);
        }
    }

    public abstract int getBasicItemViewType(int position);

    public boolean isEmpty(){
        return items.isEmpty();
    }

    @Override
    public int getItemCount() {
        return usesHeader() ? items.size() + 1 : items.size();
    }

    public ItemT getItem(int position) {
        return items.get(adjustPosition(position));
    }

    public List<ItemT> getItems() {
        return items;
    }

    @Override
    public void removeItem(int position) {
        items.remove(adjustPosition(position));
    }

    private int adjustPosition(int position) {
        return usesHeader() ? position - 1 : position;
    }

    @Override
    public void onCompleted() {
        // no-op by default
    }

    @Override
    public void onError(Throwable e) {
        notifyDataSetChanged();
        e.printStackTrace();
    }

    @Override
    public void onNext(Iterable<ItemT> items) {
        for (ItemT item : items) {
            addItem(item);
        }
        notifyDataSetChanged();
    }

    public int adjustPositionForHeader(int adapterPosition) {
        return usesHeader() ? adapterPosition - 1 : adapterPosition;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View itemView) {
            super(itemView);
        }
    }
}
