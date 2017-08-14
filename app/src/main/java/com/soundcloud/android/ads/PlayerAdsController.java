package com.soundcloud.android.ads;

import static com.soundcloud.android.utils.Log.ADS_TAG;

import com.soundcloud.android.cast.CastConnectionHelper;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.AdDeliveryEvent;
import com.soundcloud.android.events.AdFailedToBufferEvent;
import com.soundcloud.android.events.AdPlaybackErrorEvent;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlayStateEvent;
import com.soundcloud.android.playback.TrackQueueItem;
import com.soundcloud.android.playback.VideoAdQueueItem;
import com.soundcloud.android.playback.VideoSourceProvider;
import com.soundcloud.android.rx.RxJava;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.android.utils.Log;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBusV2;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Singleton
public class PlayerAdsController {

    public static final int FAILED_AD_WAIT_SECS = 6;
    private static final int MAX_CONCURRENT_AD_FETCHES = 2;
    private static final long DEFAULT_OPERATION_STALE_TIME = TimeUnit.MINUTES.toMillis(10);

    private final EventBusV2 eventBus;
    private final AdsOperations adsOperations;
    private final FeatureOperations featureOperations;
    private final VideoSourceProvider videoSourceProvider;
    private final PlayQueueManager playQueueManager;
    private final TrackRepository trackRepository;
    private final Scheduler scheduler;
    private final CastConnectionHelper castConnectionHelper;
    private final long fetchOperationStaleTime;

    private Subscription skipFailedAdSubscription = RxUtils.invalidSubscription();

    private Map<Urn, AdsFetchOperation> currentAdsFetches = new HashMap<>(MAX_CONCURRENT_AD_FETCHES);
    private Map<Urn, String> adRequestIds = new HashMap<>(MAX_CONCURRENT_AD_FETCHES);

    private Optional<ApiAdsForTrack> adsForNextTrack = Optional.absent();
    private boolean isForeground;
    private boolean isPlayerVisible;

    private final Func1<AdRequestData, Observable<ApiAdsForTrack>> fetchAds = new Func1<AdRequestData, Observable<ApiAdsForTrack>>() {
        @Override
        public Observable<ApiAdsForTrack> call(AdRequestData adRequestData) {
            adRequestIds.put(adRequestData.getMonetizableTrackUrn().get(), adRequestData.getRequestId());
            return adsOperations.ads(adRequestData, isPlayerVisible, isForeground);
        }
    };

    @Inject
    public PlayerAdsController(EventBusV2 eventBus, AdsOperations adsOperations,
                               FeatureOperations featureOperations,
                               VideoSourceProvider videoSourceProvider,
                               PlayQueueManager playQueueManager,
                               TrackRepository trackRepository, CastConnectionHelper castConnectionHelper) {
        this(eventBus, adsOperations, featureOperations, videoSourceProvider, playQueueManager, trackRepository, castConnectionHelper, AndroidSchedulers.mainThread());
    }

    public PlayerAdsController(EventBusV2 eventBus, AdsOperations adsOperations,
                               FeatureOperations featureOperations,
                               VideoSourceProvider videoSourceProvider,
                               PlayQueueManager playQueueManager,
                               TrackRepository trackRepository,
                               CastConnectionHelper castConnectionHelper,
                               Scheduler scheduler) {

        this(eventBus, adsOperations, featureOperations,
             videoSourceProvider, playQueueManager, trackRepository, castConnectionHelper, scheduler, DEFAULT_OPERATION_STALE_TIME);
    }

    public PlayerAdsController(EventBusV2 eventBus, AdsOperations adsOperations,
                               FeatureOperations featureOperations,
                               VideoSourceProvider videoSourceProvider,
                               PlayQueueManager playQueueManager,
                               TrackRepository trackRepository,
                               CastConnectionHelper castConnectionHelper,
                               Scheduler scheduler,
                               long fetchOperationStaleTime) {
        this.eventBus = eventBus;
        this.adsOperations = adsOperations;
        this.featureOperations = featureOperations;
        this.videoSourceProvider = videoSourceProvider;
        this.playQueueManager = playQueueManager;
        this.trackRepository = trackRepository;
        this.scheduler = scheduler;
        this.fetchOperationStaleTime = fetchOperationStaleTime;
        this.castConnectionHelper = castConnectionHelper;
    }

