package com.soundcloud.android.offline;

import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.sync.SyncActions;
import com.soundcloud.android.sync.SyncResult;
import rx.Observable;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;

import android.content.Context;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class OfflineContentController {

    private final Context context;
    private final OfflineSettingsStorage settingStorage;

    private final Observable<EntityStateChangedEvent> trackLikeChanged;
    private final Observable<EntityStateChangedEvent> offlinePlaylistChanged;
    private final Observable<SyncResult> likesSynced;
    private final Observable<Boolean> offlineLikesEnabled;
    private final Observable<Boolean> offlineLikesDisabled;

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
            return settingStorage.isOfflineLikesEnabled();
        }
    };

    private CompositeSubscription subscription = new CompositeSubscription();

    @Inject
    public OfflineContentController(EventBus eventBus, OfflineSettingsStorage settingsStorage, Context context) {
        this.context = context;
        this.settingStorage = settingsStorage;

        this.offlineLikesEnabled = settingsStorage.getOfflineLikesChanged().filter(RxUtils.IS_TRUE);
        this.offlineLikesDisabled = settingsStorage.getOfflineLikesChanged().filter(RxUtils.IS_FALSE);
        this.trackLikeChanged = eventBus.queue(EventQueue.ENTITY_STATE_CHANGED).filter(EntityStateChangedEvent.IS_TRACK_LIKE_EVENT_FILTER);
        this.offlinePlaylistChanged = eventBus.queue(EventQueue.ENTITY_STATE_CHANGED).filter(EntityStateChangedEvent.IS_PLAYLIST_OFFLINE_CONTENT_EVENT_FILTER);
        this.likesSynced = eventBus.queue(EventQueue.SYNC_RESULT).filter(IS_LIKES_SYNC_FILTER);
    }

    public void subscribe() {
        subscription = new CompositeSubscription(
                startOfflineContent().subscribe(new OfflineContentServiceSubscriber(context, true)),
                offlineLikesDisabled.subscribe(new OfflineContentServiceSubscriber(context, false))
        );
    }

    private Observable<?> startOfflineContent() {
        return Observable
                .merge(offlineLikesEnabled,
                        offlinePlaylistChanged,
                        likesSynced.filter(isOfflineLikesEnabled),
                        trackLikeChanged.filter(isOfflineLikesEnabled)
                );
    }

    public void unsubscribe() {
        subscription.unsubscribe();
    }

}
