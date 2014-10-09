package com.soundcloud.android.ads;

import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.properties.Feature;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackOperations;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subscriptions.Subscriptions;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.TimeUnit;

@Singleton
public class AdsController {

    public static final int SKIP_DELAY_SECS = 3;
    private final EventBus eventBus;
    private final AdsOperations adsOperations;
    private final VisualAdImpressionOperations visualAdImpressionOperations;
    private final LeaveBehindImpressionOperations leaveBehindImpressionOperations;
    private final PlayQueueManager playQueueManager;
    private final TrackOperations trackOperations;
    private final FeatureFlags featureFlags;
    private final Scheduler scheduler;

    private Observable<ApiAdsForTrack> currentObservable;
    private Subscription audioAdSubscription = Subscriptions.empty();
    private Subscription skipAdSubscription = Subscriptions.empty();

    private static final Func1<PlayQueueEvent, Boolean> IS_QUEUE_UPDATE = new Func1<PlayQueueEvent, Boolean>() {
        @Override
        public Boolean call(PlayQueueEvent playQueueEvent) {
            return playQueueEvent.isQueueUpdate();
        }
    };

    private static final Func1<PropertySet, Boolean> IS_MONETIZABLE = new Func1<PropertySet, Boolean>() {
        @Override
        public Boolean call(PropertySet propertySet) {
            return propertySet.get(TrackProperty.MONETIZABLE);
        }
    };

    private final Func1<Object, Boolean> hasNextNonAudioAdTrack = new Func1<Object, Boolean>() {
        @Override
        public Boolean call(Object event) {
            return currentObservable == null
                    && playQueueManager.hasNextTrack()
                    && !adsOperations.isNextTrackAudioAd()
                    && !adsOperations.isCurrentTrackAudioAd();
        }
    };

    private final Func1<Playa.StateTransition, Boolean> isBufferingAudioAd = new Func1<Playa.StateTransition, Boolean>() {
        @Override
        public Boolean call(Playa.StateTransition state) {
            return state.isBuffering() && adsOperations.isCurrentTrackAudioAd();
        }
    };

    private final Func1<PropertySet, Observable<ApiAdsForTrack>> fetchAudioAd = new Func1<PropertySet, Observable<ApiAdsForTrack>>() {
        @Override
        public Observable<ApiAdsForTrack> call(PropertySet propertySet) {
            return adsOperations.audioAd(propertySet.get(TrackProperty.URN));
        }
    };

    private final Action1<CurrentPlayQueueTrackEvent> resetAudioAd = new Action1<CurrentPlayQueueTrackEvent>() {
        @Override
        public void call(CurrentPlayQueueTrackEvent event) {
            currentObservable = null;
            audioAdSubscription.unsubscribe();
            skipAdSubscription.unsubscribe();
            if (!adsOperations.isCurrentTrackAudioAd()) {
                adsOperations.clearAllAds();
            }
        }
    };

    private final Action1<Playa.StateTransition> unsubscribeSkipAd = new Action1<Playa.StateTransition>() {
        @Override
        public void call(Playa.StateTransition stateTransition) {
            if (stateTransition.isPlayerPlaying() || stateTransition.isPaused()) {
                skipAdSubscription.unsubscribe();
            } else if (stateTransition.wasError() && adsOperations.isCurrentTrackAudioAd()) {
                skipAdSubscription.unsubscribe();
                playQueueManager.autoNextTrack();
            }
        }
    };

    @Inject
    public AdsController(EventBus eventBus, AdsOperations adsOperations,
                         VisualAdImpressionOperations visualAdImpressionOperations,
                         LeaveBehindImpressionOperations leaveBehindImpressionOperations,
                         PlayQueueManager playQueueManager,
                         TrackOperations trackOperations,
                         FeatureFlags featureFlags) {
        this(eventBus, adsOperations, visualAdImpressionOperations, leaveBehindImpressionOperations,
                playQueueManager, trackOperations, featureFlags, AndroidSchedulers.mainThread());
    }

