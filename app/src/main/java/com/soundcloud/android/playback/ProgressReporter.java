package com.soundcloud.android.playback;

import com.soundcloud.android.cast.CastOperations;
import com.soundcloud.android.cast.DefaultCastOperations;
import com.soundcloud.android.cast.LegacyCastOperations;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import rx.Subscription;
import rx.schedulers.TimeInterval;

import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Provider;
import java.lang.ref.WeakReference;

/**
* There is a progress puller already implemented as part of v3 (this should be used only in v2)
 * */
@Deprecated
public class ProgressReporter {

    private final CastOperations castOperations;
    private WeakReference<ProgressPuller> progressPullerReference;
    private Subscription subscription = RxUtils.invalidSubscription();

    @Inject
    ProgressReporter(Provider<DefaultCastOperations> castOperations,
                     Provider<LegacyCastOperations> legacyCastOperations,
                     FeatureFlags featureFlags) {
        this.castOperations = featureFlags.isEnabled(Flag.CAST_V3) ? castOperations.get() : legacyCastOperations.get();
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
        subscription = castOperations.intervalForProgressPull().subscribe(new ProgressTickSubscriber());
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
