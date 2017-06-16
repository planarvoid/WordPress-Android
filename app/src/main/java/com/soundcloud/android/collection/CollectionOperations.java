package com.soundcloud.android.collection;

import static com.soundcloud.android.events.EventQueue.LIKE_CHANGED;
import static com.soundcloud.android.events.EventQueue.PLAYLIST_CHANGED;
import static com.soundcloud.android.events.EventQueue.PLAY_HISTORY;
import static com.soundcloud.android.events.EventQueue.URN_STATE_CHANGED;
import static com.soundcloud.android.events.PlayHistoryEvent.IS_PLAY_HISTORY_CHANGE_PREDICATE;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.collection.playhistory.PlayHistoryOperations;
import com.soundcloud.android.collection.playlists.MyPlaylistsOperations;
import com.soundcloud.android.collection.playlists.PlaylistsOptions;
import com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedOperations;
import com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedPlayableItem;
import com.soundcloud.android.configuration.experiments.PlaylistAndAlbumsPreviewsExperiment;
import com.soundcloud.android.events.PlaylistChangedEvent;
import com.soundcloud.android.events.UrnStateChangedEvent;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.offline.OfflineStateOperations;
import com.soundcloud.android.playlists.Playlist;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.EntityItemCreator;
import com.soundcloud.android.rx.RxJava;
import com.soundcloud.android.stations.StationRecord;
import com.soundcloud.android.stations.StationsCollectionsTypes;
import com.soundcloud.android.stations.StationsOperations;
import com.soundcloud.android.sync.SyncInitiatorBridge;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.rx.eventbus.EventBusV2;
import io.reactivex.Maybe;
import io.reactivex.Notification;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;

