package com.soundcloud.android.discovery;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.search.PlaylistDiscoveryOperations;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.collections.PropertySet;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;
import rx.functions.Func2;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class DiscoveryOperations {

    private static final Observable<List<DiscoveryItem>> ON_ERROR_EMPTY_ITEM_LIST =
            Observable.just(Collections.<DiscoveryItem>emptyList());

    private final Func1<Boolean, Observable<List<DiscoveryItem>>> toRecommendations =
            new Func1<Boolean, Observable<List<DiscoveryItem>>>() {
                @Override
                public Observable<List<DiscoveryItem>> call(Boolean ignore) {
                    //we always retrieve recommendations from local storage
                    return recommendationsFromStorage();
                }
            };

    private static final Func1<List<PropertySet>, List<DiscoveryItem>> TO_RECOMMENDATIONS =
            new Func1<List<PropertySet>, List<DiscoveryItem>>() {
                @Override
                public List<DiscoveryItem> call(List<PropertySet> propertySets) {
                    List<DiscoveryItem> recommendationItems = new ArrayList<>(propertySets.size());
                    for (PropertySet propertySet : propertySets) {
                        recommendationItems.add(new RecommendationItem(propertySet));
                    }
                    return recommendationItems;
                }
            };

    private final DiscoverySyncer discoverySyncer;
    private final RecommendationsStorage recommendationsStorage;
    private final PlaylistDiscoveryOperations playlistDiscoveryOperations;
    private final Scheduler scheduler;

    @Inject
    DiscoveryOperations(DiscoverySyncer discoverySyncer,
                        RecommendationsStorage recommendationsStorage,
                        PlaylistDiscoveryOperations playlistDiscoveryOperations,
                        @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {

        this.discoverySyncer = discoverySyncer;
        this.recommendationsStorage = recommendationsStorage;
        this.playlistDiscoveryOperations = playlistDiscoveryOperations;
        this.scheduler = scheduler;
    }

    private Observable<List<DiscoveryItem>> recommendations() {
        return discoverySyncer.syncRecommendations()
                .flatMap(toRecommendations)
                .onErrorResumeNext(ON_ERROR_EMPTY_ITEM_LIST);
    }

    private Observable<List<DiscoveryItem>> recommendationsFromStorage() {
        return recommendationsStorage.seedTracks().map(TO_RECOMMENDATIONS).subscribeOn(scheduler);
    }

    private Observable<List<DiscoveryItem>> playlistDiscovery() {
        return playlistDiscoveryOperations.popularPlaylistTags().zipWith(playlistDiscoveryOperations.recentPlaylistTags(),
                new Func2<List<String>, List<String>, List<DiscoveryItem>>() {
                    @Override
                    public List<DiscoveryItem> call(List<String> popular, List<String> recent) {
                        return Collections.<DiscoveryItem>singletonList(new PlaylistDiscoveryItem(popular, recent));
                    }
                }).onErrorResumeNext(ON_ERROR_EMPTY_ITEM_LIST);
    }

    Observable<List<DiscoveryItem>> recommendationsAndPlaylistDiscovery() {
        return recommendations().zipWith(playlistDiscovery(), new Func2<List<DiscoveryItem>, List<DiscoveryItem>, List<DiscoveryItem>>() {
            @Override
            public List<DiscoveryItem> call(List<DiscoveryItem> recommendations, List<DiscoveryItem> playlistTags) {
                List<DiscoveryItem> combined = new ArrayList<>(recommendations.size() + playlistTags.size());
                combined.addAll(recommendations);
                combined.addAll(playlistTags);
                return combined;
            }
        }).subscribeOn(scheduler);
    }

    Observable<List<Urn>> recommendedTracksWithSeed(final RecommendationItem recommendationItem) {
        //When a seed track is played, we always enqueue all recommended tracks
        //but we need to keep the seed track position inside the queue, thus,
        //we query all previous and subsequents tracks, put the seed track in
        //its position and build the list.
        return recommendationsStorage.recommendedTracksBeforeSeed(recommendationItem.getSeedTrackLocalId())
                .zipWith(recommendationsStorage.recommendedTracksAfterSeed(recommendationItem.getSeedTrackLocalId()),
                        new Func2<List<Urn>, List<Urn>, List<Urn>>() {
                            @Override
                            public List<Urn> call(List<Urn> previousTracks, List<Urn> subsequentTracks) {
                                List<Urn> playList = new ArrayList<>(previousTracks.size() + subsequentTracks.size() + 1);
                                playList.addAll(previousTracks);
                                playList.add(recommendationItem.getSeedTrackUrn());
                                playList.addAll(subsequentTracks);
                                return playList;
                            }
                        }).subscribeOn(scheduler);
    }

    Observable<List<Urn>> recommendedTracks() {
        return recommendationsStorage.recommendedTracks().subscribeOn(scheduler);
    }

    Observable<List<TrackItem>> recommendedTracksForSeed(long seedTrackLocalId) {
        return recommendationsStorage.recommendedTracksForSeed(seedTrackLocalId)
                .map(TrackItem.fromPropertySets())
                .subscribeOn(scheduler);
    }
}
