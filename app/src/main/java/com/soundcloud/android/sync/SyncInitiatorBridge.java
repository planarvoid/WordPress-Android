package com.soundcloud.android.sync;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.rx.RxUtils;
import rx.Observable;

import javax.inject.Inject;

public class SyncInitiatorBridge {

    private final LegacySyncInitiator legacySyncInitiator;
    private final SyncInitiator syncInitiator;
    private final SyncStateStorage syncStateStorage;
    private final FeatureFlags featureFlags;

    @Inject
    public SyncInitiatorBridge(LegacySyncInitiator legacySyncInitiator,
                               SyncInitiator syncInitiator,
                               SyncStateStorage syncStateStorage,
                               FeatureFlags featureFlags) {
        this.legacySyncInitiator = legacySyncInitiator;
        this.syncInitiator = syncInitiator;
        this.syncStateStorage = syncStateStorage;
        this.featureFlags = featureFlags;
    }

    public void refreshMe() {
        if (featureFlags.isEnabled(Flag.FEATURE_NEW_SYNC_ADAPTER)) {
            fireAndForget(syncInitiator.sync(Syncable.ME));
        } else {
            legacySyncInitiator.syncMe();
        }
    }

    public Observable<Boolean> hasSyncedMyPlaylistsBefore() {
        if (featureFlags.isEnabled(Flag.FEATURE_NEW_SYNC_ADAPTER)) {
            return Observable.just(syncStateStorage.hasSyncedBefore(Syncable.MY_PLAYLISTS));
        } else {
            return syncStateStorage.hasSyncedBefore(LegacySyncContent.MyPlaylists.content.uri);
        }
    }

    public Observable<Void> refreshMyPlaylists() {
        if (featureFlags.isEnabled(Flag.FEATURE_NEW_SYNC_ADAPTER)) {
            return syncInitiator.sync(Syncable.MY_PLAYLISTS).map(RxUtils.TO_VOID);
        } else {
            return legacySyncInitiator.refreshMyPlaylists().map(RxUtils.TO_VOID);
        }
    }

    public Observable<Boolean> hasSyncedMyLikesBefore() {
        if (featureFlags.isEnabled(Flag.FEATURE_NEW_SYNC_ADAPTER)) {
            return Observable.just(syncStateStorage.hasSyncedBefore(Syncable.TRACK_LIKES)
                                           && syncStateStorage.hasSyncedBefore(Syncable.PLAYLIST_LIKES));
        } else {
            return syncStateStorage.hasSyncedBefore(LegacySyncContent.MyLikes.content.uri);
        }
    }

    public Observable<Void> refreshLikes() {
        if (featureFlags.isEnabled(Flag.FEATURE_NEW_SYNC_ADAPTER)) {
            return syncInitiator.sync(Syncable.TRACK_LIKES).mergeWith(syncInitiator.sync(Syncable.PLAYLIST_LIKES))
                                .map(RxUtils.TO_VOID);
        } else {
            return legacySyncInitiator.refreshLikes().map(RxUtils.TO_VOID);
        }
    }

    public Observable<Void> refreshFollowings() {
        if (featureFlags.isEnabled(Flag.FEATURE_NEW_SYNC_ADAPTER)) {
            return syncInitiator.sync(Syncable.MY_FOLLOWINGS).map(RxUtils.TO_VOID);
        } else {
            return legacySyncInitiator.refreshFollowings().map(RxUtils.TO_VOID);
        }
    }

    public Observable<Void> syncTrackLikes() {
        if (featureFlags.isEnabled(Flag.FEATURE_NEW_SYNC_ADAPTER)) {
            return syncInitiator.sync(Syncable.TRACK_LIKES).map(RxUtils.TO_VOID);
        } else {
            return legacySyncInitiator.syncTrackLikes().map(RxUtils.TO_VOID);
        }
    }
}
