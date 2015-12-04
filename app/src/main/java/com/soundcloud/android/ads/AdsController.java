package com.soundcloud.android.ads;

import static com.soundcloud.android.utils.Log.ADS_TAG;

import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.AudioAdFailedToBufferEvent;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.Player;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;

import android.util.Log;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Singleton
public class AdsController {

    public static final int FAILED_AD_WAIT_SECS = 6;
    private static final int MAX_CONCURRENT_AD_FETCHES = 2;
    private static final long DEFAULT_OPERATION_STALE_TIME = TimeUnit.MINUTES.toMillis(10);

    private final EventBus eventBus;
    private final AdsOperations adsOperations;
    private final VisualAdImpressionOperations visualAdImpressionOperations;
    private final AdOverlayImpressionOperations adOverlayImpressionOperations;
    private final PlayQueueManager playQueueManager;
    private final TrackRepository trackRepository;
    private final Scheduler scheduler;
    private final long fetchOperationStaleTime;

    private Subscription skipFailedAdSubscription = RxUtils.invalidSubscription();
    private Map<Urn, AdsFetchOperation> currentAdsFetches = new HashMap<>(MAX_CONCURRENT_AD_FETCHES);
    private Optional<ApiAdsForTrack> adsForNextTrack = Optional.absent();
    private ActivityLifeCycleEvent currentLifeCycleEvent;

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

    private final Func1<Object, Boolean> shouldFetchAudioAdForNextTrack = new Func1<Object, Boolean>() {
        @Override
        public Boolean call(Object event) {
            return playQueueManager.hasNextItem()
                    && !adsOperations.isNextItemAd()
                    && !adsOperations.isCurrentItemAd()
                    && !alreadyFetchedAdForTrack(playQueueManager.getNextPlayQueueItem());
        }
    };

    private final Func1<Object, Boolean> shouldFetchInterstitialForCurrentTrack = new Func1<Object, Boolean>() {
        @Override
        public Boolean call(Object event) {
            return !adsOperations.isCurrentItemAd()
                    && !alreadyFetchedAdForTrack(playQueueManager.getCurrentPlayQueueItem());
        }
    };

    private final Func1<Player.StateTransition, Boolean> isBufferingAudioAd = new Func1<Player.StateTransition, Boolean>() {
        @Override
        public Boolean call(Player.StateTransition state) {
            return state.isBuffering() && adsOperations.isCurrentItemAudioAd();
        }
    };

    private final Func1<PropertySet, Observable<ApiAdsForTrack>> fetchAds = new Func1<PropertySet, Observable<ApiAdsForTrack>>() {
        @Override
        public Observable<ApiAdsForTrack> call(PropertySet propertySet) {
            return adsOperations.ads(propertySet.get(TrackProperty.URN));
        }
    };

    private final Action1<Player.StateTransition> unsubscribeFailedAdSkip = new Action1<Player.StateTransition>() {
        @Override
        public void call(Player.StateTransition stateTransition) {
            if (stateTransition.isPlayerPlaying() || stateTransition.isPaused()) {
                skipFailedAdSubscription.unsubscribe();
            } else if (stateTransition.wasError() && adsOperations.isCurrentItemAudioAd()) {
                skipFailedAdSubscription.unsubscribe();
                playQueueManager.moveToNextPlayableItem(false);
            }
        }
    };

    @Inject
    public AdsController(EventBus eventBus, AdsOperations adsOperations,
                         VisualAdImpressionOperations visualAdImpressionOperations,
                         AdOverlayImpressionOperations adOverlayImpressionOperations,
                         PlayQueueManager playQueueManager,
                         TrackRepository trackRepository) {
        this(eventBus, adsOperations, visualAdImpressionOperations, adOverlayImpressionOperations,
                playQueueManager, trackRepository, AndroidSchedulers.mainThread());
    }

    public AdsController(EventBus eventBus, AdsOperations adsOperations,
                         VisualAdImpressionOperations visualAdImpressionOperations,
                         AdOverlayImpressionOperations adOverlayImpressionOperations,
                         PlayQueueManager playQueueManager,
                         TrackRepository trackRepository,
                         Scheduler scheduler) {

        this(eventBus, adsOperations, visualAdImpressionOperations, adOverlayImpressionOperations,
                playQueueManager, trackRepository, scheduler, DEFAULT_OPERATION_STALE_TIME);
    }

