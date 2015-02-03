package com.soundcloud.android.offline;

import com.soundcloud.android.rx.observers.DefaultSubscriber;

import android.content.Context;

final class StartOfflineContentServiceSubscriber extends DefaultSubscriber<Object> {

    private final Context context;

    public StartOfflineContentServiceSubscriber(Context context) {
        this.context = context;
    }

    @Override
    public void onNext(Object ignored) {
        OfflineContentService.startSyncing(context);
    }
}
