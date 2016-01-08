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

    void start() {
        OfflineContentService.start(context);
    }

    void stop() {
        OfflineContentService.stop(context);
    }

    Action0 action0Start() {
        return new Action0() {
            @Override
            public void call() {
                start();
            }
        };
    }

    Action0 action0Stop() {
        return new Action0() {
            @Override
            public void call() {
                stop();
            }
        };
    }

    Action1<Object> action1Start() {
        return new Action1<Object>() {
            @Override
            public void call(Object ignored) {
                start();
            }
        };
    }

    Subscriber<Void> startSubscriber() {
        return new DefaultSubscriber<Void>() {
            @Override
            public void onNext(Void ignored) {
                start();
            }
        };
    }
}
