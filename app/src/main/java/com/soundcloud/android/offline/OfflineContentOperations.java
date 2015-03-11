package com.soundcloud.android.offline;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineContentEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.commands.CountOfflineLikesCommand;
import com.soundcloud.android.offline.commands.LoadPendingDownloadsCommand;
import com.soundcloud.android.offline.commands.LoadTracksWithStalePoliciesCommand;
import com.soundcloud.android.offline.commands.LoadTracksWithValidPoliciesCommand;
import com.soundcloud.android.offline.commands.RemoveOfflinePlaylistCommand;
import com.soundcloud.android.offline.commands.StoreOfflinePlaylistCommand;
import com.soundcloud.android.offline.commands.UpdateOfflineContentCommand;
import com.soundcloud.android.policies.PolicyOperations;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.propeller.WriteResult;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;

public class OfflineContentOperations {

    private final LoadPendingDownloadsCommand loadPendingDownloads;
    private final UpdateOfflineContentCommand updateOfflineContent;
    private final CountOfflineLikesCommand offlineTrackCount;
    private final StoreOfflinePlaylistCommand storeOfflinePlaylist;
    private final RemoveOfflinePlaylistCommand removeOfflinePlaylist;

    private final LoadTracksWithStalePoliciesCommand loadTracksWithStatePolicies;
    private final LoadTracksWithValidPoliciesCommand loadTracksWithValidPolicies;
    private final OfflineSettingsStorage settingsStorage;
    private final PolicyOperations policyOperations;
    private final EventBus eventBus;

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

    private final Func1<Collection<Urn>, Observable<Void>> UPDATE_POLICIES = new Func1<Collection<Urn>, Observable<Void>>() {
        @Override
        public Observable<Void> call(Collection<Urn> urns) {
            if (urns.isEmpty()) {
                return Observable.just(null);
            }
            return policyOperations.fetchAndStorePolicies(urns);
        }
    };

    private static final Func1<WriteResult, Boolean> WRITE_RESULT_TO_SUCCESS = new Func1<WriteResult, Boolean>() {
        @Override
        public Boolean call(WriteResult writeResult) {
            return writeResult.success();
        }
    };

    private final Func1<Void, Observable<WriteResult>> updateOfflineQueue = new Func1<Void, Observable<WriteResult>>() {
        @Override
        public Observable<WriteResult> call(Void aVoid) {
            return updateOfflineQueue();
        }
    };

    @Inject
    public OfflineContentOperations(LoadTracksWithStalePoliciesCommand loadTracksWithStatePolicies,
                                    UpdateOfflineContentCommand updateOfflineContent,
                                    LoadPendingDownloadsCommand loadPendingCommand,
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
        this.offlineTrackCount = offlineTrackCount;
        this.eventBus = eventBus;
        this.storeOfflinePlaylist = storeOfflinePlaylist;
        this.removeOfflinePlaylist = removeOfflinePlaylist;
        this.policyOperations = policyOperations;
        this.loadTracksWithValidPolicies = loadTracksWithValidPolicies;
    }

    public void setOfflineLikesEnabled(boolean isEnabled) {
        settingsStorage.setOfflineLikesEnabled(isEnabled);
    }

    public Observable<Boolean> makePlaylistAvailableOffline(final Urn playlistUrn) {
        return storeOfflinePlaylist.with(playlistUrn).toObservable()
                .map(WRITE_RESULT_TO_SUCCESS)
                .doOnNext(publishMarkedForOfflineChange(playlistUrn, true));
    }

    public Observable<Boolean> makePlaylistUnavailableOffline(final Urn playlistUrn) {
        return removeOfflinePlaylist.with(playlistUrn).toObservable()
                .map(WRITE_RESULT_TO_SUCCESS)
                .doOnNext(publishMarkedForOfflineChange(playlistUrn, false));
    }

    public boolean isOfflineLikesEnabled() {
        return settingsStorage.isOfflineLikesEnabled();
    }

    public Observable<Boolean> getOfflineLikesSettingsStatus() {
        return settingsStorage.getOfflineLikesChanged();
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

    Observable<WriteResult> updateOfflineQueue() {
        return loadTracksWithValidPolicies.call(isOfflineLikesEnabled())
                .flatMap(updateOfflineContent);
    }

    Observable<List<DownloadRequest>> loadDownloadRequests() {
        return updateStalePolicies()
                .flatMap(updateOfflineQueue)
                .flatMap(loadPendingDownloads);
    }

    @VisibleForTesting
    Observable<Void> updateStalePolicies() {
        return loadTracksWithStatePolicies.call(isOfflineLikesEnabled())
                .flatMap(UPDATE_POLICIES);
    }
}