    public void reconfigureAdForNextTrack() {
        if (!isForeground && adsForNextTrack.isPresent() && playQueueManager.hasNextItem()) {
            final ApiAdsForTrack ads = adsForNextTrack.get();
            final PlayQueueItem nextItem = playQueueManager.getNextPlayQueueItem();
            if (nextItem.isVideoAd()) {
                adsOperations.replaceUpcomingVideoAd(ads, (VideoAdQueueItem) nextItem);
            } else if (!nextItem.isAudioAd() && ads.audioAd().isPresent()) {
                adsOperations.insertAudioAd((TrackQueueItem) nextItem, ads.audioAd().get());
            }
        }
    }

    public void publishAdDeliveryEventIfUpcoming() {
        final Urn monetizableUrn = getUpcomingMonetizableUrn();
        if (!monetizableUrn.equals(Urn.NOT_SET) && adsForNextTrack.isPresent() && adRequestIds.containsKey(monetizableUrn)) {
            final Optional<AdData> nextTrackAdData = adsOperations.getNextTrackAdData();
            final Urn selectedAdUrn = nextTrackAdData.isPresent() ? nextTrackAdData.get().adUrn() : Urn.NOT_SET;
            eventBus.publish(EventQueue.TRACKING,
                             AdDeliveryEvent.adDelivered(Optional.of(monetizableUrn),
                                                         selectedAdUrn,
                                                         adRequestIds.get(monetizableUrn),
                                                         isPlayerVisible, isForeground)
            );
        }
    }

