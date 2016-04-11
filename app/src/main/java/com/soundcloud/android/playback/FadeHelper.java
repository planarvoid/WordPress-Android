package com.soundcloud.android.playback;

import static com.soundcloud.android.playback.VolumeInterpolator.ACCELERATE;
import static com.soundcloud.android.playback.VolumeInterpolator.DECELERATE;

import com.google.auto.factory.AutoFactory;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.annotations.VisibleForTesting;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import java.util.concurrent.TimeUnit;

@AutoFactory(allowSubclasses = true)
class FadeHelper {
    private static final long STEP_MS = 10;

    interface Listener {
        void onFadeFinished();

        void onFade(float volume);
    }

    private final Listener listener;
    private final Scheduler scheduler;
    private Subscription subscription = RxUtils.invalidSubscription();

    FadeHelper(Listener listener) {
        this.listener = listener;
        this.scheduler = Schedulers.newThread();
    }

    @VisibleForTesting
    FadeHelper(Listener listener, Scheduler scheduler) {
        this.listener = listener;
        this.scheduler = scheduler;
    }

    void stop() {
        if (!subscription.isUnsubscribed()) {
            listener.onFadeFinished();
            subscription.unsubscribe();
        }
    }

    void fade(FadeRequest request) {
        if (request.duration() > 0) {
            stop();
            start(request);
        } else {
            finishWithVolume(request.endValue());
        }
    }

    private void start(final FadeRequest request) {
        long delay = Math.max(0, -request.offset());
        long ahead = Math.max(0, request.offset());
        int samples = (int) (Math.ceil((request.duration() - ahead) / STEP_MS) + 1);

        subscription = Observable
                .interval(delay, STEP_MS, TimeUnit.MILLISECONDS, scheduler)
                .take(samples)
                .map(toFadeVolume(request))
                .subscribe(new FadeSubscriber());
    }

    private void finishWithVolume(float volume) {
        listener.onFade(volume);
        listener.onFadeFinished();
    }

    private class FadeSubscriber extends DefaultSubscriber<Float> {
        @Override
        public void onNext(Float volume) {
            listener.onFade(volume);
        }

        @Override
        public void onCompleted() {
            listener.onFadeFinished();
        }
    }

    private static Func1<Long, Float> toFadeVolume(final FadeRequest request) {
        return new Func1<Long, Float>() {
            @Override
            public Float call(Long step) {
                float elapsed = step * STEP_MS + (Math.max(0, request.offset()));
                float fraction = Math.max(0.0f, Math.min(1.0f, elapsed / (float) request.duration()));
                return interpolate(fraction, request.startValue(), request.endValue());
            }

            private float interpolate(float position, float start, float end) {
                return (start >= end ? ACCELERATE : DECELERATE).range(position, start, end);
            }
        };
    }
}