    public AdsController(EventBus eventBus, AdsOperations adsOperations,
                         VisualAdImpressionOperations visualAdImpressionOperations,
                         LeaveBehindImpressionOperations leaveBehindImpressionOperations,
                         PlayQueueManager playQueueManager,
                         TrackOperations trackOperations,
                         FeatureFlags featureFlags, Scheduler scheduler) {
        this.eventBus = eventBus;
        this.adsOperations = adsOperations;
        this.visualAdImpressionOperations = visualAdImpressionOperations;
        this.leaveBehindImpressionOperations = leaveBehindImpressionOperations;
        this.playQueueManager = playQueueManager;
        this.trackOperations = trackOperations;
        this.featureFlags = featureFlags;
        this.scheduler = scheduler;
    }

    public void subscribe() {
        eventBus.queue(EventQueue.PLAY_QUEUE_TRACK)
                .doOnNext(resetAudioAd)
                .filter(hasNextNonAudioAdTrack)
                .subscribe(new PlayQueueSubscriber());

        eventBus.queue(EventQueue.PLAY_QUEUE)
                .filter(IS_QUEUE_UPDATE)
                .filter(hasNextNonAudioAdTrack)
                .subscribe(new PlayQueueSubscriber());

        eventBus.queue(EventQueue.PLAYBACK_STATE_CHANGED)
                .doOnNext(unsubscribeSkipAd)
                .filter(isBufferingAudioAd)
                .subscribe(new PlaybackStateSubscriber());

        eventBus.queue(EventQueue.PLAYBACK_STATE_CHANGED)
                .subscribe(new LeaveBehindSubscriber());

        visualAdImpressionOperations.trackImpression().subscribe(eventBus.queue(EventQueue.TRACKING));

        if (featureFlags.isEnabled(Feature.LEAVE_BEHIND)) {
            leaveBehindImpressionOperations.trackImpression().subscribe(eventBus.queue(EventQueue.TRACKING));
        }
    }

    private final class PlayQueueSubscriber extends DefaultSubscriber<Object> {
        @Override
        public void onNext(Object event) {
            final Urn nextTrackUrn = playQueueManager.getNextTrackUrn();
            currentObservable = trackOperations.track(nextTrackUrn)
                    .filter(IS_MONETIZABLE)
                    .mergeMap(fetchAudioAd)
                    .observeOn(AndroidSchedulers.mainThread());
            audioAdSubscription = currentObservable.subscribe(new AudioAdSubscriber(playQueueManager.getCurrentPosition(), nextTrackUrn));
        }
    }

    private final class AudioAdSubscriber extends DefaultSubscriber<ApiAdsForTrack> {
        private final int intendedPosition;
        private final Urn monetizableTrack;

        AudioAdSubscriber(int intendedPosition, Urn monetizableTrack) {
            this.intendedPosition = intendedPosition;
            this.monetizableTrack = monetizableTrack;
        }

        @Override
        public void onNext(ApiAdsForTrack apiAdsForTrack) {
            /*
             * We're checking if we're still at the intended position before we try to insert the ad in the play queue.
             * This is a temporary work-around for a race condition where unsubscribe doesn't happen immediately and
             * we attempt to put an ad in the queue twice. Matthias, please help!
             */
            if (playQueueManager.getCurrentPosition() == intendedPosition) {
                adsOperations.applyAdToTrack(monetizableTrack, apiAdsForTrack);
            }
        }
    }

    private final class PlaybackStateSubscriber extends DefaultSubscriber<Playa.StateTransition> {

        @Override
        public void onNext(Playa.StateTransition state) {
            skipAdSubscription.unsubscribe();
            skipAdSubscription = Observable.timer(SKIP_DELAY_SECS, TimeUnit.SECONDS, scheduler)
                    .subscribe(new DefaultSubscriber<Long>() {
                        @Override
                        public void onNext(Long args) {
                            playQueueManager.autoNextTrack();
                        }
                    });
        }
    }

    private class LeaveBehindSubscriber extends DefaultSubscriber<Playa.StateTransition> {
        @Override
        public void onNext(Playa.StateTransition state) {
            if (adsOperations.isCurrentTrackAudioAd()) {
                if (state.trackEnded()) {
                    adsOperations.getMonetizableTrackMetaData().put(LeaveBehindProperty.META_AD_COMPLETED, true);
                }
            }
        }
    }
}
