package com.soundcloud.android.olddiscovery.recommendations;

import static com.soundcloud.android.sync.SyncOperations.emptyResult;
import static com.soundcloud.android.sync.Syncable.RECOMMENDED_TRACKS;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.olddiscovery.OldDiscoveryItem;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.rx.RxJava;
import com.soundcloud.android.sync.SyncOperations;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRepository;
import com.soundcloud.annotations.VisibleForTesting;
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
    private final TrackItemRepository trackRepository;
    private final Scheduler scheduler;

    @Inject
    RecommendedTracksOperations(SyncOperations syncOperations,
                                RecommendationsStorage recommendationsStorage,
                                StoreRecommendationsCommand storeRecommendationsCommand,
                                PlayQueueManager playQueueManager,
                                TrackItemRepository trackRepository,
                                @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.syncOperations = syncOperations;
        this.recommendationsStorage = recommendationsStorage;
        this.storeRecommendationsCommand = storeRecommendationsCommand;
        this.playQueueManager = playQueueManager;
        this.trackRepository = trackRepository;
        this.scheduler = scheduler;
    }

    @VisibleForTesting
    Observable<List<TrackItem>> tracksForSeed(long seedTrackLocalId) {
        return recommendationsStorage.recommendedTracksForSeed(seedTrackLocalId)
                                     .filter(list -> !list.isEmpty())
                                     .flatMap(urns -> RxJava.toV1Observable(trackRepository.trackListFromUrns(urns)));
    }

    public Observable<OldDiscoveryItem> recommendedTracks() {
        return loadFirstBucket(syncOperations.lazySyncIfStale(RECOMMENDED_TRACKS));
    }

    public Observable<OldDiscoveryItem> refreshRecommendedTracks() {
        return loadFirstBucket(syncOperations.failSafeSync(RECOMMENDED_TRACKS));
    }

    Observable<OldDiscoveryItem> allBuckets() {
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
            recommendations.add(Recommendation.create(trackItem,
                                                      seedUrn,
                                                      isPlaying,
                                                      queryPosition,
                                                      queryUrn));
        }

        return recommendations;
    }

    private Observable<OldDiscoveryItem> loadRecommendedTracks(SyncOperations.Result result) {
        return recommendationsStorage.firstSeed()
                                     .subscribeOn(scheduler)
                                     .flatMap(this::loadDiscoveryItem)
                                     .switchIfEmpty(emptyResult(result));
    }

    private Observable<OldDiscoveryItem> loadFirstBucket(Observable<SyncOperations.Result> source) {
        return source.flatMap(this::loadRecommendedTracks);
    }

    private Observable<OldDiscoveryItem> loadAllBuckets() {
        return recommendationsStorage.allSeeds().concatMapEager(this::loadDiscoveryItem).subscribeOn(scheduler);

    }

    private Observable<OldDiscoveryItem> loadDiscoveryItem(RecommendationSeed seed) {
        return tracksForSeed(seed.seedTrackLocalId())
                .map(trackItems -> RecommendedTracksBucketItem.create(seed, toRecommendations(seed, trackItems)));
    }
}
