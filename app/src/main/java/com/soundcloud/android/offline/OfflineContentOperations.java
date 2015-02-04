package com.soundcloud.android.offline;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayableUpdatedEvent;
import com.soundcloud.android.likes.LoadLikedTrackUrnsCommand;
import com.soundcloud.android.offline.commands.LoadPendingDownloadsCommand;
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

    private final OfflineSettingsStorage settingsStorage;

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

    private static final Func1<PlayableUpdatedEvent, Boolean> WAS_TRACK_LIKED = new Func1<PlayableUpdatedEvent, Boolean>() {
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

    private final Func1<Object, Boolean> isOfflineLikesEnabled = new Func1<Object, Boolean>() {
        @Override
        public Boolean call(Object ignored) {
            return isLikesOfflineSyncEnabled();
        }
    };

    private final Observable<PlayableUpdatedEvent> likedTracks;
    private final Observable<SyncResult> syncedLikes;
    private final Observable<Boolean> featureEnabled;
    private final Observable<Boolean> featureDisabled;

    @Inject
    public OfflineContentOperations(UpdateOfflineContentCommand updateOfflineContent,
                                    LoadLikedTrackUrnsCommand loadLikedTrackUrns,
                                    LoadPendingDownloadsCommand loadPendingCommand,
                                    UpdateContentAsPendingRemovalCommand updateContentAsPendingRemoval,
                                    OfflineSettingsStorage settingsStorage,
                                    EventBus eventBus) {
        this.updateOfflineContent = updateOfflineContent;
        this.settingsStorage = settingsStorage;
        this.loadLikedTrackUrns = loadLikedTrackUrns;
        this.loadPendingDownloads = loadPendingCommand;
        this.updateContentAsPendingRemoval = updateContentAsPendingRemoval;
        this.likedTracks = eventBus.queue(EventQueue.PLAYABLE_CHANGED).filter(WAS_TRACK_LIKED);
        this.syncedLikes = eventBus.queue(EventQueue.SYNC_RESULT).filter(IS_LIKES_SYNC_FILTER);
        this.featureEnabled = settingsStorage.getLikesOfflineSyncChanged().filter(IS_ENABLED);
        this.featureDisabled = settingsStorage.getLikesOfflineSyncChanged().filter(IS_DISABLED);
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
                .flatMap(updateContentAsPendingRemoval);
    }

    public Observable<?> startOfflineContentSyncing() {
        return triggerOfflineContentSyncing();
    }

    Observable<List<DownloadRequest>> updateDownloadRequestsFromLikes() {
        return loadLikedTrackUrns
                .toObservable()
                .flatMap(updateOfflineContent)
                .flatMap(loadPendingDownloads);
    }

    private Observable<Object> triggerOfflineContentSyncing() {
        return Observable
                .merge(featureEnabled,
                        syncedLikes.filter(isOfflineLikesEnabled),
                        likedTracks.filter(isOfflineLikesEnabled)
                );
    }
}
