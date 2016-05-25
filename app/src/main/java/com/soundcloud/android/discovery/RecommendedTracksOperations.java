package com.soundcloud.android.discovery;

import static com.soundcloud.android.discovery.RecommendationProperty.SEED_TRACK_LOCAL_ID;
import static com.soundcloud.android.discovery.RecommendationProperty.SEED_TRACK_URN;
import static com.soundcloud.android.rx.RxUtils.continueWith;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.sync.recommendations.StoreRecommendationsCommand;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.collections.PropertySet;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;

class RecommendedTracksOperations {
    private List<Recommendation> toRecommendations(final Urn seedUrn, List<TrackItem> trackItems) {
        final List<Recommendation> recommendations = new ArrayList<>(trackItems.size());
        final PlayQueueItem currentPlayQueueItem = playQueueManager.getCurrentPlayQueueItem();

        for (TrackItem trackItem : trackItems) {
            boolean isPlaying = !currentPlayQueueItem.isEmpty() && currentPlayQueueItem.getUrn().equals(trackItem.getUrn());
            recommendations.add(new Recommendation(trackItem, seedUrn, isPlaying));
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
                return new RecommendationBucket(seed, toRecommendations(seed.get(SEED_TRACK_URN), trackItems));
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

    Observable<List<TrackItem>> tracksForSeed(long seedTrackLocalId) {
        return recommendationsStorage.recommendedTracksForSeed(seedTrackLocalId)
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
            return recommendedTracksSyncInitiator.sync()
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
