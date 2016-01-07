package com.soundcloud.android.offline;

import static com.soundcloud.android.events.EntityStateChangedEvent.IS_PLAYLIST_CONTENT_CHANGED_FILTER;
import static com.soundcloud.android.events.EntityStateChangedEvent.IS_PLAYLIST_OFFLINE_CONTENT_EVENT_FILTER;
import static com.soundcloud.android.events.EntityStateChangedEvent.IS_TRACK_LIKE_EVENT_FILTER;
import static com.soundcloud.android.rx.RxUtils.continueWith;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.collection.CollectionOperations;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PolicyUpdateEvent;
import com.soundcloud.android.events.UrnEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PlaylistOperations;
import com.soundcloud.android.playlists.PlaylistWithTracks;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.sync.SyncActions;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Func1;

import android.content.Context;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Singleton
public class OfflineContentController {

    private final Context context;
    private final CollectionOperations collectionOperations;
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

    private final Func1<List<PlaylistItem>, List<Urn>> TO_URN = new Func1<List<PlaylistItem>, List<Urn>>() {
        @Override
        public List<Urn> call(List<PlaylistItem> playlistItems) {
            return Lists.transform(playlistItems, PlayableItem.TO_URN);
        }
    };

    private Func1<Collection<Urn>, Observable<List<PlaylistWithTracks>>> syncPlaylistsIfNecessary = new Func1<Collection<Urn>, Observable<List<PlaylistWithTracks>>>() {
        @Override
        public Observable<List<PlaylistWithTracks>> call(Collection<Urn> playlists) {
            return playlistOperations.playlists(playlists).subscribeOn(scheduler);
        }
    };

    private final Action0 startService = new Action0() {
        @Override
        public void call() {
            OfflineContentService.start(OfflineContentController.this.context);
        }
    };

    private final Action0 stopService = new Action0() {
        @Override
        public void call() {
            OfflineContentService.stop(OfflineContentController.this.context);
        }
    };

    private final Func1<Void, Boolean> isOfflineCollectionEnabled = new Func1<Void, Boolean>() {
        @Override
        public Boolean call(Void signal) {
            return offlineContentOperations.isOfflineCollectionEnabled();
        }
    };

    private final Func1<List<PlaylistWithTracks>, Observable<?>> setOfflinePlaylists = new Func1<List<PlaylistWithTracks>, Observable<?>>() {
        @Override
        public Observable<?> call(List<PlaylistWithTracks> playlistItems) {
            final List<Urn> urns = Lists.transform(playlistItems, PlaylistWithTracks.TO_URN);
            return offlineContentOperations.setOfflinePlaylists(urns);
        }
    };

    private Subscription subscription = RxUtils.invalidSubscription();

    @Inject
    public OfflineContentController(Context context, EventBus eventBus,
                                    OfflineSettingsStorage settingsStorage,
                                    PlaylistOperations playlistOperations,
                                    OfflineContentOperations offlineContentOperations,
                                    CollectionOperations collectionOperations,
                                    @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.context = context;
        this.eventBus = eventBus;
        this.collectionOperations = collectionOperations;
        this.scheduler = scheduler;
        this.playlistOperations = playlistOperations;
        this.offlineContentOperations = offlineContentOperations;
        this.syncWifiOnlyToggled = settingsStorage.getWifiOnlyOfflineSyncStateChange();
    }

    public void subscribe() {
        subscription = offlineContentEvents()
                .doOnSubscribe(startService)
                .doOnUnsubscribe(stopService)
                .subscribe(new OfflineContentServiceSubscriber(context));
    }

    public void unsubscribe() {
        subscription.unsubscribe();
    }

    private Observable<Void> offlineContentEvents() {
        return Observable
                .merge(getOfflinePlaylistChangedEvents(),
                        getOfflineLikesChangedEvents(),
                        getSyncOverWifiStateChanged(),
                        policyUpdates(),
                        offlineCollectionsEvents()
                )
                .map(RxUtils.TO_VOID);
    }

    private Observable<?> offlineCollectionsEvents() {
        return Observable
                .merge(
                        collectionOperations.onCollectionChanged().filter(isOfflineCollectionEnabled),
                        offlineContentOperations.getOfflineCollectionStateChanges().filter(RxUtils.IS_TRUE)
                )
                .map(RxUtils.TO_VOID)
                .flatMap(continueWith(offlineContentOperations.enableOfflineLikedTracks()))
                .flatMap(continueWith(setPlaylistsCollectionAsOffline()));
    }

    private Observable<?> setPlaylistsCollectionAsOffline() {
        return collectionOperations.myPlaylists().map(TO_URN)
                .flatMap(syncPlaylistsIfNecessary)
                .flatMap(setOfflinePlaylists);
    }

    private Observable<?> getOfflinePlaylistChangedEvents() {
        return Observable.merge(
                offlinePlaylistSynced(),
                offlinePlaylistStatusChanged(),
                offlinePlaylistContentChanged()
        );
    }

    private Observable<?> offlinePlaylistSynced() {
        return eventBus.queue(EventQueue.SYNC_RESULT)
                .filter(IS_PLAYLIST_SYNCED_FILTER)
                .flatMap(isOfflinePlaylist)
                .filter(RxUtils.IS_TRUE);
    }

    private Observable<Boolean> getSyncOverWifiStateChanged() {
        return syncWifiOnlyToggled.filter(RxUtils.IS_FALSE);
    }

    private Observable<?> offlinePlaylistStatusChanged() {
        return eventBus.queue(EventQueue.ENTITY_STATE_CHANGED)
                .filter(IS_PLAYLIST_OFFLINE_CONTENT_EVENT_FILTER)
                .map(new Func1<EntityStateChangedEvent, Set<Urn>>() {
                    @Override
                    public Set<Urn> call(EntityStateChangedEvent event) {
                        return event.getChangeMap().keySet();
                    }
                })
                .flatMap(syncPlaylistsIfNecessary);
    }

    private Observable<PolicyUpdateEvent> policyUpdates() {
        return eventBus.queue(EventQueue.POLICY_UPDATES);
    }

    private Observable<?> offlinePlaylistContentChanged() {
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
