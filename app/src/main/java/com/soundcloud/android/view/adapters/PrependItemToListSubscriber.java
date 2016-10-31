package com.soundcloud.android.view.adapters;

import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.rx.observers.DefaultSubscriber;

public final class PrependItemToListSubscriber<T> extends DefaultSubscriber<T> {
    private final RecyclerItemAdapter<T,?> adapter;

    public PrependItemToListSubscriber(RecyclerItemAdapter<T,?> adapter) {
        this.adapter = adapter;
    }

    @Override
    public void onNext(final T item) {
        adapter.prependItem(item);
        adapter.notifyItemInserted(0);
    }
}