    void clearAds() {
        final boolean clearOverlays = true;
        adsForNextTrack = Optional.absent();
        adsOperations.clearAllAdsFromQueue(clearOverlays);
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

    public void onPlayStateChanged(PlayStateEvent playStateEvent) {
        if (adsOperations.isCurrentItemAudioAd() && playStateEvent.playbackEnded()) {
            final Optional<AdData> monetizableAdData = adsOperations.getNextTrackAdData();
            if (monetizableAdData.isPresent() && monetizableAdData.get() instanceof VisualAdData) {
                ((VisualAdData) monetizableAdData.get()).setMetaAdCompleted();
            }
        }
    }

    private boolean alreadyFetchedAdForTrack(PlayQueueItem playQueueItem) {
        return currentAdsFetches.containsKey(playQueueItem.getUrn());
    }

    private void createAdsFetchObservable(PlayQueueItem playQueueItem, DefaultSubscriber<ApiAdsForTrack> adSubscriber) {
        final Observable<ApiAdsForTrack> apiAdsForTrack = Observable.zip(RxJava.toV1Observable(trackRepository.track(playQueueItem.getUrn()))
                                                                               .filter(Track::monetizable)
                                                                               .map(Track::urn),
                                                                         adsOperations.kruxSegments(),
                                                                         AdRequestData::forPlayerAd)
                                                                    .flatMap(fetchAds)
                                                                    .observeOn(AndroidSchedulers.mainThread());

        currentAdsFetches.put(playQueueItem.getUrn(), new AdsFetchOperation(apiAdsForTrack.subscribe(adSubscriber)));
    }

    private boolean shouldFetchInterstitialForCurrentTrack() {
        return featureOperations.shouldRequestAds()
                && !castConnectionHelper.isCasting()
                && playQueueManager.getCurrentPlayQueueItem().isTrack()
                && !adsOperations.isCurrentItemAd()
                && !alreadyFetchedAdForTrack(playQueueManager.getCurrentPlayQueueItem());
    }

    private boolean shouldFetchAudioAdForNextItem() {
        return featureOperations.shouldRequestAds()
                && !castConnectionHelper.isCasting()
                && playQueueManager.hasTrackAsNextItem()
                && !adsOperations.isNextItemAd()
                && !adsOperations.isCurrentItemAd()
                && !alreadyFetchedAdForTrack(playQueueManager.getNextPlayQueueItem());
    }

    void onQueueChangeForAd() {
        if (shouldFetchInterstitialForCurrentTrack()) {
            fetchAdForCurrentTrack();
        }

        if (shouldFetchAudioAdForNextItem()) {
            fetchAdForNextTrack();
        }
    }

    void onPlaybackStateChanged(PlayStateEvent playStateEvent) {
        unsubscribeFailedAdSkip(playStateEvent);

        if (isBufferingAd(playStateEvent)) {
            skipFailedAd(playStateEvent);
        }
    }

    private boolean isBufferingAd(PlayStateEvent state) {
        return state.isBuffering() && adsOperations.isCurrentItemAd();
    }

    private void unsubscribeFailedAdSkip(PlayStateEvent playStateEvent) {
        if (playStateEvent.isPlayerPlaying() || playStateEvent.isPaused()) {
            skipFailedAdSubscription.unsubscribe();
        } else if (playStateEvent.getTransition().wasError() && adsOperations.isCurrentItemAd()) {
            skipFailedAdSubscription.unsubscribe();
            playQueueManager.autoMoveToNextPlayableItem();
        }
    }

    private void fetchAdForNextTrack() {
        final PlayQueueItem nextItem = playQueueManager.getNextPlayQueueItem();
        final NextTrackSubscriber nextTrackSubscriber = new NextTrackSubscriber(playQueueManager.getCurrentPlayQueueItem());
        createAdsFetchObservable(nextItem, nextTrackSubscriber);
    }

    private void fetchAdForCurrentTrack() {
        final PlayQueueItem currentItem = playQueueManager.getCurrentPlayQueueItem();
        final InterstitialSubscriber audioAdSubscriber = new InterstitialSubscriber(currentItem);
        createAdsFetchObservable(currentItem, audioAdSubscriber);
    }

    void onCurrentPlayQueueItem(CurrentPlayQueueItemEvent currentItemEvent) {
        adsForNextTrack = Optional.absent();
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

    private void skipFailedAd(final PlayStateEvent state) {
        skipFailedAdSubscription.unsubscribe();
        skipFailedAdSubscription = Observable.timer(FAILED_AD_WAIT_SECS, TimeUnit.SECONDS, scheduler)
                                             .subscribe(new DefaultSubscriber<Long>() {
                                                 @Override
                                                 public void onNext(Long args) {
                                                     Log.i(ADS_TAG,
                                                           "Skipping ad after waiting " + FAILED_AD_WAIT_SECS + " seconds for it to load.");
                                                     eventBus.publish(EventQueue.TRACKING, createErrorEvent(state));
                                                     playQueueManager.autoMoveToNextPlayableItem();
                                                 }
                                             });
    }

    private TrackingEvent createErrorEvent(PlayStateEvent state) {
        final AdData adData = adsOperations.getCurrentTrackAdData().get();
        if (adData instanceof VideoAd && videoSourceProvider.getCurrentSource().isPresent()) {
            return AdPlaybackErrorEvent.failToBuffer(adData,
                                                     state.getTransition(),
                                                     videoSourceProvider.getCurrentSource().get());
        } else {
            return AdFailedToBufferEvent.create(state.getPlayingItemUrn(),
                                                state.getProgress(),
                                                FAILED_AD_WAIT_SECS);
        }
    }

    void onActivityLifeCycleEvent(ActivityLifeCycleEvent latestState) {
        isForeground = latestState.isForeground();
    }

    void onPlayerState(PlayerUIEvent latestState) {
        isPlayerVisible = latestState.getKind() == PlayerUIEvent.PLAYER_EXPANDED;
    }

    private final class AdsFetchOperation {
        private final Subscription subscription;
        private final long createdAtMillis;

        AdsFetchOperation(Subscription subscription) {
            this.subscription = subscription;
            this.createdAtMillis = System.currentTimeMillis();
        }

        public boolean hasExpired() {
            return System.currentTimeMillis() - createdAtMillis > fetchOperationStaleTime;
        }
    }
}
