package com.soundcloud.android.offline;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineSyncEvent;
import com.soundcloud.android.events.PlayableUpdatedEvent;
import com.soundcloud.android.likes.LoadLikedTrackUrnsCommand;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.offline.commands.DeleteOfflineContentCommand;
import com.soundcloud.android.offline.commands.LoadPendingRemovalCommand;
import com.soundcloud.android.offline.commands.UpdateContentAsPendingRemovalCommand;
import com.soundcloud.android.offline.commands.UpdateOfflineContentCommand;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.sync.SyncActions;
import com.soundcloud.android.sync.SyncResult;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.TimeUnit;

public class OfflineContentOperations {

    private final static long REMOVAL_DELAY = TimeUnit.MINUTES.toMillis(3);

    private final LoadLikedTrackUrnsCommand loadLikedTrackUrns;
    private final UpdateOfflineContentCommand updateOfflineContent;
    private final UpdateContentAsPendingRemovalCommand updateContentAsPendingRemoval;
    private final LoadPendingRemovalCommand pendingRemoval;
    private final OfflineSettingsStorage settingsStorage;
    private final DeleteOfflineContentCommand deleteOfflineContent;
    private final Scheduler delayScheduler;
    private final EventBus eventBus;
    private final Scheduler storageScheduler;

    private final Action1<Object> publishQueueUpdated = new Action1<Object>() {
        @Override
        public void call(Object ignored) {
            eventBus.publish(EventQueue.OFFLINE_SYNC, OfflineSyncEvent.queueUpdate());
        }
    };

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

    private static final Func1<PlayableUpdatedEvent, Boolean> WAS_TRACK_LIKED = new Func1<PlayableUpdatedEvent, Boolean>() {
        @Override
        public Boolean call(PlayableUpdatedEvent event) {
            return event.getUrn().isTrack() && event.isFromLike() && event.getChangeSet().get(PlayableProperty.IS_LIKED);
        }
    };

    private static final Func1<PlayableUpdatedEvent, Boolean> WAS_TRACK_UNLIKED = new Func1<PlayableUpdatedEvent, Boolean>() {
        @Override
        public Boolean call(PlayableUpdatedEvent event) {
            return event.getUrn().isTrack() && event.isFromLike() && !event.getChangeSet().get(PlayableProperty.IS_LIKED);
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
            return isLikesOfflineSyncEnabled();
        }
    };

    private final Observable<PlayableUpdatedEvent> unlikedTracks;
    private final Observable<PlayableUpdatedEvent> likedTracks;
    private final Observable<SyncResult> syncedLikes;
    private final Observable<Boolean> featureEnabled;
    private final Observable<Boolean> featureDisabled;

    @Inject
    public OfflineContentOperations(UpdateOfflineContentCommand updateOfflineContent,
                                    LoadPendingRemovalCommand pendingRemoval,
                                    DeleteOfflineContentCommand deleteOfflineContent,
                                    UpdateContentAsPendingRemovalCommand updateContentAsPendingRemoval,
                                    LoadLikedTrackUrnsCommand loadLikedTrackUrns,
                                    OfflineSettingsStorage settingsStorage,
                                    EventBus eventBus,
                                    @Named("Storage") Scheduler storageScheduler) {
        this(updateOfflineContent, pendingRemoval, deleteOfflineContent, loadLikedTrackUrns, updateContentAsPendingRemoval, settingsStorage, eventBus, storageScheduler, Schedulers.computation());
    }

    @VisibleForTesting
    OfflineContentOperations(UpdateOfflineContentCommand updateOfflineContent,
                             LoadPendingRemovalCommand pendingRemoval,
                             DeleteOfflineContentCommand deleteOfflineContent,
                             LoadLikedTrackUrnsCommand loadLikedTrackUrns,
                             UpdateContentAsPendingRemovalCommand updateContentAsPendingRemoval,
                             OfflineSettingsStorage settingsStorage,
                             EventBus eventBus,
                             @Named("Storage") Scheduler storageScheduler,
                             Scheduler delayScheduler) {

        this.updateOfflineContent = updateOfflineContent;
        this.pendingRemoval = pendingRemoval;
        this.deleteOfflineContent = deleteOfflineContent;
        this.settingsStorage = settingsStorage;
        this.eventBus = eventBus;
        this.storageScheduler = storageScheduler;
        this.delayScheduler = delayScheduler;
        this.loadLikedTrackUrns = loadLikedTrackUrns;
        this.updateContentAsPendingRemoval = updateContentAsPendingRemoval;
        this.unlikedTracks = eventBus.queue(EventQueue.PLAYABLE_CHANGED).filter(WAS_TRACK_UNLIKED);
        this.likedTracks = eventBus.queue(EventQueue.PLAYABLE_CHANGED).filter(WAS_TRACK_LIKED);
        this.syncedLikes = eventBus.queue(EventQueue.SYNC_RESULT).filter(IS_LIKES_SYNC_FILTER);
        this.featureEnabled = settingsStorage.getLikesOfflineSyncChanged().filter(IS_ENABLED);
        this.featureDisabled = settingsStorage.getLikesOfflineSyncChanged().filter(IS_DISABLED);
    }

    public Observable<Void> processPendingRemovals() {
        return pendingRemoval
                .with(REMOVAL_DELAY)
                .toObservable()
                .subscribeOn(storageScheduler)
                .flatMap(deleteOfflineContent);
    }

    public void setLikesOfflineSync(boolean isEnabled) {
        settingsStorage.setLikesOfflineSync(isEnabled);
    }

    public boolean isLikesOfflineSyncEnabled() {
        return settingsStorage.isLikesOfflineSyncEnabled();
    }

    public Observable<Boolean> getSettingsStatus() {
        return settingsStorage.getLikesOfflineSyncChanged();
    }

    public Observable<?> stopOfflineContentSyncing() {
        return featureDisabled
                .subscribeOn(storageScheduler)
                .flatMap(updateContentAsPendingRemoval)
                .doOnNext(publishQueueUpdated);
    }

    public Observable<?> startOfflineContentSyncing() {
        return triggerOfflineContentSyncing()
                // This is temporary to fix the merge conflict, this is going to be refactored today.
                // Just close your eyes, everything is fine.
                .flatMap(new Func1<Object, Observable<?>>() {
                    @Override
                    public Observable<?> call(Object o) {
                        return updateOfflineLikes();
                    }
                })
                .doOnNext(publishQueueUpdated);
    }

    Observable<?> updateOfflineLikes() {
        return loadLikedTrackUrns
                .toObservable()
                .subscribeOn(storageScheduler)
                .flatMap(updateOfflineContent);
    }

    private Observable<Object> triggerOfflineContentSyncing() {
        return Observable
                .merge(featureEnabled,
                        syncedLikes.filter(isOfflineLikesEnabled),
                        likedTracks.filter(isOfflineLikesEnabled),
                        unlikedTracks
                                .filter(isOfflineLikesEnabled)
                                .delay(REMOVAL_DELAY, TimeUnit.MILLISECONDS, delayScheduler));
    }
}
