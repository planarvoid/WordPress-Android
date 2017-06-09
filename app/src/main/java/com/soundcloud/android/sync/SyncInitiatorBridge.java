package com.soundcloud.android.sync;

import io.reactivex.Completable;
import io.reactivex.Single;

import javax.inject.Inject;

public class SyncInitiatorBridge {

    private final SyncInitiator syncInitiator;
    private final SyncStateStorage syncStateStorage;

    @Inject
    public SyncInitiatorBridge(SyncInitiator syncInitiator,
                               SyncStateStorage syncStateStorage) {
        this.syncInitiator = syncInitiator;
        this.syncStateStorage = syncStateStorage;
    }

    public void refreshMe() {
        syncInitiator.syncAndForget(Syncable.ME);
    }

    public Single<Boolean> hasSyncedLikedAndPostedPlaylistsBefore() {
        return Single.just(syncStateStorage.hasSyncedBefore(Syncable.MY_PLAYLISTS) &&
                                       syncStateStorage.hasSyncedBefore(Syncable.PLAYLIST_LIKES));
    }

    public Single<SyncJobResult> refreshMyPlaylists() {
        return syncInitiator.sync(Syncable.MY_PLAYLISTS);
    }

    public Completable refreshMyPostedAndLikedPlaylists() {
        return syncInitiator.sync(Syncable.MY_PLAYLISTS).toCompletable().mergeWith(syncInitiator.sync(Syncable.PLAYLIST_LIKES).toCompletable());
    }

    public Single<Boolean> hasSyncedTrackLikesBefore() {
        return Single.just(syncStateStorage.hasSyncedBefore(Syncable.TRACK_LIKES));
    }

    public Completable refreshLikedTracks() {
        return syncInitiator.sync(Syncable.TRACK_LIKES).toCompletable();
    }

    public Single<SyncJobResult> refreshFollowings() {
        return syncInitiator.sync(Syncable.MY_FOLLOWINGS);
    }

    public Single<SyncJobResult> syncTrackLikes() {
        return syncInitiator.sync(Syncable.TRACK_LIKES);
    }
}
