package com.soundcloud.android.view.adapters;

import android.view.View;
import android.view.ViewGroup;

import java.util.List;

public interface CellPresenter<ItemT> {

    View createItemView(ViewGroup parent);
    void bindItemView(int position, View itemView, List<ItemT> items);

}
