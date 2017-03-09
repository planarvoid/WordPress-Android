package com.soundcloud.android.sync;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.rx.RxUtils;
import rx.Observable;

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
        fireAndForget(syncInitiator.sync(Syncable.ME));
    }

    public Observable<Boolean> hasSyncedLikedAndPostedPlaylistsBefore() {
        return Observable.just(syncStateStorage.hasSyncedBefore(Syncable.MY_PLAYLISTS) &&
                                       syncStateStorage.hasSyncedBefore(Syncable.PLAYLIST_LIKES));
    }

    public Observable<Void> refreshMyPlaylists() {
        return syncInitiator.sync(Syncable.MY_PLAYLISTS).map(RxUtils.TO_VOID);
    }

    public Observable<Void> refreshMyPostedAndLikedPlaylists() {
        return syncInitiator.sync(Syncable.MY_PLAYLISTS)
                            .zipWith(syncInitiator.sync(Syncable.PLAYLIST_LIKES), RxUtils.ZIP_TO_VOID
                            );
    }

    public Observable<Boolean> hasSyncedTrackLikesBefore() {
        return Observable.just(syncStateStorage.hasSyncedBefore(Syncable.TRACK_LIKES));
    }

    public Observable<Void> refreshLikedTracks() {
        return syncInitiator.sync(Syncable.TRACK_LIKES).map(RxUtils.TO_VOID);
    }

    public Observable<Void> refreshFollowings() {
        return syncInitiator.sync(Syncable.MY_FOLLOWINGS).map(RxUtils.TO_VOID);
    }

    public Observable<Void> syncTrackLikes() {
        return syncInitiator.sync(Syncable.TRACK_LIKES).map(RxUtils.TO_VOID);
    }
}
