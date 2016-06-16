package com.soundcloud.android.discovery;

import static com.soundcloud.android.discovery.RecommendationProperty.QUERY_POSITION;
import static com.soundcloud.android.discovery.RecommendationProperty.QUERY_URN;
import static com.soundcloud.android.discovery.RecommendationProperty.SEED_TRACK_LOCAL_ID;
import static com.soundcloud.android.discovery.RecommendationProperty.SEED_TRACK_URN;
import static com.soundcloud.android.rx.RxUtils.continueWith;

import com.soundcloud.android.ApplicationModule;
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

class RecommendedTracksOperations {

    private List<Recommendation> toRecommendations(PropertySet seed, List<TrackItem> trackItems) {
        final Urn seedUrn = seed.get(SEED_TRACK_URN);
        final int queryPosition = seed.get(QUERY_POSITION);
        final Urn queryUrn = seed.get(QUERY_URN);
        final List<Recommendation> recommendations = new ArrayList<>(trackItems.size());
        final PlayQueueItem currentPlayQueueItem = playQueueManager.getCurrentPlayQueueItem();

        for (TrackItem trackItem : trackItems) {
            boolean isPlaying = !currentPlayQueueItem.isEmpty() && currentPlayQueueItem.getUrn()
                    .equals(trackItem.getUrn());
            recommendations.add(new Recommendation(trackItem, seedUrn, isPlaying, queryPosition, queryUrn));
        }

        return recommendations;
    }

    private Func1<PropertySet, Observable<DiscoveryItem>> toBucket = new Func1<PropertySet, Observable<DiscoveryItem>>() {
        @Override
        public Observable<DiscoveryItem> call(PropertySet seed) {
            return tracksForSeed(seed.get(SEED_TRACK_LOCAL_ID)).map(mergeWith(seed));
        }
    };

    private Func1<List<TrackItem>, DiscoveryItem> mergeWith(final PropertySet seed) {
        return new Func1<List<TrackItem>, DiscoveryItem>() {
            @Override
            public DiscoveryItem call(List<TrackItem> trackItems) {
                return RecommendedTracksBucketItem.create(seed, toRecommendations(seed, trackItems));
            }
        };
    }

    private final SyncOperations syncOperations;
    private final RecommendationsStorage recommendationsStorage;
    private final StoreRecommendationsCommand storeRecommendationsCommand;
    private final PlayQueueManager playQueueManager;
    private final Scheduler scheduler;

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

    @VisibleForTesting
    Observable<List<TrackItem>> tracksForSeed(long seedTrackLocalId) {
        return recommendationsStorage.recommendedTracksForSeed(seedTrackLocalId)
                .filter(RxUtils.IS_NOT_EMPTY_LIST)
                .map(TrackItem.fromPropertySets());
    }

    Observable<DiscoveryItem> firstBucket() {
        return syncOperations
                .lazySyncIfStale(Syncable.RECOMMENDED_TRACKS)
                .flatMap(continueWith(loadFirstBucket()));
    }

    Observable<DiscoveryItem> allBuckets() {
        return syncOperations
                .lazySyncIfStale(Syncable.RECOMMENDED_TRACKS)
                .flatMap(continueWith(loadAllBuckets()));
    }

    void clearData() {
        storeRecommendationsCommand.clearTables();
    }

    private Observable<DiscoveryItem> loadFirstBucket() {
        return recommendationsStorage.firstSeed()
                .flatMap(toBucket)
                .subscribeOn(scheduler);
    }

    private Observable<DiscoveryItem> loadAllBuckets() {
        return recommendationsStorage.allSeeds()
                .flatMap(toBucket)
                .subscribeOn(scheduler);
    }
}
