package com.soundcloud.android.playlists;

import com.soundcloud.android.api.legacy.model.LocalCollection;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.legacy.model.SoundAssociation;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.NotFoundException;
import com.soundcloud.android.storage.PlaylistStorage;
import com.soundcloud.android.storage.SoundAssociationStorage;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncStateManager;
import com.soundcloud.android.utils.Log;
import rx.Observable;
import rx.functions.Func1;

import javax.inject.Inject;

public class LegacyPlaylistOperations {

    private static final String LOG_TAG = "PlaylistOperations";

    private final PlaylistStorage playlistStorage;
    private final SoundAssociationStorage soundAssocStorage;
    private final SyncInitiator syncInitiator;
    private final SyncStateManager syncStateManager;

    /**
     * Function which tests whether a playlist has to be synced (e.g. because it has become stale), or if not
     * emits the given playlist immediately.
     */
    private final Func1<PublicApiPlaylist, Observable<PublicApiPlaylist>> syncIfNecessary = new Func1<PublicApiPlaylist, Observable<PublicApiPlaylist>>() {
        @Override
        public Observable<PublicApiPlaylist> call(PublicApiPlaylist playlist) {
            LocalCollection syncState = syncStateManager.fromContent(playlist.toUri());
            if (PublicApiPlaylist.isLocal(playlist.getId())) {
                Log.d(LOG_TAG, "Requesting sync on local playlist " + playlist);
                syncInitiator.syncLocalPlaylists();
                return Observable.just(playlist);
            } else if (syncState.isSyncDue()) {
                Log.d(LOG_TAG, "Checking playlist sync state: stale = " + syncState.isSyncDue());
                return Observable.concat(Observable.just(playlist), syncThenLoadPlaylist(playlist.getUrn()));
            } else {
                Log.d(LOG_TAG, "Playlist up to date, emitting directly");
                return Observable.just(playlist);
            }
        }
    };

    @Inject
    public LegacyPlaylistOperations(PlaylistStorage playlistStorage, SoundAssociationStorage soundAssocStorage,
                                    SyncInitiator syncInitiator, SyncStateManager syncStateManager) {
        this.playlistStorage = playlistStorage;
        this.soundAssocStorage = soundAssocStorage;
        this.syncInitiator = syncInitiator;
        this.syncStateManager = syncStateManager;
    }

    public Observable<PublicApiPlaylist> createNewPlaylist(
            PublicApiUser currentUser, String title, boolean isPrivate, long firstTrackId) {
        // insert the new playlist into the database
        return playlistStorage.createNewUserPlaylistAsync(currentUser, title, isPrivate, firstTrackId)
                .flatMap(handlePlaylistStored())
                .flatMap(handlePlaylistCreationStored());
    }

    public Observable<PublicApiPlaylist> loadPlaylist(final Urn playlistUrn) {
        Log.d(LOG_TAG, "Loading playlist " + playlistUrn);
        return playlistStorage.loadPlaylistWithTracksAsync(playlistUrn.getNumericId())
                .onErrorResumeNext(handlePlaylistNotFound(playlistUrn))
                .flatMap(syncIfNecessary);
    }

    public Observable<PublicApiPlaylist> refreshPlaylist(final Urn playlistUrn) {
        Log.d(LOG_TAG, "Refreshing playlist " + playlistUrn);
        return syncThenLoadPlaylist(playlistUrn);
    }

    /**
     * If a playlist cannot be found in local storage, returns a sync sequence for resume purposes, otherwise
     * simply propagates the error.
     */
    private Func1<Throwable, Observable<? extends PublicApiPlaylist>> handlePlaylistNotFound(final Urn playlistUrn) {
        return new Func1<Throwable, Observable<? extends PublicApiPlaylist>>() {
            @Override
            public Observable<? extends PublicApiPlaylist> call(Throwable throwable) {
                if (throwable instanceof NotFoundException) {
                    Log.d(LOG_TAG, "Playlist missing from local storage, will sync " + playlistUrn);
                    return syncThenLoadPlaylist(playlistUrn);
                }
                Log.d(LOG_TAG, "Caught error, forwarding to observer: " + throwable);
                return Observable.error(throwable);
            }
        };
    }

    /**
     * Performs a sync on the given playlist, then reloads it from local storage.
     */
    private Observable<PublicApiPlaylist> syncThenLoadPlaylist(final Urn playlistUrn) {
        Log.d(LOG_TAG, "Sending intent to sync playlist " + playlistUrn);
        return syncInitiator.syncPlaylist(playlistUrn).flatMap(new Func1<Boolean, Observable<PublicApiPlaylist>>() {
            @Override
            public Observable<PublicApiPlaylist> call(Boolean playlistWasUpdated) {
                Log.d(LOG_TAG, "Reloading playlist from local storage: " + playlistUrn);
                return playlistStorage.loadPlaylistWithTracksAsync(playlistUrn.getNumericId());
            }
        });
    }

    private Func1<PublicApiPlaylist, Observable<SoundAssociation>> handlePlaylistStored() {
        return new Func1<PublicApiPlaylist, Observable<SoundAssociation>>() {
            @Override
            public Observable<SoundAssociation> call(PublicApiPlaylist playlist) {
                // store the newly created playlist as a sound association
                return soundAssocStorage.addCreationAsync(playlist);
            }
        };
    }

    private Func1<SoundAssociation, Observable<? extends PublicApiPlaylist>> handlePlaylistCreationStored() {
        return new Func1<SoundAssociation, Observable<? extends PublicApiPlaylist>>() {
            @Override
            public Observable<? extends PublicApiPlaylist> call(SoundAssociation soundAssociation) {
                // force to stale so we know to update the playlists next time it is viewed
                syncStateManager.forceToStale(Content.ME_PLAYLISTS);
                syncInitiator.requestSystemSync();
                return Observable.just((PublicApiPlaylist) soundAssociation.getPlayable());
            }
        };
    }

    public Observable<PublicApiPlaylist> addTrackToPlaylist(final long playlistId, final long trackId) {
        return playlistStorage.loadPlaylistAsync(playlistId).map(new Func1<PublicApiPlaylist, PublicApiPlaylist>() {
            @Override
            public PublicApiPlaylist call(PublicApiPlaylist playlist) {
                return playlistStorage.addTrackToPlaylist(playlist, trackId);
            }
        }).doOnCompleted(syncInitiator.requestSystemSyncAction());
    }

}