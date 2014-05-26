package com.soundcloud.android.view.adapters;

import android.view.View;
import android.view.ViewGroup;

import java.util.List;

public interface CellPresenter<ItemT> {

    View createItemView(int position, ViewGroup parent, int itemViewType);
    void bindItemView(int position, View itemView, int itemViewType, List<ItemT> items);

}
