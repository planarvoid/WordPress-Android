package com.soundcloud.android.playlists;

import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.PlaylistUrn;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.model.User;
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

public class PlaylistOperations {

    private static final String LOG_TAG = "PlaylistOperations";

    private final PlaylistStorage mPlaylistStorage;
    private final SoundAssociationStorage mSoundAssocStorage;
    private final SyncInitiator mSyncInitiator;
    private final SyncStateManager mSyncStateManager;

    /**
     * Function which tests whether a playlist has to be synced (e.g. because it has become stale), or if not
     * emits the given playlist immediately.
     */
    private final Func1<Playlist, Observable<Playlist>> mSyncIfNecessary = new Func1<Playlist, Observable<Playlist>>() {
        @Override
        public Observable<Playlist> call(Playlist playlist) {
            LocalCollection syncState = mSyncStateManager.fromContent(playlist.toUri());
            if (Playlist.isLocal(playlist.getId())) {
                Log.d(LOG_TAG, "Requesting sync on local playlist " + playlist);
                mSyncInitiator.syncLocalPlaylists();
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
    public PlaylistOperations(PlaylistStorage playlistStorage, SoundAssociationStorage soundAssocStorage,
                              SyncInitiator syncInitiator, SyncStateManager syncStateManager) {
        this.mPlaylistStorage = playlistStorage;
        this.mSoundAssocStorage = soundAssocStorage;
        this.mSyncInitiator = syncInitiator;
        this.mSyncStateManager = syncStateManager;
    }

    public Observable<Playlist> createNewPlaylist(
            User currentUser, String title, boolean isPrivate, long firstTrackId) {
        // insert the new playlist into the database
        return mPlaylistStorage.createNewUserPlaylistAsync(currentUser, title, isPrivate, firstTrackId)
                .mergeMap(handlePlaylistStored())
                .mergeMap(handlePlaylistCreationStored());
    }

    public Observable<Playlist> loadPlaylist(final PlaylistUrn playlistUrn) {
        Log.d(LOG_TAG, "Loading playlist " + playlistUrn);
        return mPlaylistStorage.loadPlaylistWithTracksAsync(playlistUrn.numericId)
                .mergeMap(mSyncIfNecessary)
                .onErrorResumeNext(handlePlaylistNotFound(playlistUrn));
    }

    public Observable<Playlist> refreshPlaylist(final PlaylistUrn playlistUrn) {
        Log.d(LOG_TAG, "Refreshing playlist " + playlistUrn);
        return syncThenLoadPlaylist(playlistUrn);
    }

    /**
     * If a playlist cannot be found in local storage, returns a sync sequence for resume purposes, otherwise
     * simply propagates the error.
     */
    private Func1<Throwable, Observable<? extends Playlist>> handlePlaylistNotFound(final PlaylistUrn playlistUrn) {
        return new Func1<Throwable, Observable<? extends Playlist>>() {
            @Override
            public Observable<? extends Playlist> call(Throwable throwable) {
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
    private Observable<Playlist> syncThenLoadPlaylist(final PlaylistUrn playlistUrn) {
        Log.d(LOG_TAG, "Sending intent to sync playlist " + playlistUrn);
        return mSyncInitiator.syncPlaylist(playlistUrn).mergeMap(new Func1<Boolean, Observable<Playlist>>() {
            @Override
            public Observable<Playlist> call(Boolean playlistWasUpdated) {
                Log.d(LOG_TAG, "Reloading playlist from local storage: " + playlistUrn);
                return mPlaylistStorage.loadPlaylistWithTracksAsync(playlistUrn.numericId);
            }
        });
    }

    private Func1<Playlist, Observable<SoundAssociation>> handlePlaylistStored() {
        return new Func1<Playlist, Observable<SoundAssociation>>() {
            @Override
            public Observable<SoundAssociation> call(Playlist playlist) {
                // store the newly created playlist as a sound association
                return mSoundAssocStorage.addCreationAsync(playlist);
            }
        };
    }

    private Func1<SoundAssociation, Observable<? extends Playlist>> handlePlaylistCreationStored() {
        return new Func1<SoundAssociation, Observable<? extends Playlist>>() {
            @Override
            public Observable<? extends Playlist> call(SoundAssociation soundAssociation) {
                // force to stale so we know to update the playlists next time it is viewed
                mSyncStateManager.forceToStale(Content.ME_PLAYLISTS);
                mSyncInitiator.requestSystemSync();
                return Observable.from((Playlist) soundAssociation.getPlayable());
            }
        };
    }

    public Observable<Playlist> addTrackToPlaylist(final long playlistId, final long trackId) {
        return mPlaylistStorage.loadPlaylistAsync(playlistId).map(new Func1<Playlist, Playlist>() {
            @Override
            public Playlist call(Playlist playlist) {
                return mPlaylistStorage.addTrackToPlaylist(playlist, trackId);
            }
        }).doOnCompleted(mSyncInitiator.requestSystemSyncAction);
    }

}