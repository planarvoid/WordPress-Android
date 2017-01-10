package com.soundcloud.android.offline;

import com.soundcloud.android.events.ConnectionType;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.events.PlaylistChangedEvent;
import com.soundcloud.android.events.PolicyUpdateEvent;
import com.soundcloud.android.events.UrnStateChangedEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Subscription;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Set;

@Singleton
public class OfflineContentController {

    private final OfflineServiceInitiator serviceInitiator;
    private final OfflineContentOperations offlineContentOperations;
    private final EventBus eventBus;
    private final Observable<Boolean> syncWifiOnlyToggled;

    private final Func1<Set<Urn>, Observable<Void>> addOfflinePlaylist = new Func1<Set<Urn>, Observable<Void>>() {
        @Override
        public Observable<Void> call(final Set<Urn> newPlaylists) {
            return offlineContentOperations.makePlaylistAvailableOffline(Lists.newArrayList(newPlaylists));
        }
    };

    private final Func1<Set<Urn>, Observable<Void>> removeOfflinePlaylist = new Func1<Set<Urn>, Observable<Void>>() {
        @Override
        public Observable<Void> call(final Set<Urn> playlists) {
            return offlineContentOperations.makePlaylistUnavailableOffline(Lists.newArrayList(playlists));
        }
    };

    private Subscription subscription = RxUtils.invalidSubscription();

    @Inject
    OfflineContentController(EventBus eventBus,
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
                playlistChanged()
        );
    }

    private Observable<Object> playlistSynced() {
        return eventBus.queue(EventQueue.SYNC_RESULT)
                       .filter(SyncJobResult.IS_SINGLE_PLAYLIST_SYNCED_FILTER)
                       .map(SyncJobResult.TO_URN)
                       .flatMap(offlineContentOperations::isOfflinePlaylist)
                       .filter(RxUtils.IS_TRUE)
                       .cast(Object.class);
    }

    private Observable<Object> playlistChanged() {
        return eventBus.queue(EventQueue.PLAYLIST_CHANGED)
                       // NOTE : this event is not sent from the Syncer.
                       // This case is handled in MyPlaylistsSyncer.syncOfflinePlaylists.
                       .map(PlaylistChangedEvent.TO_URNS)
                       .flatMap(urns -> Observable.from(urns).flatMap(offlineContentOperations::isOfflinePlaylist).toList())
                       .filter(list -> list.contains(true))
                       .cast(Object.class);
    }

    private Observable<Boolean> offlineTrackLikedChanged() {
        return eventBus.queue(EventQueue.LIKE_CHANGED)
                       .filter(LikesStatusEvent::containsTrackChange)
                       .flatMap(event -> offlineContentOperations.isOfflineLikedTracksEnabled())
                       .filter(liked -> liked);
    }

    private Observable<Object> networkOrWifiOnlySettingChanged() {
        return Observable.merge(
                eventBus.queue(EventQueue.NETWORK_CONNECTION_CHANGED)
                        .filter(connectionType -> connectionType != ConnectionType.UNKNOWN)
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
                playlistRemovedFromOfflineCollection(),
                playlistLiked(),
                playlistUnliked()
        );
    }

    private Observable<Object> playlistAddedToOfflineCollection() {
        // Note : fired by user interactions and the syncer
        return eventBus.queue(EventQueue.URN_STATE_CHANGED)
                       .filter(ignored -> offlineContentOperations.isOfflineCollectionEnabled())
                       .filter(UrnStateChangedEvent::containsCreatedPlaylist)
                       .map(UrnStateChangedEvent::urns)
                       .flatMap(addOfflinePlaylist)
                       .cast(Object.class);
    }

    private Observable<Object> playlistRemovedFromOfflineCollection() {
        // Note : fired by user interactions and the syncer
        return eventBus.queue(EventQueue.URN_STATE_CHANGED)
                       .filter(UrnStateChangedEvent::containsDeletedPlaylist)
                       .map(UrnStateChangedEvent::urns)
                       .flatMap(removeOfflinePlaylist)
                       .cast(Object.class);
    }


    private Observable<Object> playlistLiked() {
        // Note : fired by user interactions and the syncer
        return eventBus.queue(EventQueue.LIKE_CHANGED)
                       .filter(ignored -> offlineContentOperations.isOfflineCollectionEnabled())
                       .filter(LikesStatusEvent::containsLikedPlaylist)
                       .map(event -> event.likes().keySet())
                       .flatMap(addOfflinePlaylist)
                       .cast(Object.class);
    }

    private Observable<Object> playlistUnliked() {
        // Note : fired by user interactions and the syncer
        return eventBus.queue(EventQueue.LIKE_CHANGED)
                       .filter(LikesStatusEvent::containsUnlikedPlaylist)
                       .map(event -> event.likes().keySet())
                       .flatMap(removeOfflinePlaylist)
                       .cast(Object.class);
    }

}
