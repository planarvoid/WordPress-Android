package com.soundcloud.android.rx;

import com.soundcloud.android.utils.Log;
import rx.plugins.DebugHook;
import rx.plugins.DebugNotification;
import rx.plugins.DebugNotificationListener;

public class LoggingDebugHook extends DebugHook<Void> {

    private static final String TAG = "RxJavaDebug";

    public LoggingDebugHook() {
        super(new DebugNotificationListener<Void>() {

            @Override
            public <T> Void start(DebugNotification<T> n) {
                Log.d(TAG, n.toString());
                return super.start(n);
            }

            @Override
            public void error(Void context, Throwable e) {
                Log.d(TAG, "caught error: " + e);
                super.error(context, e);
            }
        });
    }
}
