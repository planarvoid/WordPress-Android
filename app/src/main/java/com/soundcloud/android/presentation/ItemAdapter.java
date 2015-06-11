package com.soundcloud.android.presentation;

import rx.Observer;

import java.util.List;

public interface ItemAdapter<ItemT> extends Observer<Iterable<ItemT>> {

    void addItem(ItemT item);

    List<ItemT> getItems();

    int getItemCount();

    ItemT getItem(int position);

    boolean isEmpty();

    void removeItem(int position);

    void clear();

    void notifyDataSetChanged();
}
