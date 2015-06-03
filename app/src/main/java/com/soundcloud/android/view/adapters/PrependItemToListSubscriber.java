package com.soundcloud.android.view.adapters;

import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.ListItemAdapter;
import com.soundcloud.android.rx.observers.DefaultSubscriber;

public final class PrependItemToListSubscriber<T extends ListItem> extends DefaultSubscriber<T> {
    private final ListItemAdapter<T> adapter;

    public PrependItemToListSubscriber(ListItemAdapter<T> adapter) {
        this.adapter = adapter;
    }

    @Override
    public void onNext(final T item) {
        adapter.prependItem(item);
        adapter.notifyDataSetChanged();
    }
}
