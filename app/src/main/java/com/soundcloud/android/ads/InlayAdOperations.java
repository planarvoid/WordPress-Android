package com.soundcloud.android.ads;

import static com.soundcloud.android.events.AdPlaybackEvent.ImageLoaded;
import static com.soundcloud.android.events.AdPlaybackEvent.NoVideoOnScreen;
import static com.soundcloud.android.events.AdPlaybackEvent.OnScreen;
import static com.soundcloud.android.events.AdPlaybackEvent.TogglePlayback;
import static com.soundcloud.android.events.AdPlaybackEvent.ToggleVolume;
import static com.soundcloud.android.events.AdPlaybackEvent.WithAdData;

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
                       .filter(new OnScreenAndImageLoaded(helper))
                       .cast(WithAdData.class)
                       .map(event -> {
                                final long eventTime = ((AdPlaybackEvent) event).getEventTime().getTime();
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
            if (event instanceof OnScreen) {
                final VideoAd videoAd = (VideoAd) ((WithAdData) event).getAd();
                adPlayer.autoplay(videoAd);
            } else if (event instanceof NoVideoOnScreen && adPlayer.isPlaying()) {
                final boolean shouldMute = ((NoVideoOnScreen) event).shouldMute();
                adPlayer.autopause(shouldMute);
            } else if (event instanceof ToggleVolume) {
                adPlayer.toggleVolume();
            } else if (event instanceof TogglePlayback) {
                final VideoAd videoAd = (VideoAd) ((WithAdData) event).getAd();
                adPlayer.togglePlayback(videoAd);
            }
        }
    }

    static class OnScreenAndImageLoaded extends Predicate {
        final WeakReference<InlayAdHelper> helperRef;

        OnScreenAndImageLoaded(InlayAdHelper helper) {
            this.helperRef = new WeakReference<>(helper);
        }

        private AppInstallAd getAppInstall(AdPlaybackEvent event) {
            return (AppInstallAd) ((WithAdData) event).getAd();
        }

        @Override
        public Boolean call(AdPlaybackEvent event) {
            return !getAppInstall(event).hasReportedImpression() && super.call(event);
        }

        @Override
        public Boolean whenOnScreen(OnScreen event) {
            final Optional<Date> imageLoaded = getAppInstall(event).imageLoadTime();
            return imageLoaded.isPresent() && imageLoaded.get().before(event.getEventTime());
        }

        @Override
        public Boolean whenImageLoaded(ImageLoaded event) {
            final InlayAdHelper helper = helperRef.get();
            return helper != null && helper.isOnScreen(event.getPosition());
        }
    }

    private static abstract class Predicate extends Func<Boolean> {
        Boolean otherwise(AdPlaybackEvent event) {
            return false;
        }
    }

    private static abstract class Func<Out> implements Func1<AdPlaybackEvent, Out> {
        @Override
        public Out call(AdPlaybackEvent event) {
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

        Out otherwise(AdPlaybackEvent event) {
            throw new IllegalArgumentException("Unhandled type for event: " + event);
        }
    }
}
