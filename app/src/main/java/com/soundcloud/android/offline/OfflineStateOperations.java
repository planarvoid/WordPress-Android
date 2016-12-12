package com.soundcloud.android.offline;

import static com.soundcloud.java.collections.MoreCollections.transform;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.likes.Like;
import com.soundcloud.android.likes.LoadLikedTracksCommand;
import com.soundcloud.android.likes.LoadLikedTracksOfflineStateCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.LoadPlaylistTracksCommand;
import com.soundcloud.java.optional.Optional;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OfflineStateOperations {

    private final IsOfflineLikedTracksEnabledCommand isOfflineLikedTracksEnabledCommand;
    private final LoadOfflinePlaylistsContainingTrackCommand loadOfflinePlaylistsContainingTrackCommand;
    private final LoadLikedTracksCommand loadLikedTracksCommand;
    private final LoadPlaylistTracksCommand loadPlaylistTracksCommand;
    private final LoadLikedTracksOfflineStateCommand loadLikedTracksOfflineStateCommand;

    private final OfflineContentStorage offlineContentStorage;
    private final TrackDownloadsStorage trackDownloadsStorage;
    private final Scheduler scheduler;

    private final Func1<Boolean, Observable<OfflineState>> TO_OFFLINE_LIKES_STATE = new Func1<Boolean, Observable<OfflineState>>() {
        @Override
        public Observable<OfflineState> call(Boolean enabled) {
            if (enabled) {
                return trackDownloadsStorage.getLikesOfflineState();
            } else {
                return Observable.just(OfflineState.NOT_OFFLINE);
            }
        }
    };

    @Inject
    OfflineStateOperations(
            IsOfflineLikedTracksEnabledCommand isOfflineLikedTracksEnabledCommand,
            LoadOfflinePlaylistsContainingTrackCommand loadOfflinePlaylistsContainingTrackCommand,
            LoadLikedTracksCommand loadLikedTracksCommand,
            LoadPlaylistTracksCommand loadPlaylistTracksCommand,
            LoadLikedTracksOfflineStateCommand loadLikedTracksOfflineStateCommand,
            OfflineContentStorage offlineContentStorage,
            TrackDownloadsStorage trackDownloadsStorage,
            @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.isOfflineLikedTracksEnabledCommand = isOfflineLikedTracksEnabledCommand;
        this.loadOfflinePlaylistsContainingTrackCommand = loadOfflinePlaylistsContainingTrackCommand;
        this.loadLikedTracksCommand = loadLikedTracksCommand;
        this.loadPlaylistTracksCommand = loadPlaylistTracksCommand;
        this.loadLikedTracksOfflineStateCommand = loadLikedTracksOfflineStateCommand;
        this.offlineContentStorage = offlineContentStorage;
        this.trackDownloadsStorage = trackDownloadsStorage;
        this.scheduler = scheduler;
    }

    Map<OfflineState, TrackCollections> loadTracksCollectionsState(Urn track, OfflineState state) {
        switch (state) {
            case REQUESTED:
            case DOWNLOADING:
                return setState(track, state);
            case NOT_OFFLINE:
            case DOWNLOADED:
            case UNAVAILABLE:
                return determineState(track);
            default:
                throw new IllegalStateException("Unknown state: " + state);
        }
    }

    public Observable<OfflineState> loadLikedTracksOfflineState() {
        return offlineContentStorage.isOfflineLikesEnabled()
                                    .flatMap(TO_OFFLINE_LIKES_STATE)
                                    .subscribeOn(scheduler);
    }

    private Map<OfflineState, TrackCollections> determineState(Urn track) {
        final HashMap<OfflineState, TrackCollections> map = new HashMap<>();
        final boolean isTrackLiked = isTrackLiked(track);
        final HashMap<OfflineState, Collection<Urn>> playlistsState = loadOfflinePlaylistsContainingTrack(track);

        final OfflineState likedTrackState = loadLikedTrackState();
        for (OfflineState state : OfflineState.values()) {
            final Collection<Urn> playlists = playlistsState.containsKey(state)
                                              ? playlistsState.get(state)
                                              : Collections.emptyList();
            final TrackCollections collections = populate(isTrackLiked && state.equals(likedTrackState), playlists);
            if (collections != TrackCollections.EMPTY) {
                map.put(state, collections);
            }
        }
        return map;
    }

    private Map<OfflineState, TrackCollections> setState(Urn track, OfflineState newState) {
        final HashMap<OfflineState, TrackCollections> map = new HashMap<>();
        final boolean isOfflineLikedTrack = isOfflineLikedTracksEnabledCommand.call(null) && isTrackLiked(track);
        final List<Urn> offlinePlaylists = loadOfflinePlaylistsContainingTrackCommand.call(track);

        final TrackCollections collections = populate(isOfflineLikedTrack, offlinePlaylists);
        if (collections != TrackCollections.EMPTY) {
            map.put(newState, collections);
        }
        return map;
    }

    private TrackCollections populate(boolean isOfflineLikedTrack, Collection<Urn> playlists) {
        if (isOfflineLikedTrack || !playlists.isEmpty()) {
            return TrackCollections.create(playlists, isOfflineLikedTrack);
        } else {
            return TrackCollections.EMPTY;
        }
    }

    private HashMap<OfflineState, Collection<Urn>> loadOfflinePlaylistsContainingTrack(Urn track) {
        final HashMap<OfflineState, Collection<Urn>> map = new HashMap<>();
        final List<Urn> playlists = loadOfflinePlaylistsContainingTrackCommand.call(track);
        for (Urn playlist : playlists) {
            final OfflineState state = getState(playlist);
            getBucket(map, state).add(playlist);
        }
        return map;
    }

    private Collection<Urn> getBucket(HashMap<OfflineState, Collection<Urn>> map, OfflineState state) {
        if (map.containsKey(state)) {
            return map.get(state);
        } else {
            final ArrayList<Urn> bucket = new ArrayList<>();
            map.put(state, bucket);
            return bucket;
        }
    }

    private OfflineState loadLikedTrackState() {
        if (isOfflineLikedTracksEnabledCommand.call(null)) {
            return getCollectionOfflineState(loadLikedTracksOfflineStateCommand.call(null));
        } else {
            return OfflineState.NOT_OFFLINE;
        }
    }

    private OfflineState getState(Urn playlist) {
        return getCollectionOfflineState(transform(loadPlaylistTracksCommand.call(playlist),
                                                   track -> track.get(OfflineProperty.OFFLINE_STATE)));
    }

    private OfflineState getCollectionOfflineState(Collection<OfflineState> tracksOfflineState) {
        if (tracksOfflineState.contains(OfflineState.REQUESTED)) {
            return OfflineState.REQUESTED;
        } else if (tracksOfflineState.contains(OfflineState.DOWNLOADED)) {
            return OfflineState.DOWNLOADED;
        } else if (tracksOfflineState.contains(OfflineState.UNAVAILABLE)) {
            return OfflineState.UNAVAILABLE;
        } else {
            return OfflineState.REQUESTED;
        }
    }

    private boolean isTrackLiked(Urn track) {
        return transform(loadLikedTracksCommand.call(Optional.absent()), Like::urn).contains(track);
    }

}
