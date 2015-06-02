package com.soundcloud.android.view.adapters;

import static com.soundcloud.android.utils.AndroidUtils.assertOnUiThread;

import com.google.common.annotations.VisibleForTesting;
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
        extends RecyclerView.Adapter<VH> implements ItemAdapter<ItemT>, PagingAwareAdapter<ItemT> {

    @VisibleForTesting static final int HEADER_VIEW_TYPE = Integer.MIN_VALUE;
    @VisibleForTesting static final int PROGRESS_VIEW_TYPE = Integer.MIN_VALUE + 1;

    protected static final int DEFAULT_VIEW_TYPE = 0;

    protected final List<ItemT> items;
    protected final SparseArray<CellRenderer<?>> cellRenderers;

    private AppendState appendState = AppendState.IDLE;

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
        this.cellRenderers.put(DEFAULT_VIEW_TYPE, cellRenderer);
    }

    @Override
    public void setOnErrorRetryListener(View.OnClickListener onErrorRetryListener) {
        getProgressCellRenderer().setRetryListener(onErrorRetryListener);
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == HEADER_VIEW_TYPE) {
            return createViewHolder(getHeaderCellRenderer().createView(parent.getContext()));

        } else if (viewType == PROGRESS_VIEW_TYPE) {
            return createViewHolder(getProgressCellRenderer().createView(parent.getContext()));

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

    private boolean usesPaging(){
        return getProgressCellRenderer() != null;
    }

    @Nullable
    protected ProgressCellRenderer getProgressCellRenderer(){
        return null;
    }

    @Override
    public void onBindViewHolder(final VH holder, final int position) {
        final int itemViewType = getItemViewType(position);
        if (itemViewType == PROGRESS_VIEW_TYPE){
            getProgressCellRenderer().bindView(holder.itemView, appendState == AppendState.ERROR);

        } else if (itemViewType != HEADER_VIEW_TYPE) {
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
    public final int getItemViewType(int position) {
        if (position == 0 && usesHeader()) {
            return HEADER_VIEW_TYPE;
        } else if (position == getItemCount() && usesPaging()){
            return PROGRESS_VIEW_TYPE;
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
        setNewAppendState(AppendState.ERROR);
        notifyDataSetChanged();
        e.printStackTrace();
    }

    @Override
    public void onNext(Iterable<ItemT> items) {
        setNewAppendState(AppendState.IDLE);
        for (ItemT item : items) {
            addItem(item);
        }
        notifyDataSetChanged();
    }

    private void setNewAppendState(AppendState newState) {
        assertOnUiThread("Adapter should always be uses on UI Thread. Tracking issue #2377");
        appendState = newState;
    }

    @Override
    public void setLoading() {
        setNewAppendState(AppendState.LOADING);
        notifyDataSetChanged();
    }

    public int adjustPositionForHeader(int adapterPosition) {
        return usesHeader() ? adapterPosition - 1 : adapterPosition;
    }

    @Override
    public boolean isIdle() {
        return appendState == AppendState.IDLE;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View itemView) {
            super(itemView);
        }
    }
}
