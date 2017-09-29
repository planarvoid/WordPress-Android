package com.soundcloud.android.offline;

import com.soundcloud.android.rx.observers.DefaultObserver;

import android.content.Context;

import javax.inject.Inject;

public class OfflineServiceInitiator {
    private final Context context;

    @Inject
    OfflineServiceInitiator(Context context) {
        this.context = context;
    }

    void start() {
        OfflineContentService.start(context);
    }

    void stop() {
        OfflineContentService.stop(context);
    }

    void startFromUserConsumer() {
        OfflineContentService.startFromUserAction(context);
    }

    DefaultObserver<Object> startObserver() {
        return new DefaultObserver<Object>() {
            @Override
            public void onNext(Object ignored) {
                OfflineContentService.start(context);
            }
        };
    }
}
