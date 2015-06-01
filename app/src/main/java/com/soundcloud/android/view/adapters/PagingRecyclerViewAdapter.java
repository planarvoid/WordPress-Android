package com.soundcloud.android.view.adapters;

import static android.support.v7.widget.RecyclerView.ViewHolder;

import com.soundcloud.android.R;

import android.view.View;

public abstract class PagingRecyclerViewAdapter<T, VH extends ViewHolder> extends RecyclerViewAdapter<T, VH>
        implements PagingAwareAdapter<T> {

    private ProgressCellRenderer progressCellRenderer;

    public PagingRecyclerViewAdapter(CellRenderer<T> CellRenderer) {
        this(CellRenderer, createDefaultProgressCellRenderer());
    }

    public PagingRecyclerViewAdapter(CellRenderer<T> CellRenderer, ProgressCellRenderer progressCellRenderer) {
        super(CellRenderer);
        this.progressCellRenderer = progressCellRenderer;
    }

    public PagingRecyclerViewAdapter(CellRendererBinding<? extends T>... CellRendererEntities) {
        super(CellRendererEntities);
        this.progressCellRenderer = createDefaultProgressCellRenderer();
    }

    @Override
    protected final ProgressCellRenderer getProgressCellRenderer() {
        return progressCellRenderer;
    }

    private static ProgressCellRenderer createDefaultProgressCellRenderer() {
        return new ProgressCellRenderer(R.layout.list_loading_item);
    }
}
