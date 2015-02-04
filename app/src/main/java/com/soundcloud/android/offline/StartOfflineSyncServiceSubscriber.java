package com.soundcloud.android.offline;

import com.soundcloud.android.rx.observers.DefaultSubscriber;

import android.content.Context;

final class StartOfflineSyncServiceSubscriber extends DefaultSubscriber<Object> {

    private final Context context;
    private final boolean shouldStart;

    public StartOfflineSyncServiceSubscriber(Context context, boolean shouldStart) {
        this.context = context;
        this.shouldStart = shouldStart;
    }

    @Override
    public void onNext(Object ignored) {
        if (shouldStart) {
            OfflineSyncService.startSyncing(context);
        } else {
            OfflineSyncService.stopSyncing(context);
        }
    }
}
