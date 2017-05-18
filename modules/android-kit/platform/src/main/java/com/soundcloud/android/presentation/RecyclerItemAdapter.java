package com.soundcloud.android.presentation;

import com.soundcloud.androidkit.R;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public abstract class RecyclerItemAdapter<ItemT, VH extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<VH> implements ItemAdapter<ItemT> {

    protected final List<ItemT> items;
    protected final SparseArray<CellRenderer<?>> cellRenderers;

    private final Rect paddingHolder = new Rect();

    @Nullable private View.OnClickListener onClickListener;
    private int backgroundResId;

    protected RecyclerItemAdapter(CellRendererBinding<? extends ItemT>... cellRendererBindings) {
        this.items = new ArrayList<>();
        this.cellRenderers = new SparseArray<>(cellRendererBindings.length);
        for (CellRendererBinding<? extends ItemT> entity : cellRendererBindings) {
            this.cellRenderers.put(entity.itemViewType, entity.cellRenderer);
        }
    }

    protected RecyclerItemAdapter(CellRenderer<? extends ItemT> cellRenderer) {
        this.items = new ArrayList<>();
        this.cellRenderers = new SparseArray<>(1);
        this.cellRenderers.put(ViewTypes.DEFAULT_VIEW_TYPE, cellRenderer);
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        final View itemView = cellRenderers.get(viewType).createItemView(parent);
        if (onClickListener != null) {
            itemView.setOnClickListener(onClickListener);
            setBackgroundWithPadding(itemView, getBackgroundResourceId(parent.getContext()));
        }
        return createViewHolder(itemView);
    }

    private void setBackgroundWithPadding(View itemView, int backgroundResourceId) {
        // preserve padding for KitKat, which throws it out when setting background
        paddingHolder.set(itemView.getPaddingLeft(), itemView.getPaddingTop(),
                itemView.getPaddingRight(), itemView.getPaddingBottom());
        itemView.setBackgroundResource(backgroundResourceId);
        itemView.setPadding(paddingHolder.left, paddingHolder.top, paddingHolder.right, paddingHolder.bottom);
    }

    private int getBackgroundResourceId(Context context) {
        // lazy init of backgroundResId to avoid unnecessary object creation
        if (backgroundResId == 0) {
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
        cellRenderers.get(getBasicItemViewType(position)).bindItemView(position, holder.itemView, (List) items);
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
        return getBasicItemViewType(position);
    }

    public abstract int getBasicItemViewType(int position);

    public boolean isEmpty() {
        return items.isEmpty();
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
    public void prependItem(ItemT item) {
        items.add(0, item);
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

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View itemView) {
            super(itemView);
        }
    }
}
