package com.soundcloud.android.rx.observers;

import android.support.v7.widget.RecyclerView;

public class RefreshRecyclerViewAdapterSubscriber extends DefaultSubscriber<Object> {
    private final RecyclerView.Adapter adapter;

    public RefreshRecyclerViewAdapterSubscriber(RecyclerView.Adapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public void onNext(Object ignored) {
        adapter.notifyDataSetChanged();
    }
}
