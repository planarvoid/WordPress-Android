package com.soundcloud.android.discovery;

import static com.soundcloud.android.discovery.RecommendationProperty.SEED_TRACK_LOCAL_ID;
import static com.soundcloud.android.rx.RxUtils.continueWith;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.sync.recommendations.StoreRecommendationsCommand;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.collections.PropertySet;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;
import rx.functions.Func2;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;

class RecommendedTracksOperations {

    private final Func1<PropertySet, Observable<DiscoveryItem>> toRecommendationBucket = new Func1<PropertySet, Observable<DiscoveryItem>>() {
        @Override
        public Observable<DiscoveryItem> call(PropertySet propertySet) {
            return tracksForSeed(propertySet.get(SEED_TRACK_LOCAL_ID)).map(mergeWith(propertySet));
        }
    };


    private static Func1<List<TrackItem>, DiscoveryItem> mergeWith(final PropertySet seed) {
        return new Func1<List<TrackItem>, DiscoveryItem>() {
            @Override
            public DiscoveryItem call(List<TrackItem> trackItems) {
                return new RecommendationBucket(seed, trackItems);
            }
        };
    }

    private static Func2<List<Urn>, List<Urn>, List<Urn>> toPlaylist(final RecommendationBucket recommendationBucket) {
        return new Func2<List<Urn>, List<Urn>, List<Urn>>() {
            @Override
            public List<Urn> call(List<Urn> previousTracks, List<Urn> subsequentTracks) {
                List<Urn> playList = new ArrayList<>(previousTracks.size() + subsequentTracks.size() + 1);
                playList.addAll(previousTracks);
                playList.add(recommendationBucket.getSeedTrackUrn());
                playList.addAll(subsequentTracks);
                return playList;
            }
        };
    }

    private final RecommendedTracksSyncInitiator recommendedTracksSyncInitiator;
    private final RecommendationsStorage recommendationsStorage;
    private final StoreRecommendationsCommand storeRecommendationsCommand;
    private final Scheduler scheduler;
    private final FeatureFlags featureFlags;


    @Inject
    RecommendedTracksOperations(RecommendedTracksSyncInitiator recommendedTracksSyncInitiator,
                                RecommendationsStorage recommendationsStorage,
                                StoreRecommendationsCommand storeRecommendationsCommand,
                                @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler,
                                FeatureFlags featureFlags) {
        this.recommendedTracksSyncInitiator = recommendedTracksSyncInitiator;
        this.recommendationsStorage = recommendationsStorage;
        this.storeRecommendationsCommand = storeRecommendationsCommand;
        this.scheduler = scheduler;
        this.featureFlags = featureFlags;
    }

    Observable<List<Urn>> tracksWithSeed(final RecommendationBucket recommendationBucket) {
        //When a seed track is played, we always enqueue all recommended tracks
        //but we need to keep the seed track position inside the queue, thus,
        //we query all previous and subsequents tracks, put the seed track in
        //its position and build the list.
        //TODO: Issue: https://github.com/soundcloud/SoundCloud-Android/issues/3705
        return recommendationsStorage.recommendedTracksBeforeSeed(recommendationBucket.getSeedTrackLocalId())
                .zipWith(
                        recommendationsStorage.recommendedTracksAfterSeed(recommendationBucket.getSeedTrackLocalId()),
                        toPlaylist(recommendationBucket))
                .subscribeOn(scheduler);
    }

    Observable<List<Urn>> allTracks() {
        return recommendationsStorage.recommendedTracks();
    }

    Observable<List<TrackItem>> tracksForSeed(long seedTrackLocalId) {
        return recommendationsStorage.recommendedTracksForSeed(seedTrackLocalId)
                .map(TrackItem.fromPropertySets());
    }

    Observable<DiscoveryItem> tracksBucket() {
        if (featureFlags.isEnabled(Flag.DISCOVERY_RECOMMENDATIONS)) {
            return recommendedTracksSyncInitiator.sync()
                                               .flatMap(continueWith(loadFirstRecommendationBucket()));
        } else {
            return Observable.empty();
        }
    }

    void clearData() {
        storeRecommendationsCommand.clearTables();
        recommendedTracksSyncInitiator.clearLastSyncTime();
    }

    private Observable<DiscoveryItem> loadFirstRecommendationBucket() {
        return recommendationsStorage.firstSeed()
                .flatMap(toRecommendationBucket)
                .subscribeOn(scheduler);
    }

}
