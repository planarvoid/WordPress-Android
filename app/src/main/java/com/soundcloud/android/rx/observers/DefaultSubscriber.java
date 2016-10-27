package com.soundcloud.android.rx.observers;

import static com.soundcloud.android.rx.OperationDurationLogger.report;

import com.soundcloud.android.BuildConfig;
import com.soundcloud.android.rx.OperationDurationLogger;
import com.soundcloud.android.rx.OperationDurationLogger.TimeMeasure;
import com.soundcloud.android.utils.CallsiteToken;
import com.soundcloud.android.utils.ErrorUtils;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;

import java.util.concurrent.TimeUnit;

/**
 * Default subscriber base class to be used whenever you want default error handling
 * (cf. {@link com.soundcloud.android.utils.ErrorUtils#handleThrowable(Throwable, com.soundcloud.android.utils.CallsiteToken)}
 */
public class DefaultSubscriber<T> extends Subscriber<T> {

    private final CallsiteToken callsiteToken = CallsiteToken.build();
    private final TimeMeasure measure = OperationDurationLogger.create(callsiteToken.getStackTrace(), BuildConfig.DEBUG);

    public static <T> Subscription fireAndForget(Observable<T> observable) {
        return observable.subscribe(new DefaultSubscriber<T>());
    }

    public DefaultSubscriber(){
    }

    @Override
    public void onStart() {
        measure.start();
    }

    @Override
    public void onCompleted() {
        reportDuration();
    }

    @Override
    public void onError(Throwable e) {
        reportDuration();
        ErrorUtils.handleThrowable(e, callsiteToken);
    }

    private void reportDuration() {
        measure.stop();
        report(measure, 2, TimeUnit.SECONDS);
    }

    @Override
    public void onNext(T args) {
        // no-op by default.
    }
}
