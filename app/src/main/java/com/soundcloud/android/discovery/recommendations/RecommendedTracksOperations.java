package com.soundcloud.android.discovery.recommendations;

import static com.soundcloud.android.discovery.recommendations.RecommendationProperty.QUERY_POSITION;
import static com.soundcloud.android.discovery.recommendations.RecommendationProperty.QUERY_URN;
import static com.soundcloud.android.discovery.recommendations.RecommendationProperty.SEED_TRACK_URN;
import static com.soundcloud.android.sync.SyncOperations.emptyResult;
import static com.soundcloud.android.sync.Syncable.RECOMMENDED_TRACKS;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.discovery.DiscoveryItem;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.sync.SyncOperations;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.collections.PropertySet;
import rx.Observable;
import rx.Scheduler;

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
                                     .filter(list -> !list.isEmpty())
                                     .map(TrackItem::fromPropertySets);
    }

    public Observable<DiscoveryItem> recommendedTracks() {
        return loadFirstBucket(syncOperations.lazySyncIfStale(RECOMMENDED_TRACKS));
    }

    public Observable<DiscoveryItem> refreshRecommendedTracks() {
        return loadFirstBucket(syncOperations.failSafeSync(RECOMMENDED_TRACKS));
    }

    Observable<DiscoveryItem> allBuckets() {
        return syncOperations
                .lazySyncIfStale(RECOMMENDED_TRACKS)
                .flatMap(result -> loadAllBuckets().switchIfEmpty(emptyResult(result)));
    }

    public void clearData() {
        storeRecommendationsCommand.clearTables();
    }

    private List<Recommendation> toRecommendations(RecommendationSeed seed, List<TrackItem> trackItems) {
        Urn seedUrn = seed.seedTrackUrn();
        int queryPosition = seed.queryPosition();
        Urn queryUrn = seed.queryUrn();
        List<Recommendation> recommendations = new ArrayList<>(trackItems.size());
        PlayQueueItem currentPlayQueueItem = playQueueManager.getCurrentPlayQueueItem();

        for (TrackItem trackItem : trackItems) {
            boolean isPlaying = !currentPlayQueueItem.isEmpty() && currentPlayQueueItem.getUrn()
                                                                                       .equals(trackItem.getUrn());
            recommendations.add(new Recommendation(trackItem,
                                                   seedUrn,
                                                   isPlaying,
                                                   queryPosition,
                                                   queryUrn));
        }

        return recommendations;
    }

    private Observable<DiscoveryItem> loadRecommendedTracks(SyncOperations.Result result) {
        return recommendationsStorage.firstSeed()
                                     .subscribeOn(scheduler)
                                     .flatMap(this::loadDiscoveryItem)
                                     .switchIfEmpty(emptyResult(result));
    }

    private Observable<DiscoveryItem> loadFirstBucket(Observable<SyncOperations.Result> source) {
        return source.flatMap(this::loadRecommendedTracks);
    }

    private Observable<DiscoveryItem> loadAllBuckets() {
        return recommendationsStorage.allSeeds().flatMap(this::loadDiscoveryItem).subscribeOn(scheduler);

    }

    private Observable<DiscoveryItem> loadDiscoveryItem(RecommendationSeed seed) {
        return tracksForSeed(seed.seedTrackLocalId())
                .map(trackItems -> RecommendedTracksBucketItem.create(seed, toRecommendations(seed, trackItems)));
    }
}