    public AdsController(EventBus eventBus, AdsOperations adsOperations,
                         VisualAdImpressionOperations visualAdImpressionOperations,
                         AdOverlayImpressionOperations adOverlayImpressionOperations,
                         PlayQueueManager playQueueManager,
                         TrackRepository trackRepository,
                         Scheduler scheduler,
                         long fetchOperationStaleTime) {

        this.eventBus = eventBus;
        this.adsOperations = adsOperations;
        this.visualAdImpressionOperations = visualAdImpressionOperations;
        this.adOverlayImpressionOperations = adOverlayImpressionOperations;
        this.playQueueManager = playQueueManager;
        this.trackRepository = trackRepository;
        this.scheduler = scheduler;
        this.fetchOperationStaleTime = fetchOperationStaleTime;
    }

    public void subscribe() {

        eventBus.queue(EventQueue.CURRENT_PLAY_QUEUE_ITEM).subscribe(new ResetAdsOnTrackChange());

        final Observable<Object> queueChangeForAd = Observable.merge(
                eventBus.queue(EventQueue.CURRENT_PLAY_QUEUE_ITEM),
                eventBus.queue(EventQueue.PLAY_QUEUE).filter(IS_QUEUE_UPDATE)
        );

        queueChangeForAd
                .filter(shouldFetchInterstitialForCurrentTrack)
                .subscribe(new FetchAdForCurrentTrackSubscriber());

        queueChangeForAd
                .filter(shouldFetchAudioAdForNextTrack)
                .subscribe(new FetchAdForNextTrackSubscriber());

        eventBus.queue(EventQueue.PLAYBACK_STATE_CHANGED)
                .doOnNext(unsubscribeFailedAdSkip)
                .filter(isBufferingAudioAd)
                .subscribe(new SkipFailedAdSubscriber());

        eventBus.queue(EventQueue.PLAYBACK_STATE_CHANGED)
                .subscribe(new LeaveBehindSubscriber());

        visualAdImpressionOperations.trackImpression().subscribe(eventBus.queue(EventQueue.TRACKING));
        adOverlayImpressionOperations.trackImpression().subscribe(eventBus.queue(EventQueue.TRACKING));

        eventBus.queue(EventQueue.ACTIVITY_LIFE_CYCLE)
                .subscribe(new ActivityStateSubscriber());
    }

    public void reconfigureAdForNextTrack() {
        final Optional<ApiAudioAd> nextTrackAudioAd = getAudioAdForNextTrack();

        if (playQueueManager.hasNextItem() &&
                !adsOperations.isNextItemAd() &&
                nextTrackAudioAd.isPresent() &&
                currentLifeCycleEvent.isNotForeground()) {
            final int nextTrackPosition = playQueueManager.getCurrentPosition() + 1;
            final Urn nextTrackUrn = playQueueManager.getNextPlayQueueItem().getUrn();
            adsOperations.insertAudioAd(nextTrackUrn, nextTrackAudioAd.get(), nextTrackPosition);
        }
    }

    private Optional<ApiAudioAd> getAudioAdForNextTrack() {
        if (adsForNextTrack.isPresent()) {
            ApiAdsForTrack ads = adsForNextTrack.get();
            if (ads.audioAd().isPresent()) {
                return ads.audioAd();
            }
        }
        return Optional.absent();
    }

    private boolean alreadyFetchedAdForTrack(PlayQueueItem playQueueItem) {
        return currentAdsFetches.containsKey(playQueueItem.getUrn());
    }

    private final class FetchAdForNextTrackSubscriber extends DefaultSubscriber<Object> {
        @Override
        public void onNext(Object event) {
            final Urn nextTrackUrn = playQueueManager.getNextPlayQueueItem().getUrn();
            final NextTrackSubscriber nextTrackSubscriber = new NextTrackSubscriber(playQueueManager.getCurrentPosition(), nextTrackUrn);
            createAdsFetchObservable(nextTrackUrn, nextTrackSubscriber);
        }
    }

    private final class FetchAdForCurrentTrackSubscriber extends DefaultSubscriber<Object> {
        @Override
        public void onNext(Object event) {
            final Urn currentTrackUrn = playQueueManager.getCurrentPlayQueueItem().getUrn();
            final InterstitialSubscriber audioAdSubscriber = new InterstitialSubscriber(playQueueManager.getCurrentPosition(), currentTrackUrn);
            createAdsFetchObservable(currentTrackUrn, audioAdSubscriber);
        }
    }

    private void createAdsFetchObservable(Urn trackUrn, DefaultSubscriber<ApiAdsForTrack> adSubscriber) {
        final Observable<ApiAdsForTrack> apiAdsForTrack = trackRepository.track(trackUrn)
                .filter(IS_MONETIZABLE)
                .flatMap(fetchAds)
                .observeOn(AndroidSchedulers.mainThread());

        currentAdsFetches.put(trackUrn, new AdsFetchOperation(apiAdsForTrack.subscribe(adSubscriber)));
    }

