package com.soundcloud.android.collections;

import static com.soundcloud.java.collections.Lists.transform;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.likes.LikeProperty;
import com.soundcloud.android.likes.LoadLikedTrackUrnsCommand;
import com.soundcloud.android.likes.PlaylistLikesStorage;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PlaylistPostStorage;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.stations.Station;
import com.soundcloud.android.stations.StationsCollectionsTypes;
import com.soundcloud.android.stations.StationsOperations;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.android.sync.SyncStateStorage;
import com.soundcloud.java.collections.PropertySet;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.functions.Func3;

import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class CollectionsOperations {

    private static final Func1<List<PropertySet>, List<PropertySet>> SORT_BY_CREATION = new Func1<List<PropertySet>, List<PropertySet>>() {
        @Override
        public List<PropertySet> call(List<PropertySet> propertySets) {
            Collections.sort(propertySets, new Comparator<PropertySet>() {
                @Override
                public int compare(PropertySet lhs, PropertySet rhs) {
                    // flipped as we want reverse chronological order
                    return getAssociationDate(rhs).compareTo(getAssociationDate(lhs));
                }

                private Date getAssociationDate(PropertySet propertySet) {
                    return propertySet.contains(LikeProperty.CREATED_AT) ? propertySet.get(LikeProperty.CREATED_AT) : propertySet.get(PostProperty.CREATED_AT);
                }
            });
            return propertySets;
        }
    };

    private static final Func1<List<PropertySet>, List<PropertySet>> SORT_BY_TITLE = new Func1<List<PropertySet>, List<PropertySet>>() {
        @Override
        public List<PropertySet> call(List<PropertySet> propertySets) {
            Collections.sort(propertySets, new Comparator<PropertySet>() {
                @Override
                public int compare(PropertySet lhs, PropertySet rhs) {
                    return lhs.get(PlaylistProperty.TITLE).compareTo(rhs.get(PlaylistProperty.TITLE));
                }
            });
            return propertySets;
        }
    };

    @VisibleForTesting
    static final int PLAYLIST_LIMIT = 1000; //arbitrarily high, we don't want to worry about paging
    public static final Func2<Boolean, SyncResult, Void> TO_VOID = new Func2<Boolean, SyncResult, Void>() {
        @Override
        public Void call(Boolean aBoolean, SyncResult syncResult) {
            return null;
        }
    };

    private final Scheduler scheduler;
    private final SyncStateStorage syncStateStorage;
    private final PlaylistPostStorage playlistPostStorage;
    private final PlaylistLikesStorage playlistLikesStorage;
    private final LoadLikedTrackUrnsCommand loadLikedTrackUrnsCommand;
    private final SyncInitiator syncInitiator;
    private final StationsOperations stationsOperations;
    private final FeatureFlags featureFlags;
    private final CollectionsOptionsStorage collectionsOptionsStorage;

    private static Func2<List<PropertySet>, List<PropertySet>, List<PropertySet>> COMBINE_POSTED_AND_LIKED = new Func2<List<PropertySet>, List<PropertySet>, List<PropertySet>>() {
        @Override
        public List<PropertySet> call(List<PropertySet> postedPlaylists, List<PropertySet> likedPlaylists) {
            List<PropertySet> all = new ArrayList<>(postedPlaylists.size() + likedPlaylists.size());
            all.addAll(postedPlaylists);
            all.addAll(likedPlaylists);
            return all;
        }
    };

    private static Func3<List<PlaylistItem>, List<Urn>, List<Station>, MyCollections> COMBINE_LIKES_AND_PLAYLISTS_AND_RECENT_STATIONS = new Func3<List<PlaylistItem>, List<Urn>, List<Station>, MyCollections>() {
        @Override
        public MyCollections call(List<PlaylistItem> playlistItems, List<Urn> likes, List<Station> recentStations) {
            return new MyCollections(likes, playlistItems, transform(recentStations, Station.TO_URN));
        }
    };

    @Inject
    CollectionsOperations(@Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler,
                          SyncStateStorage syncStateStorage,
                          PlaylistPostStorage playlistPostStorage,
                          PlaylistLikesStorage playlistLikesStorage,
                          LoadLikedTrackUrnsCommand loadLikedTrackUrnsCommand,
                          SyncInitiator syncInitiator,
                          StationsOperations stationsOperations,
                          FeatureFlags featureFlags,
                          CollectionsOptionsStorage collectionsOptionsStorage) {
        this.scheduler = scheduler;
        this.syncStateStorage = syncStateStorage;
        this.playlistPostStorage = playlistPostStorage;
        this.playlistLikesStorage = playlistLikesStorage;
        this.loadLikedTrackUrnsCommand = loadLikedTrackUrnsCommand;
        this.syncInitiator = syncInitiator;
        this.stationsOperations = stationsOperations;
        this.featureFlags = featureFlags;
        this.collectionsOptionsStorage = collectionsOptionsStorage;
    }

    Observable<MyCollections> collections(final CollectionsOptions options) {
        return syncStateStorage.hasSyncedCollectionsBefore()
                .flatMap(new Func1<Boolean, Observable<MyCollections>>() {
                    @Override
                    public Observable<MyCollections> call(Boolean hasSynced) {
                        return hasSynced ? collectionsFromStorage(options) : updatedCollections(options);
                    }
                }).subscribeOn(scheduler);
    }

    Observable<MyCollections> updatedCollections(final CollectionsOptions options) {
        return syncInitiator.refreshCollections()
                .zipWith(stationsOperations.sync(), TO_VOID)
                .flatMap(new Func1<Void, Observable<MyCollections>>() {
                    @Override
                    public Observable<MyCollections> call(Void ignored) {
                        return collectionsFromStorage(options);
                    }
                });
    }

    private Observable<MyCollections> collectionsFromStorage(CollectionsOptions options) {
        return Observable.zip(
                collectionsPlaylists(options),
                loadLikedTrackUrnsCommand.toObservable().subscribeOn(scheduler),
                recentStations().toList(),
                COMBINE_LIKES_AND_PLAYLISTS_AND_RECENT_STATIONS
        );
    }

    private Observable<Station> recentStations() {
        return featureFlags.isEnabled(Flag.STATIONS_SOFT_LAUNCH) ?
                stationsOperations.collection(StationsCollectionsTypes.RECENT) :
                Observable.<Station>empty();
    }

    private Observable<List<PlaylistItem>> collectionsPlaylists(CollectionsOptions options) {
        return unsortedPlaylists(options)
                .map(options.sortByTitle() ? SORT_BY_TITLE : SORT_BY_CREATION)
                .map(PlaylistItem.fromPropertySets())
                .subscribeOn(scheduler);
    }

    private Observable<List<PropertySet>> unsortedPlaylists(CollectionsOptions options) {
        final Observable<List<PropertySet>> loadLikedPlaylists = playlistLikesStorage.loadLikedPlaylists(PLAYLIST_LIMIT, Long.MAX_VALUE);
        final Observable<List<PropertySet>> loadPostedPlaylists = playlistPostStorage.loadPostedPlaylists(PLAYLIST_LIMIT, Long.MAX_VALUE);

        if (options.showLikes() && !options.showPosts()) {
            return loadLikedPlaylists;

        } else if (options.showPosts() && !options.showLikes()) {
            return loadPostedPlaylists;
        } else {
            return loadPostedPlaylists.zipWith(loadLikedPlaylists, COMBINE_POSTED_AND_LIKED);
        }
    }

    public void clearData() {
        collectionsOptionsStorage.clear();
    }
}
