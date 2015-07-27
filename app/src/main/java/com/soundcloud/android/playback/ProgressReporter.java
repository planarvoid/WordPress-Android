package com.soundcloud.android.playback;

import com.soundcloud.android.cast.CastOperations;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import rx.Subscription;
import rx.schedulers.TimeInterval;

import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import java.lang.ref.WeakReference;

public class ProgressReporter {

    private final CastOperations castOperations;
    private WeakReference<ProgressPuller> progressPullerReference;
    private Subscription subscription = RxUtils.invalidSubscription();

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
