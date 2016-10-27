package com.soundcloud.android.view.adapters;

import com.soundcloud.android.presentation.ItemAdapter;
import com.soundcloud.android.rx.observers.DefaultSubscriber;

public final class PrependItemToListSubscriber<T> extends DefaultSubscriber<T> {
    private final ItemAdapter<T> adapter;

    public PrependItemToListSubscriber(ItemAdapter<T> adapter) {
        this.adapter = adapter;
    }

    @Override
    public void onNext(final T item) {
        adapter.prependItem(item);
        adapter.notifyDataSetChanged();
    }
}
