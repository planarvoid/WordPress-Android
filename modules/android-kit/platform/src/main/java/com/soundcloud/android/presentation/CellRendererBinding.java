package com.soundcloud.android.presentation;

public class CellRendererBinding<ItemT>  {
    final int itemViewType;
    final CellRenderer<ItemT> cellRenderer;

    public CellRendererBinding(int itemViewType, CellRenderer<ItemT> cellRenderer) {
        this.itemViewType = itemViewType;
        this.cellRenderer = cellRenderer;
    }
}
