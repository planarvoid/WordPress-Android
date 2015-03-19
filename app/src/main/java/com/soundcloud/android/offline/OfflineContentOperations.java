package com.soundcloud.android.offline;

import static com.soundcloud.android.utils.CollectionUtils.add;
import static com.soundcloud.android.utils.CollectionUtils.intersect;
import static com.soundcloud.android.utils.CollectionUtils.subtract;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.events.CurrentDownloadEvent;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.commands.LoadDownloadedCommand;
import com.soundcloud.android.offline.commands.LoadPendingDownloadsCommand;
import com.soundcloud.android.offline.commands.LoadPendingDownloadsRequestsCommand;
import com.soundcloud.android.offline.commands.LoadPendingRemovalsCommand;
import com.soundcloud.android.offline.commands.LoadTracksWithStalePoliciesCommand;
import com.soundcloud.android.offline.commands.LoadTracksWithValidPoliciesCommand;
import com.soundcloud.android.offline.commands.StoreDownloadedCommand;
import com.soundcloud.android.offline.commands.StorePendingDownloadsCommand;
import com.soundcloud.android.offline.commands.StorePendingRemovalsCommand;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.policies.PolicyOperations;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.propeller.WriteResult;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.functions.Func4;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;

public class OfflineContentOperations {

    private final LoadPendingDownloadsRequestsCommand loadPendingDownloadRequests;
    private final LoadPendingRemovalsCommand loadPendingRemovalsCommand;
    private final LoadDownloadedCommand loadDownloadedCommand;
    private final LoadPendingDownloadsCommand loadPendingDownloadsCommand;

    private final StorePendingDownloadsCommand storePendingDownloadsCommand;
    private final StorePendingRemovalsCommand storePendingRemovalsCommand;
    private final StoreDownloadedCommand storeDownloadedCommand;

    private final LoadTracksWithStalePoliciesCommand loadTracksWithStatePolicies;
    private final LoadTracksWithValidPoliciesCommand loadTracksWithValidPolicies;

    private final OfflineTracksStorage tracksStorage;
    private final OfflinePlaylistStorage playlistStorage;
    private final OfflineSettingsStorage settingsStorage;
    private final PolicyOperations policyOperations;
    private final EventBus eventBus;

    private static final Func2<CurrentDownloadEvent, Boolean, CurrentDownloadEvent> TO_CURRENT_DOWNLOAD_EVENT = new Func2<CurrentDownloadEvent, Boolean, CurrentDownloadEvent>() {
        @Override
        public CurrentDownloadEvent call(CurrentDownloadEvent event, Boolean ignored) {
            return event;
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
                                    StoreDownloadedCommand storeDownloadedCommand,
                                    LoadTracksWithStalePoliciesCommand loadTracksWithStatePolicies,
                                    LoadPendingRemovalsCommand loadPendingRemovalsCommand,
                                    LoadPendingDownloadsRequestsCommand loadPendingCommand,
                                    LoadPendingDownloadsCommand loadPendingDownloadsCommand,
                                    OfflineSettingsStorage settingsStorage,
                                    EventBus eventBus,
                                    OfflinePlaylistStorage playlistStorage,
                                    PolicyOperations policyOperations,
                                    LoadTracksWithValidPoliciesCommand loadTracksWithValidPolicies,
                                    OfflineTracksStorage tracksStorage) {
        this.loadDownloadedCommand = loadDownloadedCommand;
        this.storePendingDownloadsCommand = storePendingDownloadsCommand;
        this.storePendingRemovalsCommand = storePendingRemovalsCommand;
        this.storeDownloadedCommand = storeDownloadedCommand;
        this.loadTracksWithStatePolicies = loadTracksWithStatePolicies;
        this.loadPendingRemovalsCommand = loadPendingRemovalsCommand;
        this.loadPendingDownloadsCommand = loadPendingDownloadsCommand;
        this.settingsStorage = settingsStorage;
        this.loadPendingDownloadRequests = loadPendingCommand;
        this.eventBus = eventBus;
        this.playlistStorage = playlistStorage;
        this.policyOperations = policyOperations;
        this.loadTracksWithValidPolicies = loadTracksWithValidPolicies;
        this.tracksStorage = tracksStorage;
    }

    public void setOfflineLikesEnabled(boolean isEnabled) {
        settingsStorage.setOfflineLikedTracksEnabled(isEnabled);
    }

    public Observable<Boolean> makePlaylistAvailableOffline(final Urn playlistUrn) {
        return playlistStorage.storeAsOfflinePlaylist(playlistUrn)
                .map(WRITE_RESULT_TO_SUCCESS)
                .doOnNext(publishMarkedForOfflineChange(playlistUrn, true));
    }

    public Observable<Boolean> makePlaylistUnavailableOffline(final Urn playlistUrn) {
        return playlistStorage.removeFromOfflinePlaylists(playlistUrn)
                .map(WRITE_RESULT_TO_SUCCESS)
                .doOnNext(publishMarkedForOfflineChange(playlistUrn, false));
    }

    public boolean isOfflineLikedTracksEnabled() {
        return settingsStorage.isOfflineLikedTracksEnabled();
    }

    public Observable<Boolean> getOfflineLikesSettingsStatus() {
        return settingsStorage.getOfflineLikedTracksStatusChange();
    }

    private Action1<Boolean> publishMarkedForOfflineChange(final Urn playlistUrn, final boolean isMarkedOffline) {
        return new Action1<Boolean>() {
            @Override
            public void call(Boolean aBoolean) {
                eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromMarkedForOffline(playlistUrn, isMarkedOffline));
            }
        };
    }

