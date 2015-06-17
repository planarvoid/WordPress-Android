package com.soundcloud.android.offline;

import static com.soundcloud.android.events.EntityStateChangedEvent.IS_PLAYLIST_CONTENT_CHANGED_FILTER;
import static com.soundcloud.android.events.EntityStateChangedEvent.IS_PLAYLIST_OFFLINE_CONTENT_EVENT_FILTER;
import static com.soundcloud.android.events.EntityStateChangedEvent.IS_TRACK_LIKE_EVENT_FILTER;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.playlists.PlaylistOperations;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.playlists.PlaylistWithTracks;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.sync.SyncActions;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Func1;

import android.content.Context;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class OfflineContentController {

    private final Context context;
    private final OfflineSettingsStorage settingStorage;
    private final OfflinePlaylistStorage playlistStorage;
    private final Scheduler scheduler;
    private final PlaylistOperations playlistOperations;
    private final EventBus eventBus;

    private final Observable<Boolean> offlineLikedTracksToggle;

    private static final Func1<SyncResult, Boolean> IS_LIKES_SYNC_FILTER = new Func1<SyncResult, Boolean>() {
        @Override
        public Boolean call(SyncResult syncResult) {
            return syncResult.wasChanged()
                    && syncResult.getAction().equals(SyncActions.SYNC_TRACK_LIKES);
        }
    };

    private final Func1<Object, Boolean> isOfflineLikesEnabled = new Func1<Object, Boolean>() {
        @Override
        public Boolean call(Object ignored) {
            return settingStorage.isOfflineLikedTracksEnabled();
        }
    };

    private final Func1<EntityStateChangedEvent, Observable<Boolean>> isOfflinePlaylist = new Func1<EntityStateChangedEvent, Observable<Boolean>>() {
        @Override
        public Observable<Boolean> call(EntityStateChangedEvent event) {
            return playlistStorage.isOfflinePlaylist(event.getNextUrn()).subscribeOn(scheduler);
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
    public OfflineContentController(Context context, EventBus eventBus,
                                    OfflineSettingsStorage settingsStorage,
                                    OfflinePlaylistStorage playlistStorage,
                                    PlaylistOperations playlistOperations,
                                    @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.context = context;
        this.eventBus = eventBus;
        this.settingStorage = settingsStorage;
        this.playlistStorage = playlistStorage;
        this.scheduler = scheduler;
        this.playlistOperations = playlistOperations;
        this.offlineLikedTracksToggle = settingsStorage.getOfflineLikedTracksStatusChange();
    }

    public void subscribe() {
        subscription = startOfflineContent().subscribe(new OfflineContentServiceSubscriber(context));
    }

    public void unsubscribe() {
        OfflineContentService.stop(context);
        subscription.unsubscribe();
    }

    private Observable<Object> startOfflineContent() {
        return Observable
                .merge(getOfflinePlaylistChangedEvents(),
                        getOfflineLikesChangedEvents(),
                        offlineLikedTracksToggle
                );
    }

    private Observable<Object> getOfflinePlaylistChangedEvents() {
        return Observable.merge(
                offlinePlaylistStatusChanged(),
                offlinePlaylistContentChanged()
        );
    }

    private Observable<PlaylistWithTracks> offlinePlaylistStatusChanged() {
        return eventBus.queue(EventQueue.ENTITY_STATE_CHANGED)
                .filter(IS_PLAYLIST_OFFLINE_CONTENT_EVENT_FILTER)
                .map(EntityStateChangedEvent.TO_SINGULAR_CHANGE)
                .flatMap(syncPlaylistIfNecessary);
    }

    private Observable<Boolean> offlinePlaylistContentChanged() {
        return eventBus.queue(EventQueue.ENTITY_STATE_CHANGED)
                .filter(IS_PLAYLIST_CONTENT_CHANGED_FILTER)
                .flatMap(isOfflinePlaylist)
                .filter(RxUtils.IS_TRUE);
    }

    private Observable<Object> getOfflineLikesChangedEvents() {
        return Observable.merge(
                eventBus.queue(EventQueue.ENTITY_STATE_CHANGED).filter(IS_TRACK_LIKE_EVENT_FILTER),
                eventBus.queue(EventQueue.SYNC_RESULT).filter(IS_LIKES_SYNC_FILTER)
        ).filter(isOfflineLikesEnabled);
    }

}
