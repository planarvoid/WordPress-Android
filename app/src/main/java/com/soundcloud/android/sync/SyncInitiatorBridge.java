package com.soundcloud.android.sync;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.rx.RxJava;
import com.soundcloud.android.rx.RxUtils;
import io.reactivex.Completable;
import io.reactivex.Single;
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

    public Single<Boolean> hasSyncedLikedAndPostedPlaylistsBefore() {
        return Single.just(syncStateStorage.hasSyncedBefore(Syncable.MY_PLAYLISTS) &&
                                       syncStateStorage.hasSyncedBefore(Syncable.PLAYLIST_LIKES));
    }

    public Observable<Void> refreshMyPlaylists() {
        return syncInitiator.sync(Syncable.MY_PLAYLISTS).map(RxUtils.TO_VOID);
    }

    public Completable refreshMyPostedAndLikedPlaylists() {
        return RxJava.toV2Completable(syncInitiator.sync(Syncable.MY_PLAYLISTS))
                .mergeWith(RxJava.toV2Completable(syncInitiator.sync(Syncable.PLAYLIST_LIKES)));
    }

    public Single<Boolean> hasSyncedTrackLikesBefore() {
        return Single.just(syncStateStorage.hasSyncedBefore(Syncable.TRACK_LIKES));
    }

    public Completable refreshLikedTracks() {
        return RxJava.toV2Completable(syncInitiator.sync(Syncable.TRACK_LIKES));
    }

    public Observable<Void> refreshFollowings() {
        return syncInitiator.sync(Syncable.MY_FOLLOWINGS).map(RxUtils.TO_VOID);
    }

    public Observable<Void> syncTrackLikes() {
        return syncInitiator.sync(Syncable.TRACK_LIKES).map(RxUtils.TO_VOID);
    }
}
