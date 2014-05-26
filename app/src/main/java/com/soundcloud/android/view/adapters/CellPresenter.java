package com.soundcloud.android.view.adapters;

import android.view.View;
import android.view.ViewGroup;

import java.util.List;

public interface CellPresenter<ItemT, ViewT extends View> {

    ViewT createItemView(int position, ViewGroup parent, int itemViewType);
    void bindItemView(int position, ViewT itemView, List<ItemT> items);

}
