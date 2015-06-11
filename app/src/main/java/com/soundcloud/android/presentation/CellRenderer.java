package com.soundcloud.android.presentation;

import android.view.View;
import android.view.ViewGroup;

import java.util.List;

public interface CellRenderer<ItemT> {

    View createItemView(ViewGroup parent);
    void bindItemView(int position, View itemView, List<ItemT> items);

}
