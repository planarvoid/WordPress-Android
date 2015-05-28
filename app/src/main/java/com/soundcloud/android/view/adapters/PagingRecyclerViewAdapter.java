package com.soundcloud.android.view.adapters;

import static android.support.v7.widget.RecyclerView.ViewHolder;
import static com.soundcloud.android.utils.AndroidUtils.assertOnUiThread;

import android.view.View;
import android.view.ViewGroup;

public abstract class PagingRecyclerViewAdapter<T, VH extends ViewHolder> extends RecyclerViewAdapter<T, VH>
        implements PagingAwareAdapter<T> {

    private final ProgressCellRenderer progressCellRenderer;
    private AppendState appendState = AppendState.IDLE;

    protected enum AppendState {
        IDLE, LOADING, ERROR
    }

    public PagingRecyclerViewAdapter(ProgressCellRenderer progressCellRenderer, CellRenderer<T> cellRenderer) {
        super(cellRenderer);
        this.progressCellRenderer = progressCellRenderer;
    }

    public PagingRecyclerViewAdapter(ProgressCellRenderer progressCellRenderer, CellRendererBinding<? extends T>... cellPresenterEntities) {
        super(cellPresenterEntities);
        this.progressCellRenderer = progressCellRenderer;
    }

    @Override
    public void setOnErrorRetryListener(View.OnClickListener onErrorRetryListener) {
        progressCellRenderer.setRetryListener(onErrorRetryListener);
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
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == PROGRESS_VIEW_TYPE) {
            return createViewHolder(progressCellRenderer.createView(parent.getContext()));
        } else {
            return super.onCreateViewHolder(parent, viewType);
        }
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        if (getItemViewType(position) == PROGRESS_VIEW_TYPE) {
            progressCellRenderer.bindView(holder.itemView, appendState == AppendState.ERROR);
        } else {
            super.onBindViewHolder(holder, position);
        }
    }

    private void setNewAppendState(AppendState newState) {
        assertOnUiThread("Adapter should always be uses on UI Thread. Tracking issue #2377");
        appendState = newState;
    }

    @Override
    public int getItemViewType(int position) {
        return appendState != AppendState.IDLE && position == items.size() ? PROGRESS_VIEW_TYPE
                : super.getItemViewType(position);
    }

    @Override
    public void setLoading() {
        setNewAppendState(AppendState.LOADING);
        notifyDataSetChanged();
    }

    @Override
    public boolean isIdle() {
        return appendState == AppendState.IDLE;
    }

    @Override
    public void onError(Throwable e) {
        super.onError(e);
        setNewAppendState(AppendState.ERROR);
        notifyDataSetChanged();
    }

    @Override
    public void onNext(Iterable<T> items) {
        setNewAppendState(AppendState.IDLE);
        super.onNext(items);
    }


}
