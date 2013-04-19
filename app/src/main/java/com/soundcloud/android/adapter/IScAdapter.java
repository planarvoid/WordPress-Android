package com.soundcloud.android.adapter;

import com.soundcloud.android.provider.Content;

import android.widget.ListAdapter;

import java.util.List;

public interface IScAdapter<T> extends ListAdapter {

    long getItemId(int position);

    Content getContent();

    /**
     * Either adds the given item to the adapter if it hasn't been added yet, or overwrites an existing equal item
     * with this one.
     */
    void insertItem(T item);

    void addItems(List<T> items);

    void clearData();

    void notifyDataSetChanged();

    //TODO: item click handling does NOT belong in an adapter...
    int handleListItemClick(int position, long id);
}
