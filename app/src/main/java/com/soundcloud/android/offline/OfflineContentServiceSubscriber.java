package com.soundcloud.android.offline;

import com.soundcloud.android.rx.observers.DefaultSubscriber;

import android.content.Context;

final class OfflineContentServiceSubscriber extends DefaultSubscriber<Object> {

    private final Context context;

    public OfflineContentServiceSubscriber(Context context) {
        this.context = context;
    }

    @Override
    public void onNext(Object ignored) {
        OfflineContentService.start(context);
    }
}
