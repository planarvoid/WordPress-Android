package com.soundcloud.android.view.adapters;

import android.view.View;
import android.view.ViewGroup;

import java.util.List;

public interface CellPresenter<ItemT> {

    int DEFAULT_ITEM_VIEW_TYPE = 0;

    View createItemView(int position, ViewGroup parent);
    void bindItemView(int position, View itemView, List<ItemT> items);
    int getItemViewType();

}
