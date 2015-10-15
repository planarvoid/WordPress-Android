package com.soundcloud.android.collections;

import static com.soundcloud.android.rx.RxUtils.continueWith;
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
import com.soundcloud.android.sync.SyncContent;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.android.sync.SyncStateStorage;
import com.soundcloud.java.collections.PropertySet;
import rx.Notification;
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

    private static final Func3<List<PlaylistItem>, List<Urn>, List<Station>, MyCollections> TO_MY_COLLECTIONS = new Func3<List<PlaylistItem>, List<Urn>, List<Station>, MyCollections>() {
        @Override
        public MyCollections call(List<PlaylistItem> playlistItems, List<Urn> likes, List<Station> recentStations) {
            return new MyCollections(likes, playlistItems, transform(recentStations, Station.TO_URN));
        }
    };

    private static final Func3<Notification<List<PlaylistItem>>, Notification<List<Urn>>, Notification<List<Station>>, Notification<MyCollections>> TO_MY_COLLECTIONS_OR_ERROR =
            new Func3<Notification<List<PlaylistItem>>, Notification<List<Urn>>, Notification<List<Station>>, Notification<MyCollections>>() {
                @Override
                public Notification<MyCollections> call(Notification<List<PlaylistItem>> playlists,
                                                        Notification<List<Urn>> likes,
                                                        Notification<List<Station>> recentStations) {
                    if (playlists.isOnError() && likes.isOnError() && recentStations.isOnError()) {
                        return Notification.createOnError(playlists.getThrowable());
                    }
                    if (playlists.isOnCompleted() && likes.isOnCompleted() && recentStations.isOnCompleted()) {
                        return Notification.createOnCompleted();
                    }
                    return Notification.createOnNext(TO_MY_COLLECTIONS.call(
                            playlists.isOnError() ? Collections.<PlaylistItem>emptyList() : playlists.getValue(),
                            likes.isOnError() ? Collections.<Urn>emptyList() : likes.getValue(),
                            recentStations.isOnError() ? Collections.<Station>emptyList() : recentStations.getValue()
                    ));
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

    Observable<MyCollections> collections(final PlaylistsOptions options) {
        return Observable
                .zip(myPlaylists(options).materialize(), tracksLiked().materialize(), recentStations().materialize(), TO_MY_COLLECTIONS_OR_ERROR)
                .dematerialize();
    }

    private Observable<List<PlaylistItem>> myPlaylists(final PlaylistsOptions options) {
        return syncStateStorage
                .hasSyncedBefore(SyncContent.MyPlaylists.content.uri)
                .flatMap(new Func1<Boolean, Observable<List<PlaylistItem>>>() {
                    @Override
                    public Observable<List<PlaylistItem>> call(Boolean hasSynced) {
                        if (hasSynced) {
                            return loadPlaylists(options);
                        } else {
                            return refreshAndLoadPlaylists(options);
                        }
                    }
                }).subscribeOn(scheduler);
    }

    private Observable<List<PlaylistItem>> refreshAndLoadPlaylists(final PlaylistsOptions options) {
        return syncInitiator
                .refreshMyPlaylists()
                .flatMap(continueWith(loadPlaylists(options)));
    }

    private Observable<List<Urn>> tracksLiked() {
        return syncStateStorage
                .hasSyncedBefore(SyncContent.MyLikes.content.uri)
                .flatMap(new Func1<Boolean, Observable<List<Urn>>>() {
                    @Override
                    public Observable<List<Urn>> call(Boolean hasSynced) {
                        if (hasSynced) {
                            return loadTracksLiked();
                        }
                        else {
                            return refreshLikesAndLoadTracksLiked();
                        }
                    }
                }).subscribeOn(scheduler);
    }

    Observable<MyCollections> updatedCollections(final PlaylistsOptions options) {
        return Observable.zip(
                refreshAndLoadPlaylists(options),
                refreshLikesAndLoadTracksLiked(),
                recentStations(),
                TO_MY_COLLECTIONS
        );
    }

    private Observable<List<Urn>> refreshLikesAndLoadTracksLiked() {
        return syncInitiator.refreshLikes().flatMap(continueWith(loadTracksLiked()));
    }

    private Observable<List<Urn>> loadTracksLiked() {
        return loadLikedTrackUrnsCommand.toObservable().subscribeOn(scheduler);
    }

    private Observable<List<Station>> recentStations() {
        final Observable<Station> stations;
        if (featureFlags.isEnabled(Flag.STATIONS_SOFT_LAUNCH)) {
            stations = stationsOperations.collection(StationsCollectionsTypes.RECENT);
        } else {
            stations = Observable.empty();
        }
        return stations.toList();
    }

    private Observable<List<PlaylistItem>> loadPlaylists(PlaylistsOptions options) {
        return unsortedPlaylists(options)
                .map(options.sortByTitle() ? SORT_BY_TITLE : SORT_BY_CREATION)
                .map(PlaylistItem.fromPropertySets())
                .subscribeOn(scheduler);
    }

    private Observable<List<PropertySet>> unsortedPlaylists(PlaylistsOptions options) {
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
