package com.soundcloud.android.collection;

import static com.soundcloud.android.events.EventQueue.ENTITY_STATE_CHANGED;
import static com.soundcloud.android.events.EventQueue.PLAY_HISTORY;
import static com.soundcloud.android.events.PlayHistoryEvent.IS_PLAY_HISTORY_ADDED;
import static com.soundcloud.android.offline.OfflineState.NOT_OFFLINE;
import static com.soundcloud.android.rx.RxUtils.continueWith;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.collection.playhistory.PlayHistoryOperations;
import com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedItem;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.likes.LikeProperty;
import com.soundcloud.android.likes.PlaylistLikesStorage;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.offline.OfflineStateOperations;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PlaylistPostStorage;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.stations.StationRecord;
import com.soundcloud.android.stations.StationsCollectionsTypes;
import com.soundcloud.android.stations.StationsOperations;
import com.soundcloud.android.sync.LegacySyncContent;
import com.soundcloud.android.sync.LegacySyncInitiator;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.android.sync.SyncStateStorage;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.collections.Sets;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Notification;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.functions.Func3;
import rx.functions.Func4;

import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class CollectionOperations {

    @VisibleForTesting static final int PLAYLIST_LIMIT = 1000; // Arbitrarily high, we don't want to worry about paging
    private static final int PLAY_HISTORY_LIMIT = 3;

    private final EventBus eventBus;
    private final Scheduler scheduler;
    private final SyncStateStorage syncStateStorage;
    private final PlaylistPostStorage playlistPostStorage;
    private final PlaylistLikesStorage playlistLikesStorage;
    private final LoadLikedTrackPreviewsCommand loadLikedTrackPreviews;
    private final LegacySyncInitiator syncInitiator;
    private final StationsOperations stationsOperations;
    private final CollectionOptionsStorage collectionOptionsStorage;
    private final OfflineStateOperations offlineStateOperations;
    private final PlayHistoryOperations playHistoryOperations;

    private static final Func1<List<PropertySet>, List<PropertySet>> REMOVE_DUPLICATE_PLAYLISTS = new Func1<List<PropertySet>, List<PropertySet>>() {
        @Override
        public List<PropertySet> call(List<PropertySet> propertySets) {
            Set<Urn> uniquePlaylists = Sets.newHashSetWithExpectedSize(propertySets.size());
            for (Iterator<PropertySet> iterator = propertySets.iterator(); iterator.hasNext(); ) {
                final Urn urn = iterator.next().get(PlaylistProperty.URN);
                if (uniquePlaylists.contains(urn)) {
                    iterator.remove();
                } else {
                    uniquePlaylists.add(urn);
                }
            }
            return propertySets;
        }
    };

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
                    return propertySet.contains(LikeProperty.CREATED_AT) ?
                           propertySet.get(LikeProperty.CREATED_AT) :
                           propertySet.get(PostProperty.CREATED_AT);
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


    private static Func2<List<PropertySet>, List<PropertySet>, List<PropertySet>> COMBINE_POSTED_AND_LIKED = new Func2<List<PropertySet>, List<PropertySet>, List<PropertySet>>() {
        @Override
        public List<PropertySet> call(List<PropertySet> postedPlaylists, List<PropertySet> likedPlaylists) {
            List<PropertySet> all = new ArrayList<>(postedPlaylists.size() + likedPlaylists.size());
            all.addAll(postedPlaylists);
            all.addAll(likedPlaylists);
            return all;
        }
    };

    private static final Func3<List<PlaylistItem>, LikesItem, List<StationRecord>, MyCollection> TO_MY_COLLECTIONS = new Func3<List<PlaylistItem>, LikesItem, List<StationRecord>, MyCollection>() {
        @Override
        public MyCollection call(List<PlaylistItem> playlistItems,
                                 LikesItem likes,
                                 List<StationRecord> recentStations) {
            return MyCollection.forCollectionWithPlaylists(likes, playlistItems, recentStations, false);
        }
    };

    private static final Func4<List<PlaylistItem>, LikesItem, List<TrackItem>, List<RecentlyPlayedItem>, MyCollection> TO_MY_COLLECTIONS_FOR_PLAY_HISTORY = new Func4<List<PlaylistItem>, LikesItem, List<TrackItem>, List<RecentlyPlayedItem>, MyCollection>() {
        @Override
        public MyCollection call(List<PlaylistItem> playlistItems,
                                 LikesItem likes,
                                 List<TrackItem> playHistoryTrackItems,
                                 List<RecentlyPlayedItem> recentlyPlayedItems) {
            return MyCollection.forCollectionWithPlayHistory(likes,
                                                             playlistItems,
                                                             playHistoryTrackItems,
                                                             recentlyPlayedItems,
                                                             false);
        }
    };

    private static final Func2<List<LikedTrackPreview>, OfflineState, LikesItem> TO_LIKES_ITEM = new Func2<List<LikedTrackPreview>, OfflineState, LikesItem>() {
        @Override
        public LikesItem call(List<LikedTrackPreview> likedTracks, OfflineState offlineState) {
            return new LikesItem(likedTracks, PropertySet.from(OfflineProperty.OFFLINE_STATE.bind(offlineState)));
        }
    };

    private static final Func3<Notification<List<PlaylistItem>>, Notification<LikesItem>, Notification<List<StationRecord>>, Notification<MyCollection>> TO_MY_COLLECTIONS_OR_ERROR =
            new Func3<Notification<List<PlaylistItem>>, Notification<LikesItem>, Notification<List<StationRecord>>, Notification<MyCollection>>() {
                @Override
                public Notification<MyCollection> call(Notification<List<PlaylistItem>> playlists,
                                                       Notification<LikesItem> likes,
                                                       Notification<List<StationRecord>> recentStations) {
                    if (playlists.isOnCompleted() && likes.isOnCompleted() && recentStations.isOnCompleted()) {
                        return Notification.createOnCompleted();
                    }
                    return Notification.createOnNext(MyCollection.forCollectionWithPlaylists(
                            likes.isOnError() ?
                            LikesItem.fromTrackPreviews(Collections.<LikedTrackPreview>emptyList()) :
                            likes.getValue(),
                            playlists.isOnError() ? Collections.<PlaylistItem>emptyList() : playlists.getValue(),
                            recentStations.isOnError() ?
                            Collections.<StationRecord>emptyList() :
                            recentStations.getValue(),
                            likes.isOnError() || playlists.isOnError() || recentStations.isOnError()
                    ));
                }
            };

    private static final Func4<Notification<List<PlaylistItem>>, Notification<LikesItem>, Notification<List<TrackItem>>, Notification<List<RecentlyPlayedItem>>, Notification<MyCollection>> TO_MY_COLLECTIONS_FOR_PLAY_HISTORY_OR_ERROR =
            new Func4<Notification<List<PlaylistItem>>, Notification<LikesItem>, Notification<List<TrackItem>>, Notification<List<RecentlyPlayedItem>>, Notification<MyCollection>>() {
                @Override
                public Notification<MyCollection> call(Notification<List<PlaylistItem>> playlists,
                                                       Notification<LikesItem> likes,
                                                       Notification<List<TrackItem>> playHistoryTrackItems,
                                                       Notification<List<RecentlyPlayedItem>> recentlyPlayedItems) {
                    if (playlists.isOnCompleted() && likes.isOnCompleted() && playHistoryTrackItems.isOnCompleted() && recentlyPlayedItems
                            .isOnCompleted()) {
                        return Notification.createOnCompleted();
                    }
                    return Notification.createOnNext(MyCollection.forCollectionWithPlayHistory(
                            likes.isOnError() ?
                            LikesItem.fromTrackPreviews(Collections.<LikedTrackPreview>emptyList()) :
                            likes.getValue(),
                            playlists.isOnError() ? Collections.<PlaylistItem>emptyList() : playlists.getValue(),
                            playHistoryTrackItems.isOnError() ?
                            Collections.<TrackItem>emptyList() :
                            playHistoryTrackItems.getValue(),
                            recentlyPlayedItems.isOnError() ?
                            Collections.<RecentlyPlayedItem>emptyList() :
                            recentlyPlayedItems.getValue(),
                            likes.isOnError() || playlists.isOnError() || playHistoryTrackItems.isOnError() || recentlyPlayedItems
                                    .isOnError()
                    ));
                }
            };

    private static final Func1<SyncJobResult, Boolean> IS_RECENT_STATIONS_SYNC_EVENT = new Func1<SyncJobResult, Boolean>() {
        @Override
        public Boolean call(SyncJobResult syncJobResult) {
            if (syncJobResult.getAction().equals(Syncable.RECENT_STATIONS.name())) {
                return syncJobResult.wasChanged();
            } else {
                return false;
            }
        }
    };

    private static final Func1<? super EntityStateChangedEvent, Boolean> IS_COLLECTION_CHANGE_FILTER = new Func1<EntityStateChangedEvent, Boolean>() {
        @Override
        public Boolean call(EntityStateChangedEvent event) {
            switch (event.getKind()) {
                case EntityStateChangedEvent.ENTITY_CREATED:
                case EntityStateChangedEvent.ENTITY_DELETED:
                    return event.getFirstUrn().isPlaylist();
                case EntityStateChangedEvent.LIKE:
                case EntityStateChangedEvent.PLAYLIST_PUSHED_TO_SERVER:
                case EntityStateChangedEvent.RECENT_STATION_UPDATED:
                case EntityStateChangedEvent.PLAYLIST_MARKED_FOR_DOWNLOAD:
                    return true;
                default:
                    return false;
            }
        }
    };

    @Inject
    CollectionOperations(EventBus eventBus,
                         @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler,
                         SyncStateStorage syncStateStorage,
                         PlaylistPostStorage playlistPostStorage,
                         PlaylistLikesStorage playlistLikesStorage,
                         LoadLikedTrackPreviewsCommand loadLikedTrackPreviews,
                         LegacySyncInitiator syncInitiator,
                         StationsOperations stationsOperations,
                         CollectionOptionsStorage collectionOptionsStorage,
                         OfflineStateOperations offlineStateOperations,
                         PlayHistoryOperations playHistoryOperations) {
        this.eventBus = eventBus;
        this.scheduler = scheduler;
        this.syncStateStorage = syncStateStorage;
        this.playlistPostStorage = playlistPostStorage;
        this.playlistLikesStorage = playlistLikesStorage;
        this.loadLikedTrackPreviews = loadLikedTrackPreviews;
        this.syncInitiator = syncInitiator;
        this.stationsOperations = stationsOperations;
        this.collectionOptionsStorage = collectionOptionsStorage;
        this.offlineStateOperations = offlineStateOperations;
        this.playHistoryOperations = playHistoryOperations;
    }

    public Observable<Object> onCollectionChanged() {
        return Observable.merge(
                eventBus.queue(ENTITY_STATE_CHANGED).filter(IS_COLLECTION_CHANGE_FILTER).cast(Object.class),
                eventBus.queue(EventQueue.SYNC_RESULT).filter(IS_RECENT_STATIONS_SYNC_EVENT)
        );
    }

    public Observable<Object> onCollectionChangedWithPlayHistory() {
        return Observable.merge(
                eventBus.queue(ENTITY_STATE_CHANGED).filter(IS_COLLECTION_CHANGE_FILTER).cast(Object.class),
                eventBus.queue(PLAY_HISTORY).filter(IS_PLAY_HISTORY_ADDED)
        );
    }

    public Observable<MyCollection> collections(final PlaylistsOptions options) {
        return Observable.zip(
                myPlaylists(options).materialize(),
                likesItem().materialize(),
                recentStations().materialize(),
                TO_MY_COLLECTIONS_OR_ERROR
        ).dematerialize();
    }

    public Observable<MyCollection> collectionsForPlayHistory() {
        return Observable.zip(
                myPlaylists().materialize(),
                likesItem().materialize(),
                playHistoryItems().materialize(),
                recentlyPlayed().materialize(),
                TO_MY_COLLECTIONS_FOR_PLAY_HISTORY_OR_ERROR
        ).dematerialize();
    }

    private Observable<List<RecentlyPlayedItem>> recentlyPlayed() {
        return playHistoryOperations.recentlyPlayed(PlayHistoryOperations.CAROUSEL_ITEMS);
    }

    public Observable<List<PlaylistItem>> myPlaylists() {
        return myPlaylists(PlaylistsOptions.SHOW_ALL);
    }

    private Observable<List<PlaylistItem>> myPlaylists(final PlaylistsOptions options) {
        return syncStateStorage
                .hasSyncedBefore(LegacySyncContent.MyPlaylists.content.uri)
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

    private Observable<LikesItem> likesItem() {
        return Observable.zip(tracksLiked(),
                              likedTracksOfflineState(),
                              TO_LIKES_ITEM);
    }

    private Observable<List<TrackItem>> playHistoryItems() {
        return playHistoryOperations
                .playHistory(PLAY_HISTORY_LIMIT)
                .subscribeOn(scheduler);
    }

    private Observable<OfflineState> likedTracksOfflineState() {
        return offlineStateOperations.loadLikedTracksOfflineState();
    }

    private Observable<List<LikedTrackPreview>> tracksLiked() {
        return syncStateStorage
                .hasSyncedBefore(LegacySyncContent.MyLikes.content.uri)
                .flatMap(new Func1<Boolean, Observable<List<LikedTrackPreview>>>() {
                    @Override
                    public Observable<List<LikedTrackPreview>> call(Boolean hasSynced) {
                        if (hasSynced) {
                            return likedTrackPreviews();
                        } else {
                            return refreshLikesAndLoadPreviews();
                        }
                    }
                }).subscribeOn(scheduler);
    }

    public Observable<MyCollection> updatedCollections(final PlaylistsOptions options) {
        return Observable.zip(
                refreshAndLoadPlaylists(options),
                Observable.zip(refreshLikesAndLoadPreviews(),
                               likedTracksOfflineState(),
                               TO_LIKES_ITEM),
                refreshRecentStationsAndLoad(),
                TO_MY_COLLECTIONS
        );
    }

    public Observable<MyCollection> updatedCollectionsForPlayHistory() {
        return Observable.zip(
                refreshAndLoadPlaylists(PlaylistsOptions.SHOW_ALL),
                Observable.zip(refreshLikesAndLoadPreviews(),
                               likedTracksOfflineState(),
                               TO_LIKES_ITEM),
                playHistoryItems(),
                recentlyPlayed(),
                TO_MY_COLLECTIONS_FOR_PLAY_HISTORY
        );
    }

    private Observable<List<StationRecord>> refreshRecentStationsAndLoad() {
        return stationsOperations.sync().flatMap(continueWith(recentStations()));
    }

    private Observable<List<LikedTrackPreview>> refreshLikesAndLoadPreviews() {
        return syncInitiator.refreshLikes().flatMap(continueWith(likedTrackPreviews()));
    }

    private Observable<List<LikedTrackPreview>> likedTrackPreviews() {
        return loadLikedTrackPreviews.toObservable(null).subscribeOn(scheduler);
    }

    private Observable<List<StationRecord>> recentStations() {
        return stationsOperations.collection(StationsCollectionsTypes.RECENT).toList();
    }

    private Observable<List<PlaylistItem>> loadPlaylists(PlaylistsOptions options) {
        return unsortedPlaylists(options)
                .map(offlineOnly(options.showOfflineOnly()))
                .map(options.sortByTitle() ? SORT_BY_TITLE : SORT_BY_CREATION)
                .map(REMOVE_DUPLICATE_PLAYLISTS)
                .map(PlaylistItem.fromPropertySets())
                .subscribeOn(scheduler);
    }

    private Func1<List<PropertySet>, List<PropertySet>> offlineOnly(final boolean offlineOnly) {
        return new Func1<List<PropertySet>, List<PropertySet>>() {
            @Override
            public List<PropertySet> call(List<PropertySet> propertySets) {
                if (offlineOnly) {
                    for (Iterator<PropertySet> iterator = propertySets.iterator(); iterator.hasNext(); ) {
                        OfflineState offlineState = iterator.next()
                                                            .getOrElse(OfflineProperty.OFFLINE_STATE, NOT_OFFLINE);

                        if (offlineState.equals(NOT_OFFLINE)) {
                            iterator.remove();
                        }
                    }
                }
                return propertySets;
            }
        };
    }

    private Observable<List<PropertySet>> unsortedPlaylists(PlaylistsOptions options) {
        final Observable<List<PropertySet>> loadLikedPlaylists = playlistLikesStorage.loadLikedPlaylists(PLAYLIST_LIMIT,
                                                                                                         Long.MAX_VALUE);
        final Observable<List<PropertySet>> loadPostedPlaylists = playlistPostStorage.loadPostedPlaylists(PLAYLIST_LIMIT,
                                                                                                          Long.MAX_VALUE);

        if (options.showLikes() && !options.showPosts()) {
            return loadLikedPlaylists;
        } else if (options.showPosts() && !options.showLikes()) {
            return loadPostedPlaylists;
        } else {
            return loadPostedPlaylists.zipWith(loadLikedPlaylists, COMBINE_POSTED_AND_LIKED);
        }
    }

    public void clearData() {
        collectionOptionsStorage.clear();
    }
}
