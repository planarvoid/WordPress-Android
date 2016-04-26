package com.soundcloud.android.discovery;

import static com.soundcloud.android.discovery.RecommendationProperty.SEED_TRACK_LOCAL_ID;
import static com.soundcloud.android.rx.RxUtils.continueWith;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.search.PlaylistDiscoveryOperations;
import com.soundcloud.android.sync.recommendations.StoreRecommendationsCommand;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;
import rx.functions.Func2;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DiscoveryOperations {

    private static final Observable<List<DiscoveryItem>> ON_ERROR_EMPTY_ITEM_LIST =
            Observable.just(Collections.<DiscoveryItem>emptyList());

    private static final Func2<Optional<DiscoveryItem>, List<DiscoveryItem>, List<DiscoveryItem>> TO_DISCOVERY_ITEMS_LIST =
            new Func2<Optional<DiscoveryItem>, List<DiscoveryItem>, List<DiscoveryItem>>() {
                @Override
                public List<DiscoveryItem> call(Optional<DiscoveryItem> recommendationBucket, List<DiscoveryItem> playlistTags) {
                    List<DiscoveryItem> combined = new ArrayList<>(recommendationBucket.asSet().size() + playlistTags.size());
                    combined.addAll(recommendationBucket.asSet());
                    combined.addAll(playlistTags);
                    return combined;
                }
            };

    private static final Func2<List<String>, List<String>, List<DiscoveryItem>> TAGS_TO_DISCOVERY_ITEM_LIST =
            new Func2<List<String>, List<String>, List<DiscoveryItem>>() {
                @Override
                public List<DiscoveryItem> call(List<String> popular, List<String> recent) {
                    return Collections.<DiscoveryItem>singletonList(new PlaylistDiscoveryItem(popular, recent));
                }
            };

    private Func1<List<TrackItem>, Optional<DiscoveryItem>> mergeWith(final PropertySet seed) {
        return new Func1<List<TrackItem>, Optional<DiscoveryItem>>() {
            @Override
            public Optional<DiscoveryItem> call(List<TrackItem> trackItems) {
                return Optional.<DiscoveryItem>of(new RecommendationBucket(seed, trackItems));
            }
        };
    }

    private Func1<Optional<PropertySet>, Observable<Optional<DiscoveryItem>>> toRecommendationBucket = new Func1<Optional<PropertySet>, Observable<Optional<DiscoveryItem>>>() {
        @Override
        public Observable<Optional<DiscoveryItem>> call(Optional<PropertySet> seed) {
            if (seed.isPresent()) {
                PropertySet propertyBindings = seed.get();
                return recommendedTracksForSeed(propertyBindings.get(SEED_TRACK_LOCAL_ID)).map(mergeWith(propertyBindings));
            } else {
                return Observable.just(Optional.<DiscoveryItem>absent());
            }
        }
    };

    private Observable<Optional<DiscoveryItem>> loadFirstRecommendationBucket() {
        return recommendationsStorage.firstSeed()
                .flatMap(toRecommendationBucket)
                .subscribeOn(scheduler);
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

    private final RecommendationsSyncInitiator recommendationsSyncInitiator;
    private final RecommendationsStorage recommendationsStorage;
    private final StoreRecommendationsCommand storeRecommendationsCommand;
    private final PlaylistDiscoveryOperations playlistDiscoveryOperations;
    private final Scheduler scheduler;

    @Inject
    DiscoveryOperations(RecommendationsSyncInitiator recommendationsSyncInitiator,
                        RecommendationsStorage recommendationsStorage,
                        StoreRecommendationsCommand storeRecommendationsCommand,
                        PlaylistDiscoveryOperations playlistDiscoveryOperations,
                        @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {

        this.recommendationsSyncInitiator = recommendationsSyncInitiator;
        this.recommendationsStorage = recommendationsStorage;
        this.storeRecommendationsCommand = storeRecommendationsCommand;
        this.playlistDiscoveryOperations = playlistDiscoveryOperations;
        this.scheduler = scheduler;
    }

    private Observable<Optional<DiscoveryItem>> firstRecommendationBucket() {
        return recommendationsSyncInitiator.syncRecommendations()
                .flatMap(continueWith(loadFirstRecommendationBucket()));
    }

    private Observable<List<DiscoveryItem>> searchItem() {
        List<DiscoveryItem> searchItemList = new ArrayList<>(1);
        searchItemList.add(new SearchItem());
        return Observable.just(searchItemList);
    }

    private Observable<List<DiscoveryItem>> playlistDiscovery() {
        return playlistDiscoveryOperations.popularPlaylistTags()
                .zipWith(
                        playlistDiscoveryOperations.recentPlaylistTags(),
                        TAGS_TO_DISCOVERY_ITEM_LIST)
                .onErrorResumeNext(ON_ERROR_EMPTY_ITEM_LIST);
    }

    Observable<List<DiscoveryItem>> discoveryItems() {
        return searchItem()
                .concatWith(playlistDiscovery())
                .subscribeOn(scheduler);
    }

    Observable<List<DiscoveryItem>> discoveryItemsAndRecommendations() {
        return searchItem()
                .concatWith(firstRecommendationBucket()
                        .zipWith(playlistDiscovery(), TO_DISCOVERY_ITEMS_LIST))
                .subscribeOn(scheduler);
    }

    Observable<List<Urn>> recommendedTracksWithSeed(final RecommendationBucket recommendationBucket) {
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

    Observable<List<Urn>> recommendedTracks() {
        return recommendationsStorage.recommendedTracks().subscribeOn(scheduler);
    }

    Observable<List<TrackItem>> recommendedTracksForSeed(long seedTrackLocalId) {
        return recommendationsStorage.recommendedTracksForSeed(seedTrackLocalId)
                .map(TrackItem.fromPropertySets())
                .subscribeOn(scheduler);
    }

    public void clearData() {
        storeRecommendationsCommand.clearTables();
        recommendationsSyncInitiator.clearLastSyncTime();
        playlistDiscoveryOperations.clearData();
    }
}