import android.support.annotation.NonNull;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CollectionOperations {

    private static final int PLAY_HISTORY_LIMIT = 3;

    private final EventBusV2 eventBus;
    private final Scheduler scheduler;
    private final LoadLikedTrackPreviewsCommand loadLikedTrackPreviews;
    private final SyncInitiatorBridge syncInitiator;
    private final StationsOperations stationsOperations;
    private final CollectionOptionsStorage collectionOptionsStorage;
    private final OfflineStateOperations offlineStateOperations;
    private final PlayHistoryOperations playHistoryOperations;
    private final RecentlyPlayedOperations recentlyPlayedOperations;
    private final MyPlaylistsOperations myPlaylistsOperations;
    private final EntityItemCreator entityItemCreator;
    private final PlaylistAndAlbumsPreviewsExperiment playlistAndAlbumsPreviewsExperiment;

    private static List<PlaylistItem> getPlaylistsPreview(Notification<List<PlaylistItem>> playlists) {
        return playlists.isOnError() ? Collections.emptyList() : playlists.getValue();
    }

    private static LikesItem getLikesPreviews(Notification<LikesItem> likes) {
        return likes.isOnError() ?
               LikesItem.fromTrackPreviews(Collections.emptyList()) :
               likes.getValue();
    }

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

    private static final Predicate<? super UrnStateChangedEvent> IS_COLLECTION_CHANGE_FILTER = event -> {
        switch (event.kind()) {
            case ENTITY_CREATED:
            case ENTITY_DELETED:
                return event.containsPlaylist();
            case STATIONS_COLLECTION_UPDATED:
                return true;
            default:
                return false;
        }
    };

    @Inject
    CollectionOperations(EventBusV2 eventBus,
                         @Named(ApplicationModule.RX_HIGH_PRIORITY) Scheduler scheduler,
                         LoadLikedTrackPreviewsCommand loadLikedTrackPreviews,
                         SyncInitiatorBridge syncInitiator,
                         StationsOperations stationsOperations,
                         CollectionOptionsStorage collectionOptionsStorage,
                         OfflineStateOperations offlineStateOperations,
                         PlayHistoryOperations playHistoryOperations,
                         RecentlyPlayedOperations recentlyPlayedOperations,
                         MyPlaylistsOperations myPlaylistsOperations,
                         EntityItemCreator entityItemCreator,
                         PlaylistAndAlbumsPreviewsExperiment playlistAndAlbumsPreviewsExperiment) {
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
        this.entityItemCreator = entityItemCreator;
        this.playlistAndAlbumsPreviewsExperiment = playlistAndAlbumsPreviewsExperiment;
    }

    Observable<Object> onCollectionChanged() {
        return Observable.merge(
                eventBus.queue(PLAYLIST_CHANGED).filter(event -> event.kind() == PlaylistChangedEvent.Kind.PLAYLIST_PUSHED_TO_SERVER).cast(Object.class),
                eventBus.queue(URN_STATE_CHANGED).filter(IS_COLLECTION_CHANGE_FILTER).cast(Object.class),
                eventBus.queue(LIKE_CHANGED).cast(Object.class),
                eventBus.queue(PLAY_HISTORY).filter(IS_PLAY_HISTORY_CHANGE_PREDICATE)
        );
    }

    public Observable<MyCollection> collections() {
        return Observable.zip(
                myPlaylists().map(toPlaylistsItems()).toObservable().materialize(),
                likesItem().materialize(),
                loadStations().toObservable().materialize(),
                playHistoryItems().materialize(),
                recentlyPlayed().materialize(),
                (playlists, likes, stations, playHistoryTrackItems, recentlyPlayedItems) -> {
                    if (playlists.isOnComplete() && likes.isOnComplete() && stations.isOnComplete()
                            && playHistoryTrackItems.isOnComplete() && recentlyPlayedItems.isOnComplete()) {
                        return Notification.createOnComplete();
                    }
                    return Notification.createOnNext(myCollection(getLikesPreviews(likes),
                                                                  getPlaylistsPreview(playlists),
                                                                  getStationsPreview(stations),
                                                                  getPlayHistoryTrackItems(playHistoryTrackItems),
                                                                  getRecentlyPlayedPlayableItems(recentlyPlayedItems),
                                                                  likes.isOnError() || playlists.isOnError() || stations.isOnError()
                                                                          || playHistoryTrackItems.isOnError() || recentlyPlayedItems.isOnError()));
                }
        ).dematerialize();
    }

    private boolean shouldSeparatePlaylistsAndAlbums() {
        return playlistAndAlbumsPreviewsExperiment.isEnabled();
    }

    private Observable<List<RecentlyPlayedPlayableItem>> recentlyPlayed() {
        return recentlyPlayedOperations.recentlyPlayed(RecentlyPlayedOperations.CAROUSEL_ITEMS).toObservable();
    }

    private Observable<List<RecentlyPlayedPlayableItem>> refreshRecentlyPlayedItems() {
        return recentlyPlayedOperations.refreshRecentlyPlayed(RecentlyPlayedOperations.CAROUSEL_ITEMS).toObservable();
    }

    public Maybe<List<Playlist>> myPlaylists() {
        return myPlaylistsOperations.myPlaylists(PlaylistsOptions.SHOW_ALL);
    }

    private Observable<LikesItem> likesItem() {
        return Observable.zip(tracksLiked(),
                              likedTracksOfflineState(),
                              LikesItem::create);
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
                            .flatMapObservable(hasSynced -> {
                                if (hasSynced) {
                                    return likedTrackPreviews();
                                } else {
                                    return refreshLikesAndLoadPreviews();
                                }
                            }).subscribeOn(scheduler);
    }

    Observable<MyCollection> updatedCollections() {
        return Observable.zip(
                myPlaylistsOperations.refreshAndLoadPlaylists(PlaylistsOptions.SHOW_ALL).map(toPlaylistsItems()).toObservable(),
                Observable.zip(refreshLikesAndLoadPreviews(),
                               likedTracksOfflineState(),
                               LikesItem::create),
                refreshStationsAndLoad().toObservable(),
                refreshPlayHistoryItems(),
                refreshRecentlyPlayedItems(),
                (playlistItems, likes, stationRecords, playHistoryTrackItems, recentlyPlayedPlayableItems) -> myCollection(likes,
                                                                                                                           playlistItems,
                                                                                                                           stationRecords,
                                                                                                                           playHistoryTrackItems,
                                                                                                                           recentlyPlayedPlayableItems,
                                                                                                                           false)
        );
    }

    @NonNull
    private MyCollection myCollection(LikesItem likes,
                                      List<PlaylistItem> playlistsAndAlbums,
                                      List<StationRecord> stations,
                                      List<TrackItem> playHistoryTrackItems,
                                      List<RecentlyPlayedPlayableItem> recentlyPlayedItems,
                                      boolean hasError) {
        if (shouldSeparatePlaylistsAndAlbums()) {
            final List<PlaylistItem> playlists = new ArrayList<>();
            final List<PlaylistItem> albums = new ArrayList<>();
            for (PlaylistItem item : playlistsAndAlbums) {
                if (item.isAlbum()) {
                    albums.add(item);
                } else {
                    playlists.add(item);
                }
            }

            return MyCollection.forCollectionWithPlayHistoryAndSeparatedAlbums(likes, playlists, albums, stations, playHistoryTrackItems, recentlyPlayedItems, hasError);
        } else {
            return MyCollection.forCollectionWithPlayHistory(likes, playlistsAndAlbums, stations, playHistoryTrackItems, recentlyPlayedItems, hasError);
        }
    }

    @NonNull
    private Function<List<Playlist>, List<PlaylistItem>> toPlaylistsItems() {
        return playlists -> Lists.transform(playlists, entityItemCreator::playlistItem);
    }

    private Observable<List<LikedTrackPreview>> refreshLikesAndLoadPreviews() {
        return syncInitiator.refreshLikedTracks()
                            .andThen(likedTrackPreviews());
    }

    private Observable<List<LikedTrackPreview>> likedTrackPreviews() {
        return RxJava.toV2Observable(loadLikedTrackPreviews.toObservable(null)).subscribeOn(scheduler);
    }

    private Single<List<StationRecord>> refreshStationsAndLoad() {
        return syncStations().flatMap(__ -> loadStations());
    }

    private Single<SyncJobResult> syncStations() {
        return stationsOperations.syncStations(StationsCollectionsTypes.LIKED);
    }

    private Single<List<StationRecord>> loadStations() {
        return stationsOperations.collection(StationsCollectionsTypes.LIKED);
    }

    public void clearData() {
        collectionOptionsStorage.clear();
    }
}
