package com.soundcloud.android.offline;

import com.soundcloud.android.rx.observers.DefaultSubscriber;
import rx.functions.Action0;

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

    public static Action0 startServiceAction(final Context context) {
        return new Action0() {
            @Override
            public void call() {
                OfflineContentService.start(context);
            }
        };
    }
}
