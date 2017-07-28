package com.soundcloud.android.presentation;

import static android.support.v7.widget.RecyclerView.ViewHolder;

import com.soundcloud.androidkit.R;

import android.view.View;
import android.view.ViewGroup;

public abstract class PagingRecyclerItemAdapter<T, VH extends ViewHolder> extends RecyclerItemAdapter<T, VH>
        implements PagingAwareAdapter<T> {

    private final ProgressCellRenderer progressCellRenderer;

    private AppendState appendState = AppendState.IDLE;

    public PagingRecyclerItemAdapter(CellRenderer<T> cellRenderer) {
        this(cellRenderer, createDefaultProgressCellRenderer());
    }

    public PagingRecyclerItemAdapter(CellRenderer<T> cellRenderer, ProgressCellRenderer progressCellRenderer) {
        super(cellRenderer);
        this.progressCellRenderer = progressCellRenderer;
    }

    public PagingRecyclerItemAdapter(CellRendererBinding<? extends T>... cellRendererBindings) {
        super(cellRendererBindings);
        this.progressCellRenderer = createDefaultProgressCellRenderer();
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == ViewTypes.PROGRESS_VIEW_TYPE) {
            return createViewHolder(progressCellRenderer.createView(parent.getContext()));
        } else {
            return super.onCreateViewHolder(parent, viewType);
        }
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        if (getItemViewType(position) == ViewTypes.PROGRESS_VIEW_TYPE) {
            progressCellRenderer.bindView(holder.itemView, appendState == AppendState.ERROR);
        } else {
            super.onBindViewHolder(holder, position);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (appendState != AppendState.IDLE && position == items.size()) {
            return ViewTypes.PROGRESS_VIEW_TYPE;
        } else {
            return super.getItemViewType(position);
        }
    }

    @Override
    public int getItemCount() {
        if (items.isEmpty()) {
            return 0;
        } else {
            return appendState == AppendState.IDLE ? items.size() : items.size() + 1;
        }
    }

    @Override
    public void setOnErrorRetryListener(View.OnClickListener onErrorRetryListener) {
        progressCellRenderer.setRetryListener(onErrorRetryListener);
    }

    @Override
    public void onError(Throwable e) {
        setNewAppendState(AppendState.ERROR);
        super.onError(e);
    }

    @Override
    public void onNext(Iterable<T> items) {
        setNewAppendState(AppendState.IDLE);
        super.onNext(items);
    }

    @Override
    public void setLoading() {
        setNewAppendState(AppendState.LOADING);
    }

    @Override
    public boolean isIdle() {
        return appendState == AppendState.IDLE;
    }

    public void setNewAppendState(AppendState newState) {
        if (appendState != newState) {
            appendState = newState;
            notifyDataSetChanged();
        }
    }

    private static ProgressCellRenderer createDefaultProgressCellRenderer() {
        return new ProgressCellRenderer(R.layout.ak_list_loading_item);
    }

    public enum AppendState {
        IDLE, LOADING, ERROR
    }
}
