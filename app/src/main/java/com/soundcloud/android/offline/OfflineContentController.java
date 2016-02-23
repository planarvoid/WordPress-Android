package com.soundcloud.android.offline;

import static com.soundcloud.android.events.EntityStateChangedEvent.IS_PLAYLIST_CONTENT_CHANGED_FILTER;
import static com.soundcloud.android.events.EntityStateChangedEvent.IS_TRACK_LIKE_EVENT_FILTER;

import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PolicyUpdateEvent;
import com.soundcloud.android.events.UrnEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.sync.SyncActions;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.propeller.TxnResult;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Subscription;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class OfflineContentController {

    private final OfflineServiceInitiator serviceInitiator;
    private final OfflineContentOperations offlineContentOperations;
    private final EventBus eventBus;

    private final Observable<Boolean> syncWifiOnlyToggled;

    private static final Func1<SyncResult, Boolean> IS_LIKES_SYNC_FILTER = new Func1<SyncResult, Boolean>() {
        @Override
        public Boolean call(SyncResult syncResult) {
            return syncResult.wasChanged()
                    && SyncActions.SYNC_TRACK_LIKES.equals(syncResult.getAction());
        }
    };

    private static final Func1<SyncResult, Boolean> IS_PLAYLIST_SYNCED_FILTER = new Func1<SyncResult, Boolean>() {
        @Override
        public Boolean call(SyncResult syncResult) {
            return SyncActions.SYNC_PLAYLIST.equals(syncResult.getAction())
                    && syncResult.wasChanged()
                    && syncResult.hasChangedEntities();
        }
    };

    private static final Func1<EntityStateChangedEvent, Boolean> IS_PLAYLIST_LIKED_OR_CREATED = new Func1<EntityStateChangedEvent, Boolean>() {
        @Override
        public Boolean call(EntityStateChangedEvent event) {
            return event.isPlaylistLike() || event.isPlaylistCreated();
        }
    };

    private final Func1<UrnEvent, Observable<Boolean>> isOfflinePlaylist = new Func1<UrnEvent, Observable<Boolean>>() {
        @Override
        public Observable<Boolean> call(UrnEvent event) {
            return offlineContentOperations.isOfflinePlaylist(event.getFirstUrn());
        }
    };

    private final Func1<UrnEvent, Observable<Boolean>> areOfflineLikesEnabled = new Func1<UrnEvent, Observable<Boolean>>() {
        @Override
        public Observable<Boolean> call(UrnEvent urnEvent) {
            return offlineContentOperations.isOfflineLikedTracksEnabled();
        }
    };

    private final Func1<TxnResult, Boolean> TO_SUCCESS = new Func1<TxnResult, Boolean>() {
        @Override
        public Boolean call(TxnResult txnResult) {
            return txnResult.success();
        }
    };

    private final Func1<Urn, Observable<Boolean>> toOfflinePlaylist = new Func1<Urn, Observable<Boolean>>() {
        @Override
        public Observable<Boolean> call(Urn newPlaylist) {
            if (offlineContentOperations.isOfflineCollectionEnabled()) {
                return offlineContentOperations
                        .setMyPlaylistsAsOfflinePlaylists()
                        .map(TO_SUCCESS);
            } else {
                return offlineContentOperations.isOfflinePlaylist(newPlaylist);
            }
        }
    };

    private Subscription subscription = RxUtils.invalidSubscription();

    @Inject
    public OfflineContentController(EventBus eventBus,
                                    OfflineSettingsStorage settingsStorage,
                                    OfflineServiceInitiator serviceInitiator,
                                    OfflineContentOperations offlineContentOperations) {
        this.serviceInitiator = serviceInitiator;
        this.eventBus = eventBus;
        this.offlineContentOperations = offlineContentOperations;
        this.syncWifiOnlyToggled = settingsStorage.getWifiOnlyOfflineSyncStateChange();
    }

    public void subscribe() {
        subscription = offlineContentEvents()
                .doOnSubscribe(serviceInitiator.start())
                        // TODO : when shutting down the feature, some entities
                        // states change. It means, we should start the service, let it
                        // publish entities updates and then let it stop itself.
                        // https://github.com/soundcloud/SoundCloud-Android/issues/4742
                .doOnUnsubscribe(serviceInitiator.stop())
                .subscribe(serviceInitiator.startSubscriber());
    }

    public void unsubscribe() {
        subscription.unsubscribe();
    }

    private Observable<Void> offlineContentEvents() {
        return Observable
                .merge(offlinePlaylistChanged(),
                        offlineTrackLikedChanged(),
                        syncOverWifiOnlySettingChanged(),
                        policyUpdates()
                )
                .map(RxUtils.TO_VOID);
    }

    private Observable<Object> offlinePlaylistChanged() {
        return Observable.merge(
                offlinePlaylistSynced(),
                offlinePlaylistContentChanged(),
                playlistAddedToOfflineCollection()
        );
    }

    private Observable<Object> offlinePlaylistSynced() {
        return eventBus.queue(EventQueue.SYNC_RESULT)
                .filter(IS_PLAYLIST_SYNCED_FILTER)
                .map(SyncResult.TO_URN)
                .flatMap(toOfflinePlaylist)
                .filter(RxUtils.IS_TRUE)
                .cast(Object.class);
    }

    private Observable<Boolean> syncOverWifiOnlySettingChanged() {
        return syncWifiOnlyToggled.filter(RxUtils.IS_FALSE);
    }

    private Observable<PolicyUpdateEvent> policyUpdates() {
        return eventBus.queue(EventQueue.POLICY_UPDATES);
    }

    private Observable<Object> offlinePlaylistContentChanged() {
        return eventBus.queue(EventQueue.ENTITY_STATE_CHANGED)
                .filter(IS_PLAYLIST_CONTENT_CHANGED_FILTER)
                .flatMap(isOfflinePlaylist)
                .filter(RxUtils.IS_TRUE)
                .cast(Object.class);
    }

    private Observable<Object> playlistAddedToOfflineCollection() {
        return eventBus.queue(EventQueue.ENTITY_STATE_CHANGED)
                .filter(IS_PLAYLIST_LIKED_OR_CREATED)
                .map(EntityStateChangedEvent.TO_URN)
                .flatMap(toOfflinePlaylist)
                .filter(RxUtils.IS_TRUE)
                .cast(Object.class);
    }

    private Observable<Boolean> offlineTrackLikedChanged() {
        return Observable.merge(
                eventBus.queue(EventQueue.ENTITY_STATE_CHANGED).filter(IS_TRACK_LIKE_EVENT_FILTER),
                eventBus.queue(EventQueue.SYNC_RESULT).filter(IS_LIKES_SYNC_FILTER))
                .flatMap(areOfflineLikesEnabled)
                .filter(RxUtils.IS_TRUE);
    }

}
