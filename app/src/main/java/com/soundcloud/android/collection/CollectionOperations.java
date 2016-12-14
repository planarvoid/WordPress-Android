package com.soundcloud.android.collection;

import static com.soundcloud.android.events.EventQueue.ENTITY_STATE_CHANGED;
import static com.soundcloud.android.events.EventQueue.PLAY_HISTORY;
import static com.soundcloud.android.events.PlayHistoryEvent.IS_PLAY_HISTORY_CHANGE;
import static com.soundcloud.android.rx.RxUtils.continueWith;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.collection.playhistory.PlayHistoryOperations;
import com.soundcloud.android.collection.playlists.MyPlaylistsOperations;
import com.soundcloud.android.collection.playlists.PlaylistsOptions;
import com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedOperations;
import com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedPlayableItem;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.offline.OfflineStateOperations;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.stations.StationRecord;
import com.soundcloud.android.stations.StationsCollectionsTypes;
import com.soundcloud.android.stations.StationsOperations;
import com.soundcloud.android.sync.SyncInitiatorBridge;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Notification;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.functions.Func5;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collections;
import java.util.List;

public class CollectionOperations {

    private static final int PLAY_HISTORY_LIMIT = 3;

    private final EventBus eventBus;
    private final Scheduler scheduler;
    private final LoadLikedTrackPreviewsCommand loadLikedTrackPreviews;
    private final SyncInitiatorBridge syncInitiator;
    private final StationsOperations stationsOperations;
    private final CollectionOptionsStorage collectionOptionsStorage;
    private final OfflineStateOperations offlineStateOperations;
    private final PlayHistoryOperations playHistoryOperations;
    private final RecentlyPlayedOperations recentlyPlayedOperations;
    private final MyPlaylistsOperations myPlaylistsOperations;


    private static final Func5<List<PlaylistItem>, LikesItem, List<StationRecord>, List<TrackItem>, List<RecentlyPlayedPlayableItem>, MyCollection> TO_MY_COLLECTIONS =
            new Func5<List<PlaylistItem>, LikesItem, List<StationRecord>, List<TrackItem>, List<RecentlyPlayedPlayableItem>, MyCollection>() {
                @Override
                public MyCollection call(List<PlaylistItem> playlistItems,
                                         LikesItem likes,
                                         List<StationRecord> stationRecords,
                                         List<TrackItem> playHistoryTrackItems,
                                         List<RecentlyPlayedPlayableItem> recentlyPlayedPlayableItems) {
                    return MyCollection.forCollectionWithPlayHistory(likes,
                                                                     playlistItems,
                                                                     stationRecords,
                                                                     playHistoryTrackItems,
                                                                     recentlyPlayedPlayableItems,
                                                                     false);
                }
            };

    private static final Func2<List<LikedTrackPreview>, OfflineState, LikesItem> TO_LIKES_ITEM = new Func2<List<LikedTrackPreview>, OfflineState, LikesItem>() {
        @Override
        public LikesItem call(List<LikedTrackPreview> likedTracks, OfflineState offlineState) {
            return LikesItem.create(likedTracks, offlineState);
        }
    };

    private static List<PlaylistItem> getPlaylistsPreview(Notification<List<PlaylistItem>> playlists) {
        return playlists.isOnError() ? Collections.emptyList() : playlists.getValue();
    }

    private static LikesItem getLikesPreviews(Notification<LikesItem> likes) {
        return likes.isOnError() ?
               LikesItem.fromTrackPreviews(Collections.emptyList()) :
               likes.getValue();
    }

    private static final Func5<Notification<List<PlaylistItem>>, Notification<LikesItem>, Notification<List<StationRecord>>, Notification<List<TrackItem>>, Notification<List<RecentlyPlayedPlayableItem>>, Notification<MyCollection>> TO_MY_COLLECTIONS_OR_ERROR =
            new Func5<Notification<List<PlaylistItem>>, Notification<LikesItem>, Notification<List<StationRecord>>, Notification<List<TrackItem>>, Notification<List<RecentlyPlayedPlayableItem>>, Notification<MyCollection>>() {
                @Override
                public Notification<MyCollection> call(Notification<List<PlaylistItem>> playlists,
                                                       Notification<LikesItem> likes,
                                                       Notification<List<StationRecord>> stations,
                                                       Notification<List<TrackItem>> playHistoryTrackItems,
                                                       Notification<List<RecentlyPlayedPlayableItem>> recentlyPlayedItems) {
                    if (playlists.isOnCompleted() && likes.isOnCompleted() && stations.isOnCompleted()
                            && playHistoryTrackItems.isOnCompleted() && recentlyPlayedItems.isOnCompleted()) {
                        return Notification.createOnCompleted();
                    }
                    return Notification.createOnNext(MyCollection.forCollectionWithPlayHistory(
                            getLikesPreviews(likes),
                            getPlaylistsPreview(playlists),
                            getStationsPreview(stations),
                            getPlayHistoryTrackItems(playHistoryTrackItems),
                            getRecentlyPlayedPlayableItems(recentlyPlayedItems),
                            likes.isOnError() || playlists.isOnError() || stations.isOnError()
                                    || playHistoryTrackItems.isOnError() || recentlyPlayedItems.isOnError()
                    ));
                }
            };

    private static List<RecentlyPlayedPlayableItem> getRecentlyPlayedPlayableItems(Notification<List<RecentlyPlayedPlayableItem>> recentlyPlayed) {
        return recentlyPlayed.isOnError() ? Collections.emptyList() :
               recentlyPlayed.getValue();
    }

    private static List<TrackItem> getPlayHistoryTrackItems(Notification<List<TrackItem>> trackItems) {
        return trackItems.isOnError() ? Collections.emptyList() : trackItems.getValue();
    }

