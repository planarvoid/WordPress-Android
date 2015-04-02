package com.soundcloud.android.offline;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.commands.ClearTrackDownloadsCommand;
import com.soundcloud.android.offline.commands.LoadExpectedContentCommand;
import com.soundcloud.android.offline.commands.LoadOfflineContentUpdatesCommand;
import com.soundcloud.android.offline.commands.LoadTracksWithStalePoliciesCommand;
import com.soundcloud.android.offline.commands.StoreDownloadUpdatesCommand;
import com.soundcloud.android.policies.PolicyOperations;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.propeller.WriteResult;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;
import java.util.List;

public class OfflineContentOperations {

    private final StoreDownloadUpdatesCommand storeDownloadUpdatesCommand;

    private final LoadTracksWithStalePoliciesCommand loadTracksWithStalePolicies;
    private final LoadExpectedContentCommand loadExpectedContentCommand;
    private final LoadOfflineContentUpdatesCommand loadOfflineContentUpdatesCommand;
    private final ClearTrackDownloadsCommand clearTrackDownloadsCommand;

    private final OfflineTracksStorage tracksStorage;
    private final SecureFileStorage secureFileStorage;
    private final OfflinePlaylistStorage playlistStorage;
    private final OfflineSettingsStorage settingsStorage;
    private final PolicyOperations policyOperations;
    private final EventBus eventBus;
    private final Scheduler scheduler;

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

    @Inject
    public OfflineContentOperations(StoreDownloadUpdatesCommand storeDownloadUpdatesCommand,
                                    LoadTracksWithStalePoliciesCommand loadTracksWithStalePolicies,
                                    ClearTrackDownloadsCommand clearTrackDownloadsCommand,
                                    OfflineSettingsStorage settingsStorage,
                                    EventBus eventBus,
                                    OfflinePlaylistStorage playlistStorage,
                                    PolicyOperations policyOperations,
                                    LoadExpectedContentCommand loadExpectedContentCommand,
                                    LoadOfflineContentUpdatesCommand loadOfflineContentUpdatesCommand,
                                    OfflineTracksStorage tracksStorage,
                                    SecureFileStorage secureFileStorage,
                                    @Named("HighPriority") Scheduler scheduler) {
        this.storeDownloadUpdatesCommand = storeDownloadUpdatesCommand;
        this.loadTracksWithStalePolicies = loadTracksWithStalePolicies;
        this.clearTrackDownloadsCommand = clearTrackDownloadsCommand;
        this.settingsStorage = settingsStorage;
        this.eventBus = eventBus;
        this.playlistStorage = playlistStorage;
        this.policyOperations = policyOperations;
        this.loadExpectedContentCommand = loadExpectedContentCommand;
        this.loadOfflineContentUpdatesCommand = loadOfflineContentUpdatesCommand;
        this.tracksStorage = tracksStorage;
        this.secureFileStorage = secureFileStorage;
        this.scheduler = scheduler;
    }

    public void setOfflineLikesEnabled(boolean isEnabled) {
        settingsStorage.setOfflineLikedTracksEnabled(isEnabled);
    }

    public Observable<Boolean> makePlaylistAvailableOffline(final Urn playlistUrn) {
        return playlistStorage.storeAsOfflinePlaylist(playlistUrn)
                .map(WRITE_RESULT_TO_SUCCESS)
                .doOnNext(publishMarkedForOfflineChange(playlistUrn, true))
                .subscribeOn(scheduler);
    }

    public Observable<Boolean> makePlaylistUnavailableOffline(final Urn playlistUrn) {
        return playlistStorage.removeFromOfflinePlaylists(playlistUrn)
                .map(WRITE_RESULT_TO_SUCCESS)
                .doOnNext(publishMarkedForOfflineChange(playlistUrn, false))
                .subscribeOn(scheduler);
    }

    public boolean isOfflineLikedTracksEnabled() {
        return settingsStorage.isOfflineLikedTracksEnabled();
    }

    public Observable<Boolean> getOfflineLikesSettingsStatus() {
        return settingsStorage.getOfflineLikedTracksStatusChange();
    }

    public Observable<WriteResult> clearOfflineContent() {
        return clearTrackDownloadsCommand.toObservable(null).doOnNext(new Action1<WriteResult>() {
            @Override
            public void call(WriteResult writeResult) {
                secureFileStorage.deleteAllTracks();
            }
        }).subscribeOn(scheduler);
    }

    private Action1<Boolean> publishMarkedForOfflineChange(final Urn playlistUrn, final boolean isMarkedOffline) {
        return new Action1<Boolean>() {
            @Override
            public void call(Boolean aBoolean) {
                eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromMarkedForOffline(playlistUrn, isMarkedOffline));
            }
        };
    }

    Observable<OfflineContentRequests> loadOfflineContentUpdates() {
        return updateStalePolicies()
                .flatMap(loadExpectedContentCommand.toContinuation())
                .flatMap(loadOfflineContentUpdatesCommand.toContinuation())
                .doOnNext(storeDownloadUpdatesCommand.toAction())
                .subscribeOn(scheduler);
    }

    Observable<List<Urn>> loadContentToDelete() {
        return tracksStorage.getTracksToRemove().subscribeOn(scheduler);
    }

    @VisibleForTesting
    Observable<Void> updateStalePolicies() {
        return loadTracksWithStalePolicies.toObservable()
                .flatMap(UPDATE_POLICIES);
    }
}
