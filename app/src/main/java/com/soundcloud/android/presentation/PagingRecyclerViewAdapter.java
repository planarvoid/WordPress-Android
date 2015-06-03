package com.soundcloud.android.presentation;

import static android.support.v7.widget.RecyclerView.ViewHolder;
import static com.soundcloud.android.utils.AndroidUtils.assertOnUiThread;

import com.soundcloud.android.R;

import android.view.View;
import android.view.ViewGroup;

public abstract class PagingRecyclerViewAdapter<T, VH extends ViewHolder> extends RecyclerViewAdapter<T, VH>
        implements PagingAwareAdapter<T> {

    private final ProgressCellRenderer progressCellRenderer;

    private AppendState appendState = AppendState.IDLE;

    public PagingRecyclerViewAdapter(CellRenderer<T> cellRenderer) {
        this(cellRenderer, createDefaultProgressCellRenderer());
    }

    public PagingRecyclerViewAdapter(CellRenderer<T> cellRenderer, ProgressCellRenderer progressCellRenderer) {
        super(cellRenderer);
        this.progressCellRenderer = progressCellRenderer;
    }

    public PagingRecyclerViewAdapter(CellRendererBinding<? extends T>... cellRendererBindings) {
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
        if (getItemViewType(position) == ViewTypes.PROGRESS_VIEW_TYPE){
            progressCellRenderer.bindView(holder.itemView, appendState == AppendState.ERROR);
        } else {
            super.onBindViewHolder(holder, position);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position == getItemCount()){
            return ViewTypes.PROGRESS_VIEW_TYPE;
        } else {
            return super.getItemViewType(position);
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

    private void setNewAppendState(AppendState newState) {
        assertOnUiThread("Adapter should always be uses on UI Thread. Tracking issue #2377");
        appendState = newState;
    }

    private static ProgressCellRenderer createDefaultProgressCellRenderer() {
        return new ProgressCellRenderer(R.layout.list_loading_item);
    }
}
