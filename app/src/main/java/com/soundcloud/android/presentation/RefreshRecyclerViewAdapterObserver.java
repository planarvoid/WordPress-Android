package com.soundcloud.android.presentation;

import com.soundcloud.android.rx.observers.DefaultObserver;

import android.support.v7.widget.RecyclerView;

public class RefreshRecyclerViewAdapterObserver extends DefaultObserver<Object> {
    private final RecyclerView.Adapter adapter;

    public RefreshRecyclerViewAdapterObserver(RecyclerView.Adapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public void onNext(Object ignored) {
        adapter.notifyDataSetChanged();
    }
}
