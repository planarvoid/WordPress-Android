package com.soundcloud.android.offline;

import com.soundcloud.android.rx.observers.DefaultSubscriber;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;

import android.content.Context;

import javax.inject.Inject;

class OfflineServiceInitiator {
    private final Context context;

    @Inject
    OfflineServiceInitiator(Context context) {
        this.context = context;
    }

    Action0 start() {
        return new Action0() {
            @Override
            public void call() {
                OfflineContentService.start(context);
            }
        };
    }

    Action0 stop() {
        return new Action0() {
            @Override
            public void call() {
                OfflineContentService.stop(context);
            }
        };
    }

    Action1<Object> startFromUserAction() {
        return new Action1<Object>() {
            @Override
            public void call(Object ignored) {
                OfflineContentService.startFromUserAction(context);
            }
        };
    }

    Subscriber<Void> startSubscriber() {
        return new DefaultSubscriber<Void>() {
            @Override
            public void onNext(Void ignored) {
                OfflineContentService.start(context);
            }
        };
    }
}
