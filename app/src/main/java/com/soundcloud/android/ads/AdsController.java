package com.soundcloud.android.ads;

import static com.soundcloud.android.utils.Log.ADS_TAG;

import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.AdDeliveryEvent;
import com.soundcloud.android.events.AdFailedToBufferEvent;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaybackStateTransition;
import com.soundcloud.android.playback.TrackQueueItem;
import com.soundcloud.android.playback.VideoQueueItem;
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
import java.util.List;
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
    private boolean didReplaceNextAd;
    private boolean isForeground;
    private boolean isPlayerVisible;

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

    private final Func1<Object, Boolean> shouldFetchAudioAdForNextItem = new Func1<Object, Boolean>() {
        @Override
        public Boolean call(Object event) {
            return playQueueManager.hasTrackAsNextItem()
                    && !adsOperations.isNextItemAd()
                    && !adsOperations.isCurrentItemAd()
                    && !alreadyFetchedAdForTrack(playQueueManager.getNextPlayQueueItem());
        }
    };

    private final Func1<Object, Boolean> shouldFetchInterstitialForCurrentTrack = new Func1<Object, Boolean>() {
        @Override
        public Boolean call(Object event) {
            return playQueueManager.getCurrentPlayQueueItem().isTrack()
                    && !adsOperations.isCurrentItemAd()
                    && !alreadyFetchedAdForTrack(playQueueManager.getCurrentPlayQueueItem());
        }
    };

    private final Func1<PlaybackStateTransition, Boolean> isBufferingAd = new Func1<PlaybackStateTransition, Boolean>() {
        @Override
        public Boolean call(PlaybackStateTransition state) {
            return state.isBuffering() && adsOperations.isCurrentItemAd();
        }
    };

    private final Func1<PropertySet, Observable<ApiAdsForTrack>> fetchAds = new Func1<PropertySet, Observable<ApiAdsForTrack>>() {
        @Override
        public Observable<ApiAdsForTrack> call(PropertySet propertySet) {
            return adsOperations.ads(propertySet.get(TrackProperty.URN), isPlayerVisible, isForeground);
        }
    };

    private final Action1<PlaybackStateTransition> unsubscribeFailedAdSkip = new Action1<PlaybackStateTransition>() {
        @Override
        public void call(PlaybackStateTransition stateTransition) {
            if (stateTransition.isPlayerPlaying() || stateTransition.isPaused()) {
                skipFailedAdSubscription.unsubscribe();
            } else if (stateTransition.wasError() && adsOperations.isCurrentItemAd()) {
                skipFailedAdSubscription.unsubscribe();
                playQueueManager.autoMoveToNextPlayableItem();
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
                .filter(shouldFetchAudioAdForNextItem)
                .subscribe(new FetchAdForNextTrackSubscriber());

        eventBus.queue(EventQueue.PLAYBACK_STATE_CHANGED)
                .doOnNext(unsubscribeFailedAdSkip)
                .filter(isBufferingAd)
                .subscribe(new SkipFailedAdSubscriber());

        visualAdImpressionOperations.trackImpression().subscribe(eventBus.queue(EventQueue.TRACKING));
        adOverlayImpressionOperations.trackImpression().subscribe(eventBus.queue(EventQueue.TRACKING));

        eventBus.queue(EventQueue.ACTIVITY_LIFE_CYCLE)
                .subscribe(new ActivityStateSubscriber());

        eventBus.queue(EventQueue.PLAYER_UI)
                .subscribe(new PlayerStateSubscriber());
    }

    public void reconfigureAdForNextTrack() {
        if (!isForeground && adsForNextTrack.isPresent() && playQueueManager.hasNextItem()) {
            final ApiAdsForTrack ads = adsForNextTrack.get();
            final PlayQueueItem nextItem = playQueueManager.getNextPlayQueueItem();
            if (AdsOperations.isVideoAd(nextItem)) {
                adsOperations.replaceUpcomingVideoAd(ads, (VideoQueueItem) nextItem);
                didReplaceNextAd = true;
            } else if (!AdsOperations.isAudioAd(nextItem) && ads.audioAd().isPresent()) {
                adsOperations.insertAudioAd((TrackQueueItem) nextItem, ads.audioAd().get());
                didReplaceNextAd = true;
            }
        }
    }

    public void publishAdDeliveryEventIfUpcoming() {
        final Urn monetizableUrn = getUpcomingMonetizableUrn();
        if (!monetizableUrn.equals(Urn.NOT_SET) && adsForNextTrack.isPresent()) {
            final String endpoint = String.format(ApiEndpoints.ADS.path(), monetizableUrn.toEncodedString());
            final Optional<AdData> nextTrackAdData = adsOperations.getNextTrackAdData();
            final Urn selectedAdUrn = nextTrackAdData.isPresent() ? nextTrackAdData.get().getAdUrn() : Urn.NOT_SET;
            eventBus.publish(EventQueue.TRACKING,
                    AdDeliveryEvent.adDelivered(monetizableUrn,
                            selectedAdUrn, endpoint, adsForNextTrack.get().toAdsReceived(),
                            didReplaceNextAd, isPlayerVisible, isForeground)
            );
        }
    }

    private Urn getUpcomingMonetizableUrn() {
        final List<Urn> upcomingUrns = playQueueManager.getUpcomingPlayQueueItems(2);
        for (Urn monetizableUrn : currentAdsFetches.keySet()) {
            if (upcomingUrns.contains(monetizableUrn)) {
                return monetizableUrn;
            }
        }
        return Urn.NOT_SET;
    }

    public void onPlayStateTransition(PlaybackStateTransition stateTransition) {
        if (adsOperations.isCurrentItemAudioAd() && stateTransition.playbackEnded()) {
            final Optional<AdData> monetizableAdData = adsOperations.getNextTrackAdData();
            if (monetizableAdData.isPresent() && monetizableAdData.get() instanceof OverlayAdData) {
                ((OverlayAdData) monetizableAdData.get()).setMetaAdCompleted();
            }
        }
    }

    private boolean alreadyFetchedAdForTrack(PlayQueueItem playQueueItem) {
        return currentAdsFetches.containsKey(playQueueItem.getUrn());
    }

    private final class FetchAdForNextTrackSubscriber extends DefaultSubscriber<Object> {
        @Override
        public void onNext(Object event) {
            final PlayQueueItem nextItem = playQueueManager.getNextPlayQueueItem();
            final NextTrackSubscriber nextTrackSubscriber = new NextTrackSubscriber(playQueueManager.getCurrentPlayQueueItem());
            createAdsFetchObservable(nextItem, nextTrackSubscriber);
        }
    }

    private final class FetchAdForCurrentTrackSubscriber extends DefaultSubscriber<Object> {
        @Override
        public void onNext(Object event) {
            final PlayQueueItem currentItem = playQueueManager.getCurrentPlayQueueItem();
            final InterstitialSubscriber audioAdSubscriber = new InterstitialSubscriber(currentItem);
            createAdsFetchObservable(currentItem, audioAdSubscriber);
        }
    }

    private void createAdsFetchObservable(PlayQueueItem playQueueItem, DefaultSubscriber<ApiAdsForTrack> adSubscriber) {
        final Observable<ApiAdsForTrack> apiAdsForTrack = trackRepository.track(playQueueItem.getUrn())
                .filter(IS_MONETIZABLE)
                .flatMap(fetchAds)
                .observeOn(AndroidSchedulers.mainThread());

        currentAdsFetches.put(playQueueItem.getUrn(), new AdsFetchOperation(apiAdsForTrack.subscribe(adSubscriber)));
    }

    private class ResetAdsOnTrackChange extends DefaultSubscriber<CurrentPlayQueueItemEvent> {
        @Override
        public void onNext(CurrentPlayQueueItemEvent currentItemEvent) {
            adsForNextTrack = Optional.absent();
            didReplaceNextAd = false;
            Iterator<Map.Entry<Urn, AdsFetchOperation>> iter = currentAdsFetches.entrySet().iterator();
            while (iter.hasNext()) {

                final Map.Entry<Urn, AdsFetchOperation> operation = iter.next();
                final Urn monetizableUrn = operation.getKey();

                if (isNotCurrentOrNextItem(monetizableUrn) || operation.getValue().hasExpired()) {
                    operation.getValue().subscription.unsubscribe();
                    iter.remove();
                }
            }

            skipFailedAdSubscription.unsubscribe();
            if (!adsOperations.isCurrentItemAd()) {
                adsOperations.clearAllAdsFromQueue();
            }
        }

        private boolean isNotCurrentOrNextItem(Urn monetizableUrn) {
            final Urn currentItemUrn = playQueueManager.getCurrentPlayQueueItem().getUrnOrNotSet();
            final Urn nextItemUrn = playQueueManager.getNextPlayQueueItem().getUrnOrNotSet();
            return !currentItemUrn.equals(monetizableUrn) && !nextItemUrn.equals(monetizableUrn);
        }
    }

    private final class NextTrackSubscriber extends DefaultSubscriber<ApiAdsForTrack> {
        private final PlayQueueItem currentItem;

        NextTrackSubscriber(PlayQueueItem currentItem) {
            this.currentItem = currentItem;
        }

        @Override
        public void onNext(ApiAdsForTrack apiAdsForTrack) {
            /*
             * We're checking if we're still at the intended position before we try to insert the ad in the play queue.
             * This is a temporary work-around for a race condition where unsubscribe doesn't happen immediately and
             * we attempt to put an ad in the queue twice. Matthias, please help!
             */
            if (playQueueManager.isCurrentItem(currentItem)) {
                adsForNextTrack = Optional.of(apiAdsForTrack);
                adsOperations.applyAdToUpcomingTrack(apiAdsForTrack);
            }
        }
    }

    private final class InterstitialSubscriber extends DefaultSubscriber<ApiAdsForTrack> {
        private final PlayQueueItem currentItem;

        InterstitialSubscriber(PlayQueueItem currentItem) {
            this.currentItem = currentItem;
        }

        @Override
        public void onNext(ApiAdsForTrack apiAdsForTrack) {
            if (playQueueManager.isCurrentItem(currentItem)) {
                adsOperations.applyInterstitialToTrack(currentItem, apiAdsForTrack);
            }
        }
    }

    private final class SkipFailedAdSubscriber extends DefaultSubscriber<PlaybackStateTransition> {
        @Override
        public void onNext(final PlaybackStateTransition state) {
            skipFailedAdSubscription.unsubscribe();
            skipFailedAdSubscription = Observable.timer(FAILED_AD_WAIT_SECS, TimeUnit.SECONDS, scheduler)
                    .subscribe(new DefaultSubscriber<Long>() {
                        @Override
                        public void onNext(Long args) {
                            Log.i(ADS_TAG, "Skipping ad after waiting " + FAILED_AD_WAIT_SECS + " seconds for it to load.");
                            final AdFailedToBufferEvent event =
                                    new AdFailedToBufferEvent(state.getUrn(), state.getProgress(), FAILED_AD_WAIT_SECS);
                            eventBus.publish(EventQueue.TRACKING, event);
                            playQueueManager.autoMoveToNextPlayableItem();
                        }
                    });
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
            isForeground = latestState.isForeground();
        }
    }

    private class PlayerStateSubscriber extends DefaultSubscriber<PlayerUIEvent> {
        @Override
        public void onNext(PlayerUIEvent latestState) {
            isPlayerVisible = latestState.getKind() == PlayerUIEvent.PLAYER_EXPANDED;
        }
    }
}