    private class ResetAdsOnTrackChange extends DefaultSubscriber<CurrentPlayQueueItemEvent> {
        @Override
        public void onNext(CurrentPlayQueueItemEvent currentItemEvent) {
            adsForNextTrack = Optional.absent();
            Iterator<Map.Entry<Urn, AdsFetchOperation>> iter = currentAdsFetches.entrySet().iterator();
            while (iter.hasNext()) {

                final Map.Entry<Urn, AdsFetchOperation> operation = iter.next();
                final Urn monetizableUrn = operation.getKey();

                if ((!playQueueManager.isCurrentTrack(monetizableUrn) && isNotNextTrack(monetizableUrn)) || operation.getValue().hasExpired()) {
                    operation.getValue().subscription.unsubscribe();
                    iter.remove();
                }
            }

            skipFailedAdSubscription.unsubscribe();

            if (!adsOperations.isCurrentItemAd()) {
                adsOperations.clearAllAdsFromQueue();
            }
        }

        private boolean isNotNextTrack(Urn monetizableUrn) {
            return !playQueueManager.isTrackAt(monetizableUrn, playQueueManager.getCurrentPosition() + 1);
        }
    }

    private final class NextTrackSubscriber extends DefaultSubscriber<ApiAdsForTrack> {
        private final int intendedPosition;
        private final Urn monetizableTrack;

        NextTrackSubscriber(int intendedPosition, Urn monetizableTrack) {
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
                adsForNextTrack = Optional.of(apiAdsForTrack);
                adsOperations.applyAdToTrack(monetizableTrack, apiAdsForTrack);
            }
        }
    }

    private final class InterstitialSubscriber extends DefaultSubscriber<ApiAdsForTrack> {
        private final int intendedPosition;
        private final Urn monetizableTrack;

        InterstitialSubscriber(int intendedPosition, Urn monetizableTrack) {
            this.intendedPosition = intendedPosition;
            this.monetizableTrack = monetizableTrack;
        }

        @Override
        public void onNext(ApiAdsForTrack apiAdsForTrack) {
            if (playQueueManager.getCurrentPosition() == intendedPosition) {
                adsOperations.applyInterstitialToTrack(monetizableTrack, apiAdsForTrack);
            }
        }
    }

    private final class SkipFailedAdSubscriber extends DefaultSubscriber<Player.StateTransition> {

        @Override
        public void onNext(final Player.StateTransition state) {
            skipFailedAdSubscription.unsubscribe();
            skipFailedAdSubscription = Observable.timer(FAILED_AD_WAIT_SECS, TimeUnit.SECONDS, scheduler)
                    .subscribe(new DefaultSubscriber<Long>() {
                        @Override
                        public void onNext(Long args) {
                            Log.i(ADS_TAG, "Skipping ad after waiting " + FAILED_AD_WAIT_SECS + " seconds for it to load.");
                            final AudioAdFailedToBufferEvent event =
                                    new AudioAdFailedToBufferEvent(
                                        state.getTrackUrn(),
                                        state.getProgress(),
                                        FAILED_AD_WAIT_SECS);
                            eventBus.publish(EventQueue.TRACKING, event);
                            playQueueManager.moveToNextPlayableItem(false);
                        }
                    });
        }
    }

    private class LeaveBehindSubscriber extends DefaultSubscriber<Player.StateTransition> {
        @Override
        public void onNext(Player.StateTransition state) {
            if (adsOperations.isCurrentItemAudioAd() && state.trackEnded()) {
                final Optional<AdData> monetizableAdData = adsOperations.getMonetizableTrackAdData();
                if (monetizableAdData.isPresent() && monetizableAdData.get() instanceof OverlayAdData) {
                    ((OverlayAdData) monetizableAdData.get()).setMetaAdCompleted();
                }
            }
        }
    }

    private class AdsFetchOperation {

        private final Subscription subscription;
        private final long createdAtMillis;

        private AdsFetchOperation(Subscription subscription) {
            this.subscription = subscription;
            this.createdAtMillis = System.currentTimeMillis();
        }

        public boolean hasExpired() {
            return System.currentTimeMillis() - createdAtMillis > fetchOperationStaleTime;
        }
    }

    private class ActivityStateSubscriber extends DefaultSubscriber<ActivityLifeCycleEvent> {
        @Override
        public void onNext(ActivityLifeCycleEvent latestState) {
            currentLifeCycleEvent = latestState;
        }
    }
}
