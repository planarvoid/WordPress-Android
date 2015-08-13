package com.soundcloud.android.discovery;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.search.PlaylistDiscoveryOperations;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncResult;
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

public class DiscoveryOperations {

    private static final Observable<List<DiscoveryItem>> ON_ERROR_EMPTY_ITEM_LIST =
            Observable.just(Collections.<DiscoveryItem>emptyList());

    private final Func1<SyncResult, Observable<List<DiscoveryItem>>> toSeedTracks =
            new Func1<SyncResult, Observable<List<DiscoveryItem>>>() {
                @Override
                public Observable<List<DiscoveryItem>> call(SyncResult ignored) {
                    return recommendationsStorage.seedTracks()
                            .map(TO_RECOMMENDATIONS)
                            .subscribeOn(scheduler);
                }
            };

    private static Func1<List<PropertySet>, List<DiscoveryItem>> TO_RECOMMENDATIONS =
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

    private static Func1<List<PropertySet>, List<RecommendedTrackItem>> TO_RECOMMENDED_TRACKS =
            new Func1<List<PropertySet>, List<RecommendedTrackItem>>() {
                @Override
                public List<RecommendedTrackItem> call(List<PropertySet> propertySets) {
                    List<RecommendedTrackItem> recommendedTrackItems = new ArrayList<>(propertySets.size());
                    for (PropertySet propertySet : propertySets) {
                        recommendedTrackItems.add(new RecommendedTrackItem(propertySet));
                    }
                    return recommendedTrackItems;
                }
            };

    private final SyncInitiator syncInitiator;
    private final RecommendationsStorage recommendationsStorage;
    private final PlaylistDiscoveryOperations playlistDiscoveryOperations;
    private final Scheduler scheduler;


    @Inject
    DiscoveryOperations(SyncInitiator syncInitiator,
                        RecommendationsStorage recommendationsStorage,
                        PlaylistDiscoveryOperations playlistDiscoveryOperations,
                        @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {

        this.syncInitiator = syncInitiator;
        this.recommendationsStorage = recommendationsStorage;
        this.playlistDiscoveryOperations = playlistDiscoveryOperations;
        this.scheduler = scheduler;
    }

    public Observable<List<DiscoveryItem>> recommendationsAndPlaylistDiscovery() {
        return recommendations().zipWith(playlistDiscovery(), new Func2<List<DiscoveryItem>, List<DiscoveryItem>, List<DiscoveryItem>>() {
            @Override
            public List<DiscoveryItem> call(List<DiscoveryItem> recommendedTracks, List<DiscoveryItem> playlistTags) {
                List<DiscoveryItem> combined = new ArrayList<>(recommendedTracks.size() + playlistTags.size());
                combined.addAll(recommendedTracks);
                combined.addAll(playlistTags);
                return combined;
            }
        });
    }

    private Observable<List<DiscoveryItem>> recommendations() {
        return syncInitiator.syncRecommendations().flatMap(toSeedTracks)
                .onErrorResumeNext(ON_ERROR_EMPTY_ITEM_LIST);
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

    public Observable<List<Urn>> recommendationsWithSeedTrack(long seedTrackLocalId, final Urn seedTrackUrn) {
        return recommendationsForSeedTrack(seedTrackLocalId)
                .map(new Func1<List<Urn>, List<Urn>>() {
                    @Override
                    public List<Urn> call(List<Urn> urns) {
                        urns.add(0, seedTrackUrn);
                        return urns;
                    }
                });
    }

    public Observable<List<Urn>> recommendationsForSeedTrack(long seedTrackLocalId) {
        return recommendationsStorage.recommendedTrackUrnsForSeed(seedTrackLocalId)
                .subscribeOn(scheduler);
    }

    public Observable<List<RecommendedTrackItem>> recommendedTracksForSeed(long seedTrackLocalId) {
        return recommendationsStorage.recommendedTracksForSeed(seedTrackLocalId)
                .map(TO_RECOMMENDED_TRACKS)
                .subscribeOn(scheduler);
    }
}
