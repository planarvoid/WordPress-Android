package com.soundcloud.android.adapter;

import com.soundcloud.android.provider.Content;
import com.soundcloud.android.view.quickaction.QuickAction;

import android.widget.ListAdapter;

import java.util.List;

public interface IScAdapter<T> extends ListAdapter {

    long getItemId(int position);

    Content getContent();

    QuickAction getQuickActionMenu();

    void addItems(List<T> items);

    void clearData();

    void notifyDataSetChanged();

    //TODO: item click handling does NOT belong in an adapter...
    int handleListItemClick(int position, long id);
}
