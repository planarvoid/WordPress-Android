package com.soundcloud.android.offline;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.CurrentDownloadEvent;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.policies.PolicyOperations;
import com.soundcloud.propeller.WriteResult;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;

import android.support.annotation.VisibleForTesting;

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

    private final TrackDownloadsStorage tracksStorage;
    private final OfflineContentStorage offlineContentStorage;
    private final FeatureOperations featureOperations;
    private final PolicyOperations policyOperations;
    private final EventBus eventBus;
    private final Scheduler scheduler;

    private static final Func1<WriteResult, Boolean> WRITE_RESULT_TO_SUCCESS = new Func1<WriteResult, Boolean>() {
        @Override
        public Boolean call(WriteResult writeResult) {
            return writeResult.success();
        }
    };

    public static final Func1<EntityStateChangedEvent, OfflineState> OFFLINE_LIKES_EVENT_TO_OFFLINE_STATE = new Func1<EntityStateChangedEvent, OfflineState>() {
        @Override
        public OfflineState call(EntityStateChangedEvent event) {
            if (event.getNextChangeSet().getOrElse(OfflineProperty.Collection.OFFLINE_LIKES, false)) {
                return OfflineState.REQUESTED;
            } else {
                return OfflineState.NO_OFFLINE;
            }
        }
    };

    private static final Func1<EntityStateChangedEvent, Boolean> OFFLINE_LIKES_EVENT_TO_IS_MARKED_OFFLINE = new Func1<EntityStateChangedEvent, Boolean>() {
        @Override
        public Boolean call(EntityStateChangedEvent event) {
            return event.getNextChangeSet().getOrElse(OfflineProperty.Collection.OFFLINE_LIKES, false);
        }
    };

    private final Func1<Collection<Urn>, Observable<Void>> UPDATE_POLICIES = new Func1<Collection<Urn>, Observable<Void>>() {
        @Override
        public Observable<Void> call(Collection<Urn> urns) {
            if (urns.isEmpty()) {
                return Observable.just(null);
            }
            return policyOperations.updatePolicies(urns);
        }
    };

    private final Func1<Boolean, Observable<OfflineState>> PENDING_LIKES_TO_OFFLINE_STATE = new Func1<Boolean, Observable<OfflineState>>() {
        @Override
        public Observable<OfflineState> call(Boolean enabled) {
            if (enabled) {
                return tracksStorage.getLikesOfflineState();
            } else {
                return Observable.just(OfflineState.NO_OFFLINE);
            }
        }
    };

    private final Action1<List<Urn>> publishOfflineContentRemoved = new Action1<List<Urn>>() {
        @Override
        public void call(List<Urn> urns) {
            eventBus.publish(EventQueue.CURRENT_DOWNLOAD, CurrentDownloadEvent.offlineContentRemoved(urns));
        }
    };

    @Inject
    OfflineContentOperations(StoreDownloadUpdatesCommand storeDownloadUpdatesCommand,
                             LoadTracksWithStalePoliciesCommand loadTracksWithStalePolicies,
                             ClearTrackDownloadsCommand clearTrackDownloadsCommand,
                             EventBus eventBus,
                             OfflineContentStorage offlineContentStorage,
                             PolicyOperations policyOperations,
                             LoadExpectedContentCommand loadExpectedContentCommand,
                             LoadOfflineContentUpdatesCommand loadOfflineContentUpdatesCommand,
                             FeatureOperations featureOperations, TrackDownloadsStorage tracksStorage,
                             @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.storeDownloadUpdatesCommand = storeDownloadUpdatesCommand;
        this.loadTracksWithStalePolicies = loadTracksWithStalePolicies;
        this.clearTrackDownloadsCommand = clearTrackDownloadsCommand;
        this.eventBus = eventBus;
        this.offlineContentStorage = offlineContentStorage;
        this.policyOperations = policyOperations;
        this.loadExpectedContentCommand = loadExpectedContentCommand;
        this.loadOfflineContentUpdatesCommand = loadOfflineContentUpdatesCommand;
        this.featureOperations = featureOperations;
        this.tracksStorage = tracksStorage;
        this.scheduler = scheduler;
    }

    public Observable<Boolean> disableOfflineLikedTracks() {
        return offlineContentStorage.storeOfflineLikesDisabled()
                .map(WRITE_RESULT_TO_SUCCESS)
                .doOnNext(publishLikesMarkedForOfflineChange(false))
                .subscribeOn(scheduler);
    }

    public Observable<Boolean> enableOfflineLikedTracks() {
        return offlineContentStorage.storeOfflineLikesEnabled()
                .map(WRITE_RESULT_TO_SUCCESS)
                .doOnNext(publishLikesMarkedForOfflineChange(true))
                .subscribeOn(scheduler);
    }

    public Observable<Boolean> isOfflineLikedTracksEnabled() {
        return offlineContentStorage.isOfflineLikesEnabled().subscribeOn(scheduler);
    }

    public Observable<Boolean> getOfflineLikedTracksStatusChanges() {
        return isOfflineLikedTracksEnabled()
                .concatWith(eventBus.queue(EventQueue.ENTITY_STATE_CHANGED)
                        .filter(EntityStateChangedEvent.IS_OFFLINE_LIKES_EVENT_FILTER)
                        .map(OFFLINE_LIKES_EVENT_TO_IS_MARKED_OFFLINE));
    }

    public Observable<Boolean> isOfflinePlaylist(Urn playlist) {
        return offlineContentStorage.isOfflinePlaylist(playlist).subscribeOn(scheduler);
    }

    public Observable<Boolean> makePlaylistAvailableOffline(final Urn playlistUrn) {
        return offlineContentStorage.storeAsOfflinePlaylist(playlistUrn)
                .map(WRITE_RESULT_TO_SUCCESS)
                .doOnNext(publishMarkedForOfflineChange(playlistUrn, true))
                .subscribeOn(scheduler);
    }

    public Observable<Boolean> makePlaylistUnavailableOffline(final Urn playlistUrn) {
        return offlineContentStorage.removeFromOfflinePlaylists(playlistUrn)
                .map(WRITE_RESULT_TO_SUCCESS)
                .doOnNext(publishMarkedForOfflineChange(playlistUrn, false))
                .subscribeOn(scheduler);
    }

    public Observable<Boolean> getOfflineContentOrOfflineLikesStatusChanges() {
        return featureOperations.offlineContentEnabled().concatWith(getOfflineLikedTracksStatusChanges());
    }

    public Observable<List<Urn>> clearOfflineContent() {
        return clearTrackDownloadsCommand.toObservable(null)
                .doOnNext(publishOfflineContentRemoved)
                .subscribeOn(scheduler);
    }

    private Action1<Boolean> publishMarkedForOfflineChange(final Urn playlistUrn, final boolean isMarkedOffline) {
        return eventBus.publishAction1(EventQueue.ENTITY_STATE_CHANGED,
                EntityStateChangedEvent.fromMarkedForOffline(playlistUrn, isMarkedOffline));
    }

    private Action1<Boolean> publishLikesMarkedForOfflineChange(final boolean isMarkedOffline) {
        return eventBus.publishAction1(EventQueue.ENTITY_STATE_CHANGED,
                EntityStateChangedEvent.fromLikesMarkedForOffline(isMarkedOffline));
    }

    Observable<OfflineContentUpdates> loadOfflineContentUpdates() {
        return updateOfflineContentStalePolicies()
                .onErrorResumeNext(Observable.<Void>just(null))
                .flatMap(loadExpectedContentCommand.toContinuation())
                .flatMap(loadOfflineContentUpdatesCommand.toContinuation())
                .doOnNext(storeDownloadUpdatesCommand.toAction())
                .subscribeOn(scheduler);
    }

    Observable<List<Urn>> loadContentToDelete() {
        return tracksStorage.getTracksToRemove().subscribeOn(scheduler);
    }

    @VisibleForTesting
    Observable<Void> updateOfflineContentStalePolicies() {
        return loadTracksWithStalePolicies.toObservable(null)
                .flatMap(UPDATE_POLICIES)
                .subscribeOn(scheduler);
    }

    public Observable<OfflineState> getLikedTracksOfflineStateFromStorage() {
        return offlineContentStorage.isOfflineLikesEnabled()
                .flatMap(PENDING_LIKES_TO_OFFLINE_STATE)
                .subscribeOn(scheduler);
    }
}
