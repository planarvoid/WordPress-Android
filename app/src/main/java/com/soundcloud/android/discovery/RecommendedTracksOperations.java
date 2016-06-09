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
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.sync.recommendations.StoreRecommendationsCommand;
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
            boolean isPlaying = !currentPlayQueueItem.isEmpty() && currentPlayQueueItem.getUrn().equals(trackItem.getUrn());
            recommendations.add(new Recommendation(trackItem, seedUrn, isPlaying, queryPosition, queryUrn));
        }

        return recommendations;
    }

    Func1<PropertySet, Observable<DiscoveryItem>> toBucket = new Func1<PropertySet, Observable<DiscoveryItem>>() {
        @Override
        public Observable<DiscoveryItem> call(PropertySet seed) {
            return tracksForSeed(seed.get(SEED_TRACK_LOCAL_ID)).map(mergeWith(seed));
        }
    };

    private Func1<List<TrackItem>, DiscoveryItem> mergeWith(final PropertySet seed) {
        return new Func1<List<TrackItem>, DiscoveryItem>() {
            @Override
            public DiscoveryItem call(List<TrackItem> trackItems) {
                return new RecommendedTracksItem(seed, toRecommendations(seed, trackItems));
            }
        };
    }

    private final RecommendedTracksSyncInitiator recommendedTracksSyncInitiator;
    private final RecommendationsStorage recommendationsStorage;
    private final StoreRecommendationsCommand storeRecommendationsCommand;
    private final PlayQueueManager playQueueManager;
    private final Scheduler scheduler;
    private final FeatureFlags featureFlags;

    @Inject
    RecommendedTracksOperations(RecommendedTracksSyncInitiator recommendedTracksSyncInitiator,
                                RecommendationsStorage recommendationsStorage,
                                StoreRecommendationsCommand storeRecommendationsCommand,
                                PlayQueueManager playQueueManager,
                                @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler,
                                FeatureFlags featureFlags) {
        this.recommendedTracksSyncInitiator = recommendedTracksSyncInitiator;
        this.recommendationsStorage = recommendationsStorage;
        this.storeRecommendationsCommand = storeRecommendationsCommand;
        this.playQueueManager = playQueueManager;
        this.scheduler = scheduler;
        this.featureFlags = featureFlags;
    }

    @VisibleForTesting
    Observable<List<TrackItem>> tracksForSeed(long seedTrackLocalId) {
        return recommendationsStorage.recommendedTracksForSeed(seedTrackLocalId)
                .filter(RxUtils.IS_NOT_EMPTY_LIST)
                .map(TrackItem.fromPropertySets());
    }

    Observable<DiscoveryItem> firstBucket() {
        return syncAndLoadBuckets(loadFirstBucket());
    }

    Observable<DiscoveryItem> allBuckets() {
        return syncAndLoadBuckets(loadAllBuckets());
    }

    private Observable<DiscoveryItem> syncAndLoadBuckets(Observable<DiscoveryItem> storageObservable) {
        if (featureFlags.isEnabled(Flag.DISCOVERY_RECOMMENDATIONS)) {
            return recommendedTracksSyncInitiator
                    .sync()
                    .flatMap(continueWith(storageObservable));
        } else {
            return Observable.empty();
        }
    }

    void clearData() {
        storeRecommendationsCommand.clearTables();
        recommendedTracksSyncInitiator.clearLastSyncTime();
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
