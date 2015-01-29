package com.soundcloud.android.offline;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayableUpdatedEvent;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.sync.SyncActions;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.propeller.WriteResult;
import rx.Observable;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;

import android.content.Context;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class OfflineContentController {

    private final EventBus eventBus;
    private final Context context;
    private final OfflineContentOperations operations;

    private CompositeSubscription subscription = new CompositeSubscription();

    private static final Func1<Boolean, Boolean> IS_DISABLED = new Func1<Boolean, Boolean>() {
        @Override public Boolean call(Boolean isEnabled) {
            return !isEnabled;
        }
    };

    private static final Func1<Boolean, Boolean> IS_ENABLED = new Func1<Boolean, Boolean>() {
        @Override public Boolean call(Boolean isEnabled) {
            return isEnabled;
        }
    };

    private static final Func1<PlayableUpdatedEvent, Boolean> IS_TRACK_LIKED_FILTER = new Func1<PlayableUpdatedEvent, Boolean>() {
        @Override
        public Boolean call(PlayableUpdatedEvent event) {
            return event.getUrn().isTrack() && event.isFromLike();
        }
    };

    public static final Func1<SyncResult, Boolean> IS_LIKES_SYNC_FILTER = new Func1<SyncResult, Boolean>() {
        @Override
        public Boolean call(SyncResult syncResult) {
            return syncResult.wasChanged()
                    && syncResult.getAction().equals(SyncActions.SYNC_TRACK_LIKES);
        }
    };

    private final Func1<Object, Observable<WriteResult>> updateOfflineLikes = new Func1<Object, Observable<WriteResult>>() {
        @Override
        public Observable<WriteResult> call(Object ignored) {
            return operations.updateOfflineLikes();
        }
    };

    private final Func1<Object, Boolean> isOfflineLikesEnabled = new Func1<Object, Boolean>() {
        @Override
        public Boolean call(Object ignored) {
            return operations.isLikesOfflineSyncEnabled();
        }
    };

    @Inject
    public OfflineContentController(EventBus eventBus, OfflineContentOperations operations, Context context) {
        this.eventBus = eventBus;
        this.operations = operations;
        this.context = context;
    }

    public void subscribe() {
        subscription = new CompositeSubscription(
                getOfflineSyncEnabled()
                        .subscribe(new StopOfflineContentServiceSubscriber()),

                getOfflineSyncDisabled()
                        .flatMap(updateOfflineLikes)
                        .subscribe(new StartOfflineContentServiceSubscriber()),

                eventBus.queue(EventQueue.SYNC_RESULT)
                        .filter(isOfflineLikesEnabled)
                        .filter(IS_LIKES_SYNC_FILTER)
                        .flatMap(updateOfflineLikes)
                        .subscribe(new StartOfflineContentServiceSubscriber()),

                eventBus.queue(EventQueue.PLAYABLE_CHANGED)
                        .filter(isOfflineLikesEnabled)
                        .filter(IS_TRACK_LIKED_FILTER)
                        .flatMap(updateOfflineLikes)
                        .subscribe(new StartOfflineContentServiceSubscriber())
        );
    }

    private Observable<Boolean> getOfflineSyncDisabled() {
        return operations
                .getSettingsStatus()
                .filter(IS_ENABLED);
    }

    private Observable<Boolean> getOfflineSyncEnabled() {
        return operations
                .getSettingsStatus()
                .filter(IS_DISABLED);
    }

    public void unsubscribe() {
        subscription.unsubscribe();
    }

    private final class StartOfflineContentServiceSubscriber extends DefaultSubscriber<Object> {

        @Override
        public void onNext(Object ignored) {
            OfflineContentService.startSyncing(context);
        }
    }

    private class StopOfflineContentServiceSubscriber extends DefaultSubscriber<Object> {
        @Override public void onNext(Object args) {
            OfflineContentService.stopSyncing(context);
        }
    }
}
