package com.soundcloud.android.offline;

import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineContentEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.commands.CountOfflineLikesCommand;
import com.soundcloud.android.offline.commands.LoadTracksWithStalePoliciesCommand;
import com.soundcloud.android.offline.commands.LoadPendingDownloadsCommand;
import com.soundcloud.android.offline.commands.LoadTracksWithValidPoliciesCommand;
import com.soundcloud.android.offline.commands.RemoveOfflinePlaylistCommand;
import com.soundcloud.android.offline.commands.StoreOfflinePlaylistCommand;
import com.soundcloud.android.offline.commands.UpdateContentAsPendingRemovalCommand;
import com.soundcloud.android.offline.commands.UpdateOfflineContentCommand;
import com.soundcloud.android.policies.PolicyOperations;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.sync.SyncActions;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.propeller.WriteResult;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

import javax.inject.Inject;
import java.util.List;

public class OfflineContentOperations {

    private final LoadPendingDownloadsCommand loadPendingDownloads;
    private final UpdateOfflineContentCommand updateOfflineContent;
    private final UpdateContentAsPendingRemovalCommand updateContentAsPendingRemoval;
    private final CountOfflineLikesCommand offlineTrackCount;
    private final StoreOfflinePlaylistCommand storeOfflinePlaylist;
    private final RemoveOfflinePlaylistCommand removeOfflinePlaylist;

    private final LoadTracksWithStalePoliciesCommand loadTracksWithStatePolicies;
    private final LoadTracksWithValidPoliciesCommand loadTracksWithValidPolicies;

    private final PolicyOperations policyOperations;
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

    private final Func1<WriteResult, Boolean> writeResultToSuccess = new Func1<WriteResult, Boolean>() {
        @Override
        public Boolean call(WriteResult writeResult) {
            return writeResult.success();
        }
    };

    private final Func1<List<Urn>, Observable<Void>> UPDATE_POLICIES = new Func1<List<Urn>, Observable<Void>>() {
        @Override
        public Observable<Void> call(List<Urn> urns) {
            if (urns.isEmpty()) {
                return Observable.just(null);
            }
            return policyOperations.fetchAndStorePolicies(urns);
        }
    };

    @Inject
    public OfflineContentOperations(LoadTracksWithStalePoliciesCommand loadTracksWithStatePolicies,
                                    UpdateOfflineContentCommand updateOfflineContent,
                                    LoadPendingDownloadsCommand loadPendingCommand,
                                    UpdateContentAsPendingRemovalCommand updateContentAsPendingRemoval,
                                    OfflineSettingsStorage settingsStorage,
                                    EventBus eventBus, CountOfflineLikesCommand offlineTrackCount,
                                    StoreOfflinePlaylistCommand storeOfflinePlaylist,
                                    RemoveOfflinePlaylistCommand removeOfflinePlaylist,
                                    PolicyOperations policyOperations,
                                    LoadTracksWithValidPoliciesCommand loadTracksWithValidPolicies) {
        this.loadTracksWithStatePolicies = loadTracksWithStatePolicies;
        this.updateOfflineContent = updateOfflineContent;
        this.settingsStorage = settingsStorage;
        this.loadPendingDownloads = loadPendingCommand;
        this.updateContentAsPendingRemoval = updateContentAsPendingRemoval;
        this.offlineTrackCount = offlineTrackCount;
        this.eventBus = eventBus;
        this.storeOfflinePlaylist = storeOfflinePlaylist;
        this.removeOfflinePlaylist = removeOfflinePlaylist;
        this.policyOperations = policyOperations;
        this.loadTracksWithValidPolicies = loadTracksWithValidPolicies;
        this.likedTracks = eventBus.queue(EventQueue.ENTITY_STATE_CHANGED).filter(EntityStateChangedEvent.IS_TRACK_LIKE_FILTER);
        this.syncedLikes = eventBus.queue(EventQueue.SYNC_RESULT).filter(IS_LIKES_SYNC_FILTER);
        this.featureEnabled = settingsStorage.getOfflineLikesChanged().filter(IS_ENABLED);
        this.featureDisabled = settingsStorage.getOfflineLikesChanged().filter(IS_DISABLED);
    }

    public void setOfflineLikesEnabled(boolean isEnabled) {
        settingsStorage.setOfflineLikesEnabled(isEnabled);
    }

    public Observable<Boolean> makePlaylistAvailableOffline(final Urn playlistUrn){
        return storeOfflinePlaylist.with(playlistUrn).toObservable()
                .map(writeResultToSuccess)
                .doOnNext(publishMarkedForOfflineChange(playlistUrn, true));
    }

    public Observable<Boolean> makePlaylistUnavailableOffline(final Urn playlistUrn){
        return removeOfflinePlaylist.with(playlistUrn).toObservable()
                .map(writeResultToSuccess)
                .doOnNext(publishMarkedForOfflineChange(playlistUrn, false));
    }

    public boolean isOfflineLikesEnabled() {
        return settingsStorage.isOfflineLikesEnabled();
    }

    public Observable<Boolean> getOfflineLikesSettingsStatus() {
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

    Observable<List<DownloadRequest>> updateDownloadRequests() {
        return loadDownloadableUrns()
                .flatMap(updateOfflineContent)
                .flatMap(loadPendingDownloads);
    }

    private Observable<List<Urn>> loadDownloadableUrns() {
        return loadTracksWithStatePolicies.toObservable()
                .flatMap(UPDATE_POLICIES)
                .flatMap(loadTracksWithValidPolicies);
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

    private Action1<Boolean> publishMarkedForOfflineChange(final Urn playlistUrn, final boolean isMarkedOffline) {
        return new Action1<Boolean>() {
            @Override
            public void call(Boolean aBoolean) {
                eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromMarkedForOffline(playlistUrn, isMarkedOffline));
            }
        };
    }
}
