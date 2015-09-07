package com.soundcloud.android.offline;

import static com.soundcloud.android.events.EntityStateChangedEvent.IS_PLAYLIST_CONTENT_CHANGED_FILTER;
import static com.soundcloud.android.events.EntityStateChangedEvent.IS_PLAYLIST_OFFLINE_CONTENT_EVENT_FILTER;
import static com.soundcloud.android.events.EntityStateChangedEvent.IS_TRACK_LIKE_EVENT_FILTER;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PolicyUpdateEvent;
import com.soundcloud.android.events.UrnEvent;
import com.soundcloud.android.playlists.PlaylistOperations;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.playlists.PlaylistWithTracks;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.sync.SyncActions;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Func1;

import android.content.Context;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class OfflineServiceInitiator {

    private final Context context;
    private final Scheduler scheduler;
    private final OfflineContentOperations offlineContentOperations;
    private final PlaylistOperations playlistOperations;
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

    private final Func1<PropertySet, Observable<PlaylistWithTracks>> syncPlaylistIfNecessary = new Func1<PropertySet, Observable<PlaylistWithTracks>>() {
        @Override
        public Observable<PlaylistWithTracks> call(PropertySet playlist) {
            return playlistOperations.playlist(playlist.get(PlaylistProperty.URN)).subscribeOn(scheduler);
        }
    };

    private Subscription subscription = RxUtils.invalidSubscription();

    @Inject
    public OfflineServiceInitiator(Context context, EventBus eventBus,
                                   OfflineSettingsStorage settingsStorage,
                                   PlaylistOperations playlistOperations,
                                   OfflineContentOperations offlineContentOperations,
                                   @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.context = context;
        this.eventBus = eventBus;
        this.scheduler = scheduler;
        this.playlistOperations = playlistOperations;
        this.offlineContentOperations = offlineContentOperations;

        this.syncWifiOnlyToggled = settingsStorage.getWifiOnlyOfflineSyncStateChange();
    }

    public void subscribe() {
        subscription = startOfflineContent()
                .doOnSubscribe(OfflineContentServiceSubscriber.startServiceAction(context))
                .subscribe(new OfflineContentServiceSubscriber(context));
    }

    public void unsubscribe() {
        OfflineContentService.stop(context);
        subscription.unsubscribe();
    }

    private Observable<Object> startOfflineContent() {
        return Observable
                .merge(getOfflinePlaylistChangedEvents(),
                        getOfflineLikesChangedEvents(),
                        getSyncOverWifiStateChanged(),
                        policyUpdates()
                );
    }

    private Observable<Object> getOfflinePlaylistChangedEvents() {
        return Observable.merge(
                offlinePlaylistSynced(),
                offlinePlaylistStatusChanged(),
                offlinePlaylistContentChanged()
        );
    }

    private Observable<Boolean> offlinePlaylistSynced() {
        return eventBus.queue(EventQueue.SYNC_RESULT)
                .filter(IS_PLAYLIST_SYNCED_FILTER)
                .flatMap(isOfflinePlaylist)
                .filter(RxUtils.IS_TRUE);
    }

    private Observable<Boolean> getSyncOverWifiStateChanged() {
        return syncWifiOnlyToggled.filter(RxUtils.IS_FALSE);
    }

    private Observable<PlaylistWithTracks> offlinePlaylistStatusChanged() {
        return eventBus.queue(EventQueue.ENTITY_STATE_CHANGED)
                .filter(IS_PLAYLIST_OFFLINE_CONTENT_EVENT_FILTER)
                .map(EntityStateChangedEvent.TO_SINGULAR_CHANGE)
                .flatMap(syncPlaylistIfNecessary);
    }

    private Observable<PolicyUpdateEvent> policyUpdates() {
        return eventBus.queue(EventQueue.POLICY_UPDATES);
    }

    private Observable<Boolean> offlinePlaylistContentChanged() {
        return eventBus.queue(EventQueue.ENTITY_STATE_CHANGED)
                .filter(IS_PLAYLIST_CONTENT_CHANGED_FILTER)
                .flatMap(isOfflinePlaylist)
                .filter(RxUtils.IS_TRUE);
    }

    private Observable<Boolean> getOfflineLikesChangedEvents() {
        return Observable.merge(
                getOfflineLikedTracksContentChanged(),
                offlineContentOperations.getOfflineLikedTracksStatusChanges());
    }

    private Observable<Boolean> getOfflineLikedTracksContentChanged() {
        return Observable.merge(
                eventBus.queue(EventQueue.ENTITY_STATE_CHANGED)
                        .filter(IS_TRACK_LIKE_EVENT_FILTER),
                eventBus.queue(EventQueue.SYNC_RESULT)
                        .filter(IS_LIKES_SYNC_FILTER))
                .flatMap(areOfflineLikesEnabled)
                .filter(RxUtils.IS_TRUE);
    }

}
