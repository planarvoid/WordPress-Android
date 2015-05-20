package com.soundcloud.android.view.adapters;

public class CellPresenterBinding<ItemT>  {
    final int itemViewType;
    final CellPresenter<ItemT> cellPresenter;

    public CellPresenterBinding(int itemViewType, CellPresenter<ItemT> cellPresenter) {
        this.itemViewType = itemViewType;
        this.cellPresenter = cellPresenter;
    }
}
