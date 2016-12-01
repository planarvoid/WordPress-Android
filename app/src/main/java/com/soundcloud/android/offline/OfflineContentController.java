package com.soundcloud.android.offline;

import static com.soundcloud.android.events.EntityStateChangedEvent.IS_PLAYLIST_CONTENT_CHANGED_FILTER;
import static com.soundcloud.android.events.EntityStateChangedEvent.IS_TRACK_LIKE_EVENT_FILTER;

import com.soundcloud.android.events.ConnectionType;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PolicyUpdateEvent;
import com.soundcloud.android.events.UrnEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Subscription;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

@Singleton
public class OfflineContentController {

    private final OfflineServiceInitiator serviceInitiator;
    private final OfflineContentOperations offlineContentOperations;
    private final EventBus eventBus;
    private final Observable<Boolean> syncWifiOnlyToggled;

    private static final Func1<EntityStateChangedEvent, Boolean> IS_PLAYLIST_LIKED_OR_CREATED = new Func1<EntityStateChangedEvent, Boolean>() {
        @Override
        public Boolean call(EntityStateChangedEvent event) {
            return event.containsLikedPlaylist() || event.containsCreatedPlaylist();
        }
    };

    private static final Func1<EntityStateChangedEvent, Boolean> IS_PLAYLIST_UNLIKED_OR_DELETED = new Func1<EntityStateChangedEvent, Boolean>() {
        @Override
        public Boolean call(EntityStateChangedEvent event) {
            return event.containsUnlikedPlaylist() || event.containsDeletedPlaylist();
        }
    };

    private static final Func1<ConnectionType, Boolean> IS_KNOWN_CONNECTION_TYPE = new Func1<ConnectionType, Boolean>() {
        @Override
        public Boolean call(ConnectionType connectionType) {
            return connectionType != ConnectionType.UNKNOWN;
        }
    };

    private final Func1<UrnEvent, Observable<Boolean>> isOfflineLikedTracksEnabled = new Func1<UrnEvent, Observable<Boolean>>() {
        @Override
        public Observable<Boolean> call(UrnEvent urnEvent) {
            return offlineContentOperations.isOfflineLikedTracksEnabled();
        }
    };

    private final Func1<Urn, Observable<Boolean>> isOfflinePlaylist = new Func1<Urn, Observable<Boolean>>() {
        @Override
        public Observable<Boolean> call(Urn playlist) {
            return offlineContentOperations.isOfflinePlaylist(playlist);
        }
    };

    private final Func1<List<Urn>, Observable<Void>> addOfflinePlaylist = new Func1<List<Urn>, Observable<Void>>() {
        @Override
        public Observable<Void> call(final List<Urn> newPlaylists) {
            return offlineContentOperations.makePlaylistAvailableOffline(newPlaylists);
        }
    };

    private final Func1<List<Urn>, Observable<Void>> removeOfflinePlaylist = new Func1<List<Urn>, Observable<Void>>() {
        @Override
        public Observable<Void> call(final List<Urn> playlists) {
            return offlineContentOperations.makePlaylistUnavailableOffline(playlists);
        }
    };

    private final Func1<Object, Boolean> isOfflineCollectionEnabled = new Func1<Object, Boolean>() {
        @Override
        public Boolean call(Object ignored) {
            return offlineContentOperations.isOfflineCollectionEnabled();
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
                // https://github.com/soundcloud/android/issues/4742
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
                       networkOrWifiOnlySettingChanged(),
                       policyUpdates(),
                       offlineCollectionChanged()
                )
                .map(RxUtils.TO_VOID);
    }

    private Observable<Object> offlinePlaylistChanged() {
        return Observable.merge(
                playlistSynced(),
                offlinePlaylistContentChanged()
        );
    }

    private Observable<Object> playlistSynced() {
        return eventBus.queue(EventQueue.SYNC_RESULT)
                       .filter(SyncJobResult.IS_SINGLE_PLAYLIST_SYNCED_FILTER)
                       .map(SyncJobResult.TO_URN)
                       .flatMap(isOfflinePlaylist)
                       .filter(RxUtils.IS_TRUE)
                       .cast(Object.class);
    }

    private Observable<Object> offlinePlaylistContentChanged() {
        return eventBus.queue(EventQueue.ENTITY_STATE_CHANGED)
                       // NOTE : this event is not sent from the Syncer.
                       // This case is handled in MyPlaylistsSyncer.syncOfflinePlaylists.
                       .filter(IS_PLAYLIST_CONTENT_CHANGED_FILTER)
                       .map(EntityStateChangedEvent.TO_URN)
                       .flatMap(isOfflinePlaylist)
                       .filter(RxUtils.IS_TRUE)
                       .cast(Object.class);
    }

    private Observable<Boolean> offlineTrackLikedChanged() {
        return eventBus.queue(EventQueue.ENTITY_STATE_CHANGED)
                       .filter(IS_TRACK_LIKE_EVENT_FILTER)
                       .flatMap(isOfflineLikedTracksEnabled)
                       .filter(RxUtils.IS_TRUE);
    }

    private Observable<Object> networkOrWifiOnlySettingChanged() {
        return Observable.merge(
                eventBus.queue(EventQueue.NETWORK_CONNECTION_CHANGED)
                        .filter(IS_KNOWN_CONNECTION_TYPE)
                        .cast(Object.class),
                syncWifiOnlyToggled
        );
    }

    private Observable<PolicyUpdateEvent> policyUpdates() {
        return eventBus.queue(EventQueue.POLICY_UPDATES);
    }

    private Observable<Object> offlineCollectionChanged() {
        return Observable.merge(
                playlistAddedToOfflineCollection(),
                playlistRemovedFromOfflineCollection()
        );
    }

    private Observable<Object> playlistAddedToOfflineCollection() {
        // Note : fired by user interactions and the syncer
        return eventBus.queue(EventQueue.ENTITY_STATE_CHANGED)
                       .filter(isOfflineCollectionEnabled)
                       .filter(IS_PLAYLIST_LIKED_OR_CREATED)
                       .map(EntityStateChangedEvent.TO_URNS)
                       .flatMap(addOfflinePlaylist)
                       .cast(Object.class);
    }

    private Observable<Object> playlistRemovedFromOfflineCollection() {
        // Note : fired by user interactions and the syncer
        return eventBus.queue(EventQueue.ENTITY_STATE_CHANGED)
                       .filter(IS_PLAYLIST_UNLIKED_OR_DELETED)
                       .map(EntityStateChangedEvent.TO_URNS)
                       .flatMap(removeOfflinePlaylist)
                       .cast(Object.class);
    }

}
