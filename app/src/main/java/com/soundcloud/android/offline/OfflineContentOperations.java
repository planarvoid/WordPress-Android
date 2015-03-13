package com.soundcloud.android.offline;

import static com.soundcloud.android.utils.CollectionUtils.add;
import static com.soundcloud.android.utils.CollectionUtils.intersect;
import static com.soundcloud.android.utils.CollectionUtils.subtract;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineContentEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.commands.CountOfflineLikesCommand;
import com.soundcloud.android.offline.commands.LoadDownloadedCommand;
import com.soundcloud.android.offline.commands.LoadPendingDownloadsCommand;
import com.soundcloud.android.offline.commands.LoadPendingDownloadsRequestsCommand;
import com.soundcloud.android.offline.commands.LoadPendingRemovalsCommand;
import com.soundcloud.android.offline.commands.LoadTracksWithStalePoliciesCommand;
import com.soundcloud.android.offline.commands.LoadTracksWithValidPoliciesCommand;
import com.soundcloud.android.offline.commands.RemoveOfflinePlaylistCommand;
import com.soundcloud.android.offline.commands.StoreDownloadedCommand;
import com.soundcloud.android.offline.commands.StoreOfflinePlaylistCommand;
import com.soundcloud.android.offline.commands.StorePendingDownloadsCommand;
import com.soundcloud.android.offline.commands.StorePendingRemovalsCommand;
import com.soundcloud.android.policies.PolicyOperations;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.propeller.WriteResult;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func4;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;

public class OfflineContentOperations {

    private final LoadPendingDownloadsRequestsCommand loadPendingDownloads;
    private final LoadPendingRemovalsCommand loadPendingRemovalsCommand;
    private final LoadDownloadedCommand loadDownloadedCommand;
    private final LoadPendingDownloadsCommand loadPendingDownloadsCommand;

    private final StorePendingDownloadsCommand storePendingDownloadsCommand;
    private final StorePendingRemovalsCommand storePendingRemovalsCommand;
    private final StoreDownloadedCommand storeDownloadedCommand;

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

    private final Func1<Void, Observable<Void>> updateOfflineQueue = new Func1<Void, Observable<Void>>() {
        @Override
        public Observable<Void> call(Void aVoid) {
            return updateOfflineQueue();
        }
    };

    private final Func4<Collection<Urn>, List<Urn>, List<Urn>, List<Urn>, Void> offlineContentQueueDifferentialUpdate = new Func4<Collection<Urn>, List<Urn>, List<Urn>, List<Urn>, Void>() {
        @Override
        public Void call(Collection<Urn> expectedContent, List<Urn> pendingDownloads, List<Urn> pendingRemovals, List<Urn> downloadedTracks) {
            storeNewPendingDownloads(expectedContent, pendingDownloads, downloadedTracks);
            storeAndNotifyNewDownloadedTracks(expectedContent, pendingRemovals, downloadedTracks);
            storeAndNotifyNewPendingRemovals(expectedContent, pendingDownloads, downloadedTracks);
            return null;
        }
    };

    @Inject
    public OfflineContentOperations(LoadDownloadedCommand loadDownloadedCommand,
                                    StorePendingDownloadsCommand storePendingDownloadsCommand,
                                    StorePendingRemovalsCommand storePendingRemovalsCommand,
                                    StoreDownloadedCommand storeDownloadedCommand, LoadTracksWithStalePoliciesCommand loadTracksWithStatePolicies,
                                    LoadPendingRemovalsCommand loadPendingRemovalsCommand,
                                    LoadPendingDownloadsRequestsCommand loadPendingCommand,
                                    LoadPendingDownloadsCommand loadPendingDownloadsCommand, OfflineSettingsStorage settingsStorage,
                                    EventBus eventBus, CountOfflineLikesCommand offlineTrackCount,
                                    StoreOfflinePlaylistCommand storeOfflinePlaylist,
                                    RemoveOfflinePlaylistCommand removeOfflinePlaylist,
                                    PolicyOperations policyOperations,
                                    LoadTracksWithValidPoliciesCommand loadTracksWithValidPolicies) {
        this.loadDownloadedCommand = loadDownloadedCommand;
        this.storePendingDownloadsCommand = storePendingDownloadsCommand;
        this.storePendingRemovalsCommand = storePendingRemovalsCommand;
        this.storeDownloadedCommand = storeDownloadedCommand;
        this.loadTracksWithStatePolicies = loadTracksWithStatePolicies;
        this.loadPendingRemovalsCommand = loadPendingRemovalsCommand;
        this.loadPendingDownloadsCommand = loadPendingDownloadsCommand;
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

    public boolean isOfflineLikedTracksEnabled() {
        return settingsStorage.isOfflineLikedTracksEnabled();
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

    Observable<List<DownloadRequest>> loadDownloadRequests() {
        return updateStalePolicies()
                .flatMap(updateOfflineQueue)
                .flatMap(loadPendingDownloads);
    }

    private Observable<Void> updateOfflineQueue() {
        return Observable
                .zip(
                        loadTracksWithValidPolicies.call(isOfflineLikedTracksEnabled()),
                        loadPendingDownloadsCommand.toObservable(),
                        loadPendingRemovalsCommand.toObservable(),
                        loadDownloadedCommand.toObservable(),
                        offlineContentQueueDifferentialUpdate
                );
    }

    private void storeAndNotifyNewPendingRemovals(Collection<Urn> expectedContent, List<Urn> pendingDownloads, List<Urn> downloadedTracks) {
        final Collection<Urn> newPendingRemovals = subtract(add(downloadedTracks, pendingDownloads), expectedContent);
        if (!newPendingRemovals.isEmpty()) {
            if (storePendingRemovalsCommand.with(newPendingRemovals).call().success()) {
                eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.downloadRemoved(newPendingRemovals));
            }
        }
    }

    private void storeAndNotifyNewDownloadedTracks(Collection<Urn> expectedContent, List<Urn> pendingRemovals, List<Urn> downloadedTracks) {
        final Collection<Urn> newDownloadedTracks = intersect(intersect(pendingRemovals, downloadedTracks), expectedContent);
        if (!newDownloadedTracks.isEmpty()) {
            if (storeDownloadedCommand.with(newDownloadedTracks).call().success()) {
                eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.downloadFinished(newDownloadedTracks));
            }
        }
    }

    private void storeNewPendingDownloads(Collection<Urn> expectedContent, List<Urn> pendingDownloads, List<Urn> downloadedTracks) {
        final Collection<Urn> newPendingDownloads = subtract(expectedContent, pendingDownloads, downloadedTracks);
        if (!newPendingDownloads.isEmpty()) {
            storePendingDownloadsCommand.with(newPendingDownloads).call();
        }
    }

    @VisibleForTesting
    Observable<Void> updateStalePolicies() {
        return loadTracksWithStatePolicies.call(isOfflineLikedTracksEnabled())
                .flatMap(UPDATE_POLICIES);
    }
}
