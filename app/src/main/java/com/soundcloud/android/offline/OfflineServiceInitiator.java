package com.soundcloud.android.offline;

import com.soundcloud.android.rx.observers.DefaultObserver;
import io.reactivex.functions.Consumer;

import android.content.Context;

import javax.inject.Inject;

class OfflineServiceInitiator {
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

    Consumer<Object> startFromUserConsumer() {
        return ignored -> OfflineContentService.startFromUserAction(context);
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
