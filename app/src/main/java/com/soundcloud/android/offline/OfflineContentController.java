package com.soundcloud.android.offline;

import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
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

    private final Observable<EntityStateChangedEvent> likedTracks;
    private final Observable<SyncResult> syncedLikes;
    private final Observable<Boolean> featureEnabled;
    private final Observable<Boolean> featureDisabled;

    private static final Func1<Boolean, Boolean> IS_DISABLED = new Func1<Boolean, Boolean>() {
        @Override
        public Boolean call(Boolean isEnabled) {
            return !isEnabled;
        }
    };

    private static final Func1<Boolean, Boolean> IS_ENABLED = new Func1<Boolean, Boolean>() {
        @Override
        public Boolean call(Boolean isEnabled) {
            return isEnabled;
        }
    };

    public static final Func1<SyncResult, Boolean> IS_LIKES_SYNC_FILTER = new Func1<SyncResult, Boolean>() {
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

        this.likedTracks = eventBus.queue(EventQueue.ENTITY_STATE_CHANGED).filter(EntityStateChangedEvent.IS_TRACK_LIKE_EVENT_FILTER);
        this.syncedLikes = eventBus.queue(EventQueue.SYNC_RESULT).filter(IS_LIKES_SYNC_FILTER);
        this.featureEnabled = settingsStorage.getOfflineLikesChanged().filter(IS_ENABLED);
        this.featureDisabled = settingsStorage.getOfflineLikesChanged().filter(IS_DISABLED);
    }

    public void subscribe() {
        subscription = new CompositeSubscription(
                startOfflineContent().subscribe(new OfflineContentServiceSubscriber(context, true)),
                featureDisabled.subscribe(new OfflineContentServiceSubscriber(context, false))
        );
    }

    public Observable<?> startOfflineContent() {
        return Observable
                .merge(featureEnabled,
                        syncedLikes.filter(isOfflineLikesEnabled),
                        likedTracks.filter(isOfflineLikesEnabled)
                );
    }

    public void unsubscribe() {
        subscription.unsubscribe();
    }

}
