package com.soundcloud.android.view.adapters;

import rx.Observer;

import java.util.List;

public interface ReactiveItemAdapter<ItemT> extends Observer<Iterable<ItemT>> {

    void addItem(ItemT item);

    List<ItemT> getItems();

    int getItemCount();

    ItemT getItem(int position);

    void removeItem(int position);

    void clear();

    void notifyDataSetChanged();
}
