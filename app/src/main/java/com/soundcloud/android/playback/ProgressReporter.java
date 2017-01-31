package com.soundcloud.android.playback;

import com.soundcloud.android.cast.LegacyCastOperations;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import rx.Scheduler;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.TimeInterval;

import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import java.lang.ref.WeakReference;

/**
 * There is a progress puller already implemented as part of v3 (this should be used only in v2)
 */
@Deprecated
public class ProgressReporter {

    private final LegacyCastOperations legacyCastOperations;
    private final Scheduler scheduler;
    private WeakReference<ProgressPuller> progressPullerReference;
    private Subscription subscription = RxUtils.invalidSubscription();

    @Inject
    ProgressReporter(LegacyCastOperations legacyCastOperations) {
        this(legacyCastOperations, AndroidSchedulers.mainThread());
    }

    ProgressReporter(LegacyCastOperations legacyCastOperations, Scheduler scheduler) {
        this.legacyCastOperations = legacyCastOperations;
        this.scheduler = scheduler;
    }

    public interface ProgressPuller {
        void pullProgress();
    }

    @VisibleForTesting
    public void setProgressPuller(ProgressPuller progressPuller) {
        progressPullerReference = new WeakReference<>(progressPuller);
    }

    public void start() {
        subscription.unsubscribe();
        subscription = legacyCastOperations
                .intervalForProgressPull()
                .observeOn(scheduler)
                .subscribe(new ProgressTickSubscriber());
    }

    public void stop() {
        subscription.unsubscribe();
    }

    private class ProgressTickSubscriber extends DefaultSubscriber<TimeInterval<Long>> {
        @Override
        public void onNext(TimeInterval<Long> timeInterval) {
            if (progressPullerReference != null) {
                final ProgressPuller progressPuller = progressPullerReference.get();
                if (progressPuller != null) {
                    progressPuller.pullProgress();
                }
            }
        }
    }
}
