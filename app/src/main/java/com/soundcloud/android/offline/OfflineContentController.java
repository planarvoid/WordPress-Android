package com.soundcloud.android.offline;

import com.soundcloud.android.events.ConnectionType;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.events.PlaylistChangedEvent;
import com.soundcloud.android.events.PolicyUpdateEvent;
import com.soundcloud.android.events.UrnStateChangedEvent;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.rx.eventbus.EventBusV2;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class OfflineContentController {

    private final OfflineServiceInitiator serviceInitiator;
    private final OfflineContentOperations offlineContentOperations;
    private final EventBusV2 eventBus;
    private final Observable<Boolean> syncWifiOnlyToggled;

    private Disposable subscription = RxUtils.invalidDisposable();

    @Inject
    OfflineContentController(EventBusV2 eventBus,
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
                .doOnSubscribe(ignore -> serviceInitiator.start())
                // TODO : when shutting down the feature, some entities
                // states change. It means, we should start the service, let it
                // publish entities updates and then let it stop itself.
                // https://github.com/soundcloud/android-listeners/issues/4742
                .doOnDispose(serviceInitiator::stop)
                .subscribeWith(serviceInitiator.startObserver());
    }

    public void dispose() {
        subscription.dispose();
    }

    private Observable<Object> offlineContentEvents() {
        return Observable
                .merge(offlinePlaylistChanged(),
                       offlineTrackLikedChanged(),
                       networkOrWifiOnlySettingChanged(),
                       policyUpdates()
                ).mergeWith(offlineCollectionChanged());
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
                       .map(SyncJobResult::getFirstUrn)
                       .flatMapSingle(offlineContentOperations::isOfflinePlaylist)
                       .filter(isTrue -> isTrue)
                       .cast(Object.class);
    }

    private Observable<Object> playlistChanged() {
        return eventBus.queue(EventQueue.PLAYLIST_CHANGED)
                       // NOTE : this event is not sent from the Syncer.
                       // This case is handled in MyPlaylistsSyncer.syncOfflinePlaylists.
                       .map(PlaylistChangedEvent.TO_URNS)
                       .flatMapSingle(urns -> Observable.fromIterable(urns).flatMapSingle(offlineContentOperations::isOfflinePlaylist).toList())
                       .filter(list -> list.contains(true))
                       .cast(Object.class);
    }

    private Observable<Boolean> offlineTrackLikedChanged() {
        return eventBus.queue(EventQueue.LIKE_CHANGED)
                       .filter(LikesStatusEvent::containsTrackChange)
                       .flatMapSingle(event -> offlineContentOperations.isOfflineLikedTracksEnabled())
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
                       .flatMap(offlineContentOperations::makePlaylistAvailableOffline)
                       .cast(Object.class);
    }

    private Observable<Object> playlistRemovedFromOfflineCollection() {
        // Note : fired by user interactions and the syncer
        return eventBus.queue(EventQueue.URN_STATE_CHANGED)
                       .filter(UrnStateChangedEvent::containsDeletedPlaylist)
                       .map(UrnStateChangedEvent::urns)
                       .flatMap(offlineContentOperations::makePlaylistUnavailableOffline)
                       .cast(Object.class);
    }


    private Observable<Object> playlistLiked() {
        // Note : fired by user interactions and the syncer
        return eventBus.queue(EventQueue.LIKE_CHANGED)
                       .filter(ignored -> offlineContentOperations.isOfflineCollectionEnabled())
                       .filter(LikesStatusEvent::containsLikedPlaylist)
                       .map(event -> event.likes().keySet())
                       .flatMap(offlineContentOperations::makePlaylistAvailableOffline)
                       .cast(Object.class);
    }

    private Observable<Object> playlistUnliked() {
        // Note : fired by user interactions and the syncer
        return eventBus.queue(EventQueue.LIKE_CHANGED)
                       .filter(LikesStatusEvent::containsUnlikedPlaylist)
                       .map(event -> event.likes().keySet())
                       .flatMap(offlineContentOperations::makePlaylistUnavailableOffline)
                       .cast(Object.class);
    }

}