    private Observable<Boolean> getOfflinePlaylistStatus(final Urn playlist) {
        return eventBus.queue(EventQueue.ENTITY_STATE_CHANGED)
                .filter(isOfflinePlaylistStatusChange(playlist))
                .map(toOfflinePlaylistStatus(playlist))
                .startWith(isOfflinePlaylist(playlist));
    }

    private Func1<EntityStateChangedEvent, Boolean> toOfflinePlaylistStatus(final Urn playlist) {
        return new Func1<EntityStateChangedEvent, Boolean>() {
            @Override
            public Boolean call(EntityStateChangedEvent event) {
                return event.getChangeMap().get(playlist).get(PlaylistProperty.IS_MARKED_FOR_OFFLINE);
            }
        };
    }

    private Func1<EntityStateChangedEvent, Boolean> isOfflinePlaylistStatusChange(final Urn playlist) {
        return new Func1<EntityStateChangedEvent, Boolean>() {
            @Override
            public Boolean call(EntityStateChangedEvent event) {
                return event.isSingularChange() && event.getNextUrn().equals(playlist) && event.getKind() == EntityStateChangedEvent.MARKED_FOR_OFFLINE;
            }
        };
    }

    public Observable<DownloadState> getPlaylistDownloadState(final Urn playlist) {
        return Observable.combineLatest(
                eventBus.queue(EventQueue.CURRENT_DOWNLOAD),
                getOfflinePlaylistStatus(playlist),
                TO_CURRENT_DOWNLOAD_EVENT)
                .flatMap(new Func1<CurrentDownloadEvent, Observable<DownloadState>>() {
                    @Override
                    public Observable<DownloadState> call(final CurrentDownloadEvent event) {
                        if (isOfflinePlaylist(playlist)) {
                            return tracksStorage
                                    .pendingPlaylistTracksUrns(playlist)
                                    .map(new Func1<List<Urn>, DownloadState>() {
                                        @Override
                                        public DownloadState call(List<Urn> pendingDownloads) {
                                            return toDownloadState(pendingDownloads, event);
                                        }
                                    });
                        }
                        return Observable.just(DownloadState.NO_OFFLINE);
                    }
                })
                .distinctUntilChanged();
    }

    private boolean isOfflinePlaylist(Urn playlist) {
        return playlistStorage.isOfflinePlaylist(playlist);
    }

    public Observable<DownloadState> getLikedTracksDownloadState() {
        return Observable.combineLatest(
                eventBus.queue(EventQueue.CURRENT_DOWNLOAD),
                settingsStorage.getOfflineLikedTracksStatus(),
                TO_CURRENT_DOWNLOAD_EVENT)
                .flatMap(new Func1<CurrentDownloadEvent, Observable<DownloadState>>() {
                    @Override
                    public Observable<DownloadState> call(final CurrentDownloadEvent event) {
                        if (isOfflineLikedTracksEnabled()) {
                            return tracksStorage
                                    .pendingLikedTracksUrns()
                                    .map(new Func1<List<Urn>, DownloadState>() {
                                        @Override
                                        public DownloadState call(List<Urn> pendingDownloads) {
                                            return toDownloadState(pendingDownloads, event);
                                        }
                                    });
                        }
                        return Observable.just(DownloadState.NO_OFFLINE);
                    }
                })
                .distinctUntilChanged();
    }

    private DownloadState toDownloadState(Collection<Urn> pendingDownloads, CurrentDownloadEvent event) {
        if (pendingDownloads.isEmpty()) {
            return DownloadState.DOWNLOADED;
        } else if (event.wasStarted() && pendingDownloads.contains(event.getTrackUrn())) {
            return DownloadState.DOWNLOADING;
        } else {
            return DownloadState.REQUESTED;
        }
    }

    Observable<List<DownloadRequest>> loadDownloadRequests() {
        return updateStalePolicies()
                .flatMap(updateOfflineQueue)
                .flatMap(loadPendingDownloadRequests);
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
