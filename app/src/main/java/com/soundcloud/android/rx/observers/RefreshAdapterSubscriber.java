package com.soundcloud.android.rx.observers;

import android.widget.BaseAdapter;

public class RefreshAdapterSubscriber extends DefaultSubscriber<Object> {
    private final BaseAdapter adapter;

    public RefreshAdapterSubscriber(BaseAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public void onNext(Object ignored) {
        adapter.notifyDataSetChanged();
    }
}
