package com.soundcloud.android.playback;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.cast.CastOperations;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.schedulers.Schedulers;
import rx.schedulers.TimeInterval;
import rx.subscriptions.Subscriptions;

import javax.inject.Inject;
import javax.inject.Named;
import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

public class ProgressReporter {

    private final CastOperations castOperations;
    private WeakReference<ProgressPuller> progressPullerReference;
    private Subscription subscription = Subscriptions.empty();

    @Inject
    ProgressReporter(CastOperations castOperations) {
        this.castOperations = castOperations;
    }

    public interface ProgressPuller {
        void pullProgress();
    }

    @VisibleForTesting
    public void setProgressPuller(ProgressPuller progressPuller) {
        progressPullerReference = new WeakReference<>(progressPuller);
    }

    public void start(){
        subscription.unsubscribe();
        subscription = castOperations.intervalForProgressPull().subscribe(new ProgressTickSubscriber());
    }

    public void stop(){
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
