package com.soundcloud.android.offline;

import com.soundcloud.android.rx.observers.DefaultSubscriber;

import android.content.Context;

final class OfflineContentServiceSubscriber extends DefaultSubscriber<Object> {

    private final Context context;
    private final boolean shouldStart;

    public OfflineContentServiceSubscriber(Context context, boolean shouldStart) {
        this.context = context;
        this.shouldStart = shouldStart;
    }

    @Override
    public void onNext(Object ignored) {
        if (shouldStart) {
            OfflineContentService.start(context);
        } else {
            OfflineContentService.stop(context);
        }
    }
}
