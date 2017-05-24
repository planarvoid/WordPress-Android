package com.soundcloud.android.ads;

import static com.soundcloud.android.events.AdPlaybackEvent.InlayAdEvent;
import static com.soundcloud.android.events.AdPlaybackEvent.NoVideoOnScreen;

import com.soundcloud.android.events.AdPlaybackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.InlayAdImpressionEvent;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import dagger.Lazy;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;

import javax.inject.Inject;
import java.lang.ref.WeakReference;
import java.util.Date;

class InlayAdOperations {

    private final EventBus eventBus;
    private final Lazy<AdPlayer> inlayAdPlayer;

    @Inject
    InlayAdOperations(EventBus eventBus, Lazy<AdPlayer> inlayAdPlayer) {
        this.eventBus = eventBus;
        this.inlayAdPlayer = inlayAdPlayer;
    }

    Subscription subscribe(InlayAdHelper helper) {
        return new CompositeSubscription(trackAppInstallImpressions(helper),
                                         handleVideoAdPlayback());
    }

    private Subscription trackAppInstallImpressions(InlayAdHelper helper) {
        return eventBus.queue(EventQueue.INLAY_AD)
                       .filter(AdPlaybackEvent::forAppInstall)
                       .cast(InlayAdEvent.class)
                       .filter(new OnScreenAndImageLoaded(helper))
                       .cast(InlayAdEvent.class)
                       .map(event -> {
                                final long eventTime = event.eventTime().getTime();
                                final AppInstallAd ad = (AppInstallAd) event.getAd();
                                ad.setImpressionReported();
                                return InlayAdImpressionEvent.create(ad, event.getPosition(), eventTime);
                            })
                       .subscribe(eventBus.queue(EventQueue.TRACKING));
    }

    private Subscription handleVideoAdPlayback() {
        return eventBus.queue(EventQueue.INLAY_AD)
                       .filter(AdPlaybackEvent::forVideoAd)
                       .observeOn(AndroidSchedulers.mainThread())
                       .subscribe(new VideoPlaybackSubscriber(inlayAdPlayer.get()));
    }

    private static class VideoPlaybackSubscriber extends DefaultSubscriber<AdPlaybackEvent> {

        private final AdPlayer adPlayer;

        VideoPlaybackSubscriber(AdPlayer adPlayer) {
            this.adPlayer = adPlayer;
        }

        @Override
        public void onNext(AdPlaybackEvent event) {
            if (event.isOnScreen()) {
                final VideoAd videoAd = (VideoAd) ((InlayAdEvent) event).getAd();
                adPlayer.autoplay(videoAd);
            } else if (event instanceof NoVideoOnScreen && adPlayer.isPlaying()) {
                final boolean shouldMute = ((NoVideoOnScreen) event).shouldMute();
                adPlayer.autopause(shouldMute);
            } else if (event.isToggleVolume()) {
                adPlayer.toggleVolume();
            } else if (event.isTogglePlayback()) {
                final VideoAd videoAd = (VideoAd) ((InlayAdEvent) event).getAd();
                adPlayer.togglePlayback(videoAd);
            }
        }
    }

    static class OnScreenAndImageLoaded extends Predicate {
        final WeakReference<InlayAdHelper> helperRef;

        OnScreenAndImageLoaded(InlayAdHelper helper) {
            this.helperRef = new WeakReference<>(helper);
        }

        private AppInstallAd getAppInstall(InlayAdEvent event) {
            return (AppInstallAd) event.getAd();
        }

        @Override
        public Boolean call(InlayAdEvent event) {
            return !getAppInstall(event).hasReportedImpression() && super.call(event);
        }

        @Override
        public Boolean whenOnScreen(InlayAdEvent event) {
            final Optional<Date> imageLoaded = getAppInstall(event).imageLoadTime();
            return imageLoaded.isPresent() && imageLoaded.get().before(event.eventTime());
        }

        @Override
        public Boolean whenImageLoaded(InlayAdEvent event) {
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
            if (event.isOnScreen()) {
                return whenOnScreen(event);
            } else if (event.isImageLoaded()) {
                return whenImageLoaded(event);
            } else {
                return otherwise(event);
            }
        }

        abstract Out whenOnScreen(InlayAdEvent event);
        abstract Out whenImageLoaded(InlayAdEvent event);

        Out otherwise(InlayAdEvent event) {
            throw new IllegalArgumentException("Unhandled type for event: " + event);
        }
    }
}