    private static List<StationRecord> getStationsPreview(Notification<List<StationRecord>> stations) {
        return stations.isOnError() ? Collections.emptyList() : stations.getValue();
    }

    private static final Func1<? super EntityStateChangedEvent, Boolean> IS_COLLECTION_CHANGE_FILTER = new Func1<EntityStateChangedEvent, Boolean>() {
        @Override
        public Boolean call(EntityStateChangedEvent event) {
            switch (event.getKind()) {
                case EntityStateChangedEvent.ENTITY_CREATED:
                case EntityStateChangedEvent.ENTITY_DELETED:
                    return event.getFirstUrn().isPlaylist();
                case EntityStateChangedEvent.LIKE:
                case EntityStateChangedEvent.PLAYLIST_PUSHED_TO_SERVER:
                case EntityStateChangedEvent.STATIONS_COLLECTION_UPDATED:
                    return true;
                default:
                    return false;
            }
        }
    };

    @Inject
    CollectionOperations(EventBus eventBus,
                         @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler,
                         LoadLikedTrackPreviewsCommand loadLikedTrackPreviews,
                         SyncInitiatorBridge syncInitiator,
                         StationsOperations stationsOperations,
                         CollectionOptionsStorage collectionOptionsStorage,
                         OfflineStateOperations offlineStateOperations,
                         PlayHistoryOperations playHistoryOperations,
                         RecentlyPlayedOperations recentlyPlayedOperations,
                         MyPlaylistsOperations myPlaylistsOperations) {
        this.eventBus = eventBus;
        this.scheduler = scheduler;
        this.loadLikedTrackPreviews = loadLikedTrackPreviews;
        this.syncInitiator = syncInitiator;
        this.stationsOperations = stationsOperations;
        this.collectionOptionsStorage = collectionOptionsStorage;
        this.offlineStateOperations = offlineStateOperations;
        this.playHistoryOperations = playHistoryOperations;
        this.recentlyPlayedOperations = recentlyPlayedOperations;
        this.myPlaylistsOperations = myPlaylistsOperations;
    }

    Observable<Object> onCollectionChanged() {
        return Observable.merge(
                eventBus.queue(ENTITY_STATE_CHANGED).filter(IS_COLLECTION_CHANGE_FILTER).cast(Object.class),
                eventBus.queue(PLAY_HISTORY).filter(IS_PLAY_HISTORY_CHANGE)
        );
    }

    public Observable<MyCollection> collections() {
        return Observable.zip(
                myPlaylists().materialize(),
                likesItem().materialize(),
                loadStations().materialize(),
                playHistoryItems().materialize(),
                recentlyPlayed().materialize(),
                TO_MY_COLLECTIONS_OR_ERROR
        ).dematerialize();
    }

    private Observable<List<RecentlyPlayedPlayableItem>> recentlyPlayed() {
        return recentlyPlayedOperations.recentlyPlayed(RecentlyPlayedOperations.CAROUSEL_ITEMS);
    }

    private Observable<List<RecentlyPlayedPlayableItem>> refreshRecentlyPlayedItems() {
        return recentlyPlayedOperations.refreshRecentlyPlayed(RecentlyPlayedOperations.CAROUSEL_ITEMS);
    }

    public Observable<List<PlaylistItem>> myPlaylists() {
        return myPlaylistsOperations.myPlaylists(PlaylistsOptions.SHOW_ALL);
    }

    private Observable<LikesItem> likesItem() {
        return Observable.zip(tracksLiked(),
                              likedTracksOfflineState(),
                              TO_LIKES_ITEM);
    }

    private Observable<List<TrackItem>> playHistoryItems() {
        return playHistoryOperations.playHistory(PLAY_HISTORY_LIMIT);
    }

    private Observable<List<TrackItem>> refreshPlayHistoryItems() {
        return playHistoryOperations.refreshPlayHistory(PLAY_HISTORY_LIMIT);
    }

    private Observable<OfflineState> likedTracksOfflineState() {
        return offlineStateOperations.loadLikedTracksOfflineState();
    }

    private Observable<List<LikedTrackPreview>> tracksLiked() {
        return syncInitiator.hasSyncedTrackLikesBefore()
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

    Observable<MyCollection> updatedCollections() {
        return Observable.zip(
                myPlaylistsOperations.refreshAndLoadPlaylists(PlaylistsOptions.SHOW_ALL),
                Observable.zip(refreshLikesAndLoadPreviews(),
                               likedTracksOfflineState(),
                               TO_LIKES_ITEM),
                refreshStationsAndLoad(),
                refreshPlayHistoryItems(),
                refreshRecentlyPlayedItems(),
                TO_MY_COLLECTIONS
        );
    }

    private Observable<List<LikedTrackPreview>> refreshLikesAndLoadPreviews() {
        return syncInitiator.refreshLikedTracks().flatMap(continueWith(likedTrackPreviews()));
    }

    private Observable<List<LikedTrackPreview>> likedTrackPreviews() {
        return loadLikedTrackPreviews.toObservable(null).subscribeOn(scheduler);
    }

    private Observable<List<StationRecord>> refreshStationsAndLoad() {
        return syncStations().flatMap(continueWith(loadStations()));
    }

    private Observable<SyncJobResult> syncStations() {
        return stationsOperations.syncStations(StationsCollectionsTypes.LIKED);
    }

    private Observable<List<StationRecord>> loadStations() {
        return stationsOperations.collection(StationsCollectionsTypes.LIKED).toList();
    }

    public void clearData() {
        collectionOptionsStorage.clear();
    }
}
