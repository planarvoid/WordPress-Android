package com.soundcloud.android.discovery.recommendations;

import static com.soundcloud.android.discovery.recommendations.RecommendationProperty.QUERY_POSITION;
import static com.soundcloud.android.discovery.recommendations.RecommendationProperty.QUERY_URN;
import static com.soundcloud.android.discovery.recommendations.RecommendationProperty.SEED_TRACK_LOCAL_ID;
import static com.soundcloud.android.discovery.recommendations.RecommendationProperty.SEED_TRACK_URN;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.discovery.DiscoveryItem;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.sync.SyncOperations;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.collections.PropertySet;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;

public class RecommendedTracksOperations {

    private final SyncOperations syncOperations;
    private final RecommendationsStorage recommendationsStorage;
    private final StoreRecommendationsCommand storeRecommendationsCommand;
    private final PlayQueueManager playQueueManager;
    private final Scheduler scheduler;
    private final Func1<SyncOperations.Result, Observable<DiscoveryItem>> loadRecommendedTracks = new Func1<SyncOperations.Result, Observable<DiscoveryItem>>() {
        @Override
        public Observable<DiscoveryItem> call(SyncOperations.Result result) {
            return recommendationsStorage.firstSeed()
                                         .subscribeOn(scheduler)
                                         .flatMap(toDiscoveryItem)
                                         .switchIfEmpty(SyncOperations.<DiscoveryItem>emptyResult(
                                                 result));
        }
    };
    private final Func1<PropertySet, Observable<DiscoveryItem>> toDiscoveryItem = new Func1<PropertySet, Observable<DiscoveryItem>>() {
        @Override
        public Observable<DiscoveryItem> call(PropertySet seed) {
            return tracksForSeed(seed.get(SEED_TRACK_LOCAL_ID)).map(mergeWith(seed));
        }
    };

    @Inject
    RecommendedTracksOperations(SyncOperations syncOperations,
                                RecommendationsStorage recommendationsStorage,
                                StoreRecommendationsCommand storeRecommendationsCommand,
                                PlayQueueManager playQueueManager,
                                @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.syncOperations = syncOperations;
        this.recommendationsStorage = recommendationsStorage;
        this.storeRecommendationsCommand = storeRecommendationsCommand;
        this.playQueueManager = playQueueManager;
        this.scheduler = scheduler;
    }

    private List<Recommendation> toRecommendations(PropertySet seed, List<TrackItem> trackItems) {
        Urn seedUrn = seed.get(SEED_TRACK_URN);
        int queryPosition = seed.get(QUERY_POSITION);
        Urn queryUrn = seed.get(QUERY_URN);
        List<Recommendation> recommendations = new ArrayList<>(trackItems.size());
        PlayQueueItem currentPlayQueueItem = playQueueManager.getCurrentPlayQueueItem();

        for (TrackItem trackItem : trackItems) {
            boolean isPlaying = !currentPlayQueueItem.isEmpty() && currentPlayQueueItem.getUrn()
                                                                                       .equals(trackItem
                                                                                                       .getUrn());
            recommendations.add(new Recommendation(trackItem,
                                                   seedUrn,
                                                   isPlaying,
                                                   queryPosition,
                                                   queryUrn));
        }

        return recommendations;
    }

    private Func1<List<TrackItem>, DiscoveryItem> mergeWith(final PropertySet seed) {
        return new Func1<List<TrackItem>, DiscoveryItem>() {
            @Override
            public DiscoveryItem call(List<TrackItem> trackItems) {
                return RecommendedTracksBucketItem.create(seed,
                                                          toRecommendations(seed, trackItems));
            }
        };
    }

    private Observable<DiscoveryItem> loadFirstBucket(Observable<SyncOperations.Result> source) {
        return source.flatMap(loadRecommendedTracks);
    }

    private Observable<DiscoveryItem> loadAllBuckets() {
        return recommendationsStorage.allSeeds()
                                     .flatMap(toDiscoveryItem)
                                     .subscribeOn(scheduler);

    }

    @VisibleForTesting
    Observable<List<TrackItem>> tracksForSeed(long seedTrackLocalId) {
        return recommendationsStorage.recommendedTracksForSeed(seedTrackLocalId)
                                     .filter(RxUtils.IS_NOT_EMPTY_LIST)
                                     .map(TrackItem.fromPropertySets());
    }

    public Observable<DiscoveryItem> recommendedTracks() {
        return loadFirstBucket(syncOperations.lazySyncIfStale(Syncable.RECOMMENDED_TRACKS));
    }

    public Observable<DiscoveryItem> refreshRecommendedTracks() {
        return loadFirstBucket(syncOperations.failSafeSync(Syncable.RECOMMENDED_TRACKS));
    }

    Observable<DiscoveryItem> allBuckets() {
        return syncOperations
                .lazySyncIfStale(Syncable.RECOMMENDED_TRACKS)
                .flatMap(new Func1<SyncOperations.Result, Observable<DiscoveryItem>>() {
                    @Override
                    public Observable<DiscoveryItem> call(SyncOperations.Result result) {
                        return loadAllBuckets()
                                .switchIfEmpty(SyncOperations.<DiscoveryItem>emptyResult(result));
                    }
                });
    }

    public void clearData() {
        storeRecommendationsCommand.clearTables();
    }
}
