package com.soundcloud.android.offline;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.likes.LikesStorage;
import com.soundcloud.android.model.Association;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.LoadPlaylistTrackUrnsCommand;
import com.soundcloud.android.playlists.PlaylistStorage;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.collections.Pair;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OfflineStateOperations {

    private final LikesStorage likesStorage;
    private final OfflineContentStorage offlineContentStorage;
    private final TrackDownloadsStorage trackDownloadsStorage;
    private final PlaylistStorage playlistStorage;
    private final LoadPlaylistTrackUrnsCommand loadPlaylistTrackUrnsCommand;
    private final Scheduler scheduler;

    @Inject
    OfflineStateOperations(
            LikesStorage likesStorage,
            OfflineContentStorage offlineContentStorage,
            TrackDownloadsStorage trackDownloadsStorage,
            PlaylistStorage playlistStorage,
            LoadPlaylistTrackUrnsCommand loadPlaylistTrackUrnsCommand,
            @Named(ApplicationModule.RX_HIGH_PRIORITY) Scheduler scheduler) {
        this.likesStorage = likesStorage;
        this.offlineContentStorage = offlineContentStorage;
        this.trackDownloadsStorage = trackDownloadsStorage;
        this.playlistStorage = playlistStorage;
        this.loadPlaylistTrackUrnsCommand = loadPlaylistTrackUrnsCommand;
        this.scheduler = scheduler;
    }

    Single<Map<OfflineState, TrackCollections>> loadTracksCollectionsState(Collection<Urn> tracks, OfflineState state) {
        switch (state) {
            case REQUESTED:
            case DOWNLOADING:
                return setState(tracks, state);
            case NOT_OFFLINE:
            case DOWNLOADED:
            case UNAVAILABLE:
                return determineState(tracks);
            default:
                throw new IllegalStateException("Unknown state: " + state);
        }
    }

    public Single<OfflineState> loadLikedTracksOfflineState() {
        return offlineContentStorage.isOfflineLikesEnabled()
                                    .flatMap(enabled -> enabled ? likesOfflineStates().map(this::getCollectionOfflineState) : Single.just(OfflineState.NOT_OFFLINE))
                                    .subscribeOn(scheduler);
    }

    private Single<Map<OfflineState, TrackCollections>> determineState(Collection<Urn> tracks) {
        return Single.zip(loadOfflinePlaylistStateContainingTracks(tracks), areTracksLiked(tracks), loadLikedTrackState(), this::toOfflineStateToTrackCollections);
    }

    private Map<OfflineState, TrackCollections> toOfflineStateToTrackCollections(Map<OfflineState, Collection<Urn>> statesToPlaylists, Boolean isTrackLiked, OfflineState likedTrackState) {
        final Map<OfflineState, TrackCollections> result = createMap();
        for (OfflineState state : OfflineState.values()) {
            final TrackCollections trackCollections = populate(isTrackLiked && state.equals(likedTrackState),
                                                               statesToPlaylists.containsKey(state) ?
                                                               statesToPlaylists.get(state) :
                                                               Collections.emptyList());
            if (isNotEmpty(trackCollections)) {
                result.put(state, trackCollections);
            }
        }
        return result;
    }

    private Single<Map<OfflineState, TrackCollections>> setState(Collection<Urn> tracks, OfflineState newState) {
        return Single.zip(offlinePlaylistsWithTracks(tracks),
                          offlineContentStorage.isOfflineLikesEnabled(),
                          areTracksLiked(tracks),
                          (offlinePlaylists, isOfflineLikedTracksEnabled, areTracksLiked) -> populate(isOfflineLikedTracksEnabled && areTracksLiked, offlinePlaylists))
                     .filter(this::isNotEmpty)
                     .map(trackCollections -> Collections.singletonMap(newState, trackCollections))
                     .toSingle(Collections.emptyMap());
    }

    private Single<List<Urn>> offlinePlaylistsWithTracks(Collection<Urn> tracks) {
        return playlistStorage.loadPlaylistsWithTracks(tracks)
                              .zipWith(offlineContentStorage.getOfflinePlaylists(), (urns, offlinePlaylists) -> {
                                  ArrayList<Urn> playlistsWithTracks = new ArrayList<>(urns);
                                  playlistsWithTracks.retainAll(offlinePlaylists);
                                  return playlistsWithTracks;
                              });
    }

    private boolean isNotEmpty(TrackCollections trackCollections) {
        return trackCollections != TrackCollections.EMPTY;
    }

    private TrackCollections populate(boolean isOfflineLikedTrack, Collection<Urn> playlists) {
        if (isOfflineLikedTrack || !playlists.isEmpty()) {
            return TrackCollections.create(playlists, isOfflineLikedTrack);
        } else {
            return TrackCollections.EMPTY;
        }
    }

    private Single<Map<OfflineState, Collection<Urn>>> loadOfflinePlaylistStateContainingTracks(Collection<Urn> tracks) {
        final Single<List<Urn>> playlists = offlinePlaylistsWithTracks(tracks);
        return loadPlaylistsOfflineState(playlists);
    }

    private Single<Map<OfflineState, Collection<Urn>>> loadPlaylistsOfflineState(Single<List<Urn>> playlists) {
        return playlists.flatMapObservable(Observable::fromIterable)
                        .flatMapSingle(this::getState)
                        .scan(this.<OfflineState, Collection<Urn>>createMap(), (previous, current) -> {
                            getBucket(previous, current.second()).add(current.first());
                            return previous;
                        }).lastOrError();
    }

    Single<Map<OfflineState, Collection<Urn>>> loadPlaylistsOfflineState(List<Urn> playlists) {
        return loadPlaylistsOfflineState(Single.just(playlists));
    }

    private Collection<Urn> getBucket(Map<OfflineState, Collection<Urn>> map, OfflineState state) {
        if (map.containsKey(state)) {
            return map.get(state);
        } else {
            final ArrayList<Urn> bucket = new ArrayList<>();
            map.put(state, bucket);
            return bucket;
        }
    }

    Single<OfflineState> loadLikedTrackState() {
        return offlineContentStorage.isOfflineLikesEnabled()
                                    .filter(isOfflineLikedTracksEnabled -> isOfflineLikedTracksEnabled)
                                    .flatMapSingleElement(ignore -> likesOfflineStates().map(this::getCollectionOfflineState))
                                    .toSingle(OfflineState.NOT_OFFLINE);
    }

    private Single<Collection<OfflineState>> likesOfflineStates() {
        return likesStorage.loadTrackLikes()
                           .flatMap(associations -> trackDownloadsStorage.getOfflineStates(Lists.transform(associations, Association::urn)))
                           .map(Map::values);
    }

    private Single<Pair<Urn, OfflineState>> getState(Urn playlist) {
        return loadPlaylistTrackUrnsCommand.toSingle(playlist)
                                           .flatMap((tracks) -> trackDownloadsStorage.getOfflineStates(tracks))
                                           .map(Map::values)
                                           .map(this::getCollectionOfflineState)
                                           .map(offlineState -> Pair.of(playlist, offlineState));
    }

    private OfflineState getCollectionOfflineState(Collection<OfflineState> tracksOfflineState) {
        return OfflineState.getOfflineState(
                tracksOfflineState.contains(OfflineState.REQUESTED),
                tracksOfflineState.contains(OfflineState.DOWNLOADED),
                tracksOfflineState.contains(OfflineState.UNAVAILABLE)
        );
    }

    private Single<Boolean> areTracksLiked(Collection<Urn> tracks) {
        return likesStorage.loadTrackLikes().filter(likes -> !likes.isEmpty()).map(likes -> Lists.transform(likes, Association::urn).containsAll(tracks)).toSingle(false);
    }

    private <KEY, VALUE> Map<KEY, VALUE> createMap() {
        return new HashMap<>();
    }
}
