package com.soundcloud.android.offline;

import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineContentEvent;
import com.soundcloud.android.likes.LoadLikedTrackUrnsCommand;
import com.soundcloud.android.offline.commands.LoadPendingDownloadsCommand;
import com.soundcloud.android.offline.commands.OfflineTrackCountCommand;
import com.soundcloud.android.offline.commands.UpdateContentAsPendingRemovalCommand;
import com.soundcloud.android.offline.commands.UpdateOfflineContentCommand;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.sync.SyncActions;
import com.soundcloud.android.sync.SyncResult;
import rx.Observable;
import rx.functions.Func1;

import javax.inject.Inject;
import java.util.List;

public class OfflineContentOperations {

    private final LoadLikedTrackUrnsCommand loadLikedTrackUrns;
    private final LoadPendingDownloadsCommand loadPendingDownloads;
    private final UpdateOfflineContentCommand updateOfflineContent;
    private final UpdateContentAsPendingRemovalCommand updateContentAsPendingRemoval;
    private final OfflineTrackCountCommand offlineTrackCount;

    private final EventBus eventBus;
    private final OfflineSettingsStorage settingsStorage;

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
            return isOfflineLikesEnabled();
        }
    };

    private static final Func1<OfflineContentEvent, Boolean> OFFLINE_SYNC_IN_PROGRESS = new Func1<OfflineContentEvent, Boolean>() {
        @Override
        public Boolean call(OfflineContentEvent offlineContentEvent) {
            return offlineContentEvent.getKind() == OfflineContentEvent.START;
        }
    };

    private static final Func1<OfflineContentEvent, Boolean> OFFLINE_SYNC_FINISHED_OR_IDLE = new Func1<OfflineContentEvent, Boolean>() {
        @Override
        public Boolean call(OfflineContentEvent offlineContentEvent) {
            return offlineContentEvent.getKind() == OfflineContentEvent.STOP ||
                    offlineContentEvent.getKind() == OfflineContentEvent.IDLE;
        }
    };

    @Inject
    public OfflineContentOperations(UpdateOfflineContentCommand updateOfflineContent,
                                    LoadLikedTrackUrnsCommand loadLikedTrackUrns,
                                    LoadPendingDownloadsCommand loadPendingCommand,
                                    UpdateContentAsPendingRemovalCommand updateContentAsPendingRemoval,
                                    OfflineSettingsStorage settingsStorage,
                                    EventBus eventBus, OfflineTrackCountCommand offlineTrackCount) {
        this.updateOfflineContent = updateOfflineContent;
        this.settingsStorage = settingsStorage;
        this.loadLikedTrackUrns = loadLikedTrackUrns;
        this.loadPendingDownloads = loadPendingCommand;
        this.updateContentAsPendingRemoval = updateContentAsPendingRemoval;
        this.offlineTrackCount = offlineTrackCount;
        this.eventBus = eventBus;
        this.likedTracks = eventBus.queue(EventQueue.ENTITY_STATE_CHANGED).filter(EntityStateChangedEvent.IS_TRACK_LIKE_FILTER);
        this.syncedLikes = eventBus.queue(EventQueue.SYNC_RESULT).filter(IS_LIKES_SYNC_FILTER);
        this.featureEnabled = settingsStorage.getOfflineLikesChanged().filter(IS_ENABLED);
        this.featureDisabled = settingsStorage.getOfflineLikesChanged().filter(IS_DISABLED);
    }

    public void setOfflineLikesEnabled(boolean isEnabled) {
        settingsStorage.setOfflineLikesEnabled(isEnabled);
    }

    public boolean isOfflineLikesEnabled() {
        return settingsStorage.isOfflineLikesEnabled();
    }

    public Observable<Boolean> getSettingsStatus() {
        return settingsStorage.getOfflineLikesChanged();
    }

    public Observable<?> stopOfflineContentService() {
        return featureDisabled
                .flatMap(updateContentAsPendingRemoval);
    }

    public Observable<?> startOfflineContent() {
        return Observable
                .merge(featureEnabled,
                        syncedLikes.filter(isOfflineLikesEnabled),
                        likedTracks.filter(isOfflineLikesEnabled)
                );
    }

    Observable<List<DownloadRequest>> updateDownloadRequestsFromLikes() {
        return loadLikedTrackUrns
                .toObservable()
                .flatMap(updateOfflineContent)
                .flatMap(loadPendingDownloads);
    }

    public Observable<OfflineContentEvent> onStarted() {
        return eventBus.queue(EventQueue.OFFLINE_CONTENT)
                .filter(OFFLINE_SYNC_IN_PROGRESS);
    }

    public Observable<Integer> onFinishedOrIdleWithDownloadedCount() {
        return eventBus.queue(EventQueue.OFFLINE_CONTENT)
                .filter(OFFLINE_SYNC_FINISHED_OR_IDLE)
                .flatMap(offlineTrackCount);
    }

}
