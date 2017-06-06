package com.soundcloud.android.view.adapters;

import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.rx.observers.DefaultObserver;

public final class PrependItemToListObserver<T> extends DefaultObserver<T> {
    private final RecyclerItemAdapter<T, ?> adapter;

    public PrependItemToListObserver(RecyclerItemAdapter<T, ?> adapter) {
        this.adapter = adapter;
    }

    @Override
    public void onNext(final T item) {
        adapter.prependItem(item);
        adapter.notifyItemInserted(0);
    }
}
