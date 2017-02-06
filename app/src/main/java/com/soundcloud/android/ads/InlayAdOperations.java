package com.soundcloud.android.ads;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.InlayAdEvent;
import com.soundcloud.android.events.InlayAdImpressionEvent;
import com.soundcloud.android.playback.VideoAdPlaybackItem;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import rx.functions.Func1;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

import javax.inject.Inject;
import java.lang.ref.WeakReference;
import java.util.Date;

import static com.soundcloud.android.events.InlayAdEvent.ImageLoaded;
import static com.soundcloud.android.events.InlayAdEvent.OnScreen;
import static com.soundcloud.android.events.InlayAdEvent.NoVideoOnScreen;
import static com.soundcloud.android.events.InlayAdEvent.WithAdData;

class InlayAdOperations {

    private final EventBus eventBus;
    private final InlayAdPlayer inlayAdPlayer;

    @Inject
    InlayAdOperations(EventBus eventBus, InlayAdPlayer inlayAdPlayer) {
        this.eventBus = eventBus;
        this.inlayAdPlayer = inlayAdPlayer;
    }

    Subscription subscribe(InlayAdHelper helper) {
        return new CompositeSubscription(trackAppInstallImpressions(helper),
                                         handleVideoAdPlayback());
    }

    private Subscription trackAppInstallImpressions(InlayAdHelper helper) {
        return eventBus.queue(EventQueue.INLAY_AD)
                       .filter(InlayAdEvent::forAppInstall)
                       .filter(new OnScreenAndImageLoaded(helper))
                       .map(event -> {
                                final InlayAdEvent.WithAdData eventWithAdData = (WithAdData) event;
                                final AppInstallAd ad = (AppInstallAd) eventWithAdData.getAd();
                                ad.setImpressionReported();
                                return InlayAdImpressionEvent.create(ad, eventWithAdData.getPosition(), event.getEventTime().getTime());
                            })
                       .subscribe(eventBus.queue(EventQueue.TRACKING));
    }

    private Subscription handleVideoAdPlayback() {
        return eventBus.queue(EventQueue.INLAY_AD)
                       .filter(InlayAdEvent::forVideoAd)
                       .subscribe(new VideoPlaybackSubscriber());
    }

    private class VideoPlaybackSubscriber extends DefaultSubscriber<InlayAdEvent> {

        @Override
        public void onNext(InlayAdEvent event) {
            if (event instanceof OnScreen) {
                final VideoAd videoAd = (VideoAd) ((WithAdData) event).getAd();
                inlayAdPlayer.play(VideoAdPlaybackItem.create(videoAd, 0L, 0.0f));
            } else if (event instanceof NoVideoOnScreen && inlayAdPlayer.isPlaying()) {
                inlayAdPlayer.pause();
            }
        }
    }

    static class OnScreenAndImageLoaded extends Predicate {
        final WeakReference<InlayAdHelper> helperRef;

        OnScreenAndImageLoaded(InlayAdHelper helper) {
            this.helperRef = new WeakReference<>(helper);
        }

        private AppInstallAd getAppInstall(InlayAdEvent event) {
            return (AppInstallAd) ((WithAdData) event).getAd();
        }

        @Override
        public Boolean call(InlayAdEvent event) {
            return !getAppInstall(event).hasReportedImpression() && super.call(event);
        }

        @Override
        public Boolean whenOnScreen(OnScreen event) {
            final Optional<Date> imageLoaded = getAppInstall(event).getImageLoadTime();
            return imageLoaded.isPresent() && imageLoaded.get().before(event.getEventTime());
        }

        @Override
        public Boolean whenImageLoaded(ImageLoaded event) {
            final InlayAdHelper helper = helperRef.get();
            return helper != null && helper.isOnScreen(event.getPosition());
        }
    }

    private static abstract class Predicate extends Func<Boolean> {
        Boolean otherwise(InlayAdEvent event) {
            return false;
        }
    }

    private static abstract class Func<Out> implements Func1<InlayAdEvent, Out> {
        @Override
        public Out call(InlayAdEvent event) {
            if (event instanceof OnScreen) {
                return whenOnScreen((OnScreen) event);
            } else if (event instanceof ImageLoaded) {
                return whenImageLoaded((ImageLoaded) event);
            } else {
                return otherwise(event);
            }
        }

        abstract Out whenOnScreen(OnScreen event);
        abstract Out whenImageLoaded(ImageLoaded event);

        Out otherwise(InlayAdEvent event) {
            throw new IllegalArgumentException("Unhandled type for event: " + event);
        }
    }
}
