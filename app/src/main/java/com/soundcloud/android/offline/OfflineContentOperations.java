package com.soundcloud.android.offline;

import static com.soundcloud.android.rx.RxUtils.continueWith;
import static com.soundcloud.android.rx.RxUtils.returning;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.collection.CollectionOperations;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PlaylistOperations;
import com.soundcloud.android.playlists.PlaylistWithTracks;
import com.soundcloud.android.policies.PolicyOperations;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;

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

    private final OfflineServiceInitiator serviceInitiator;
    private final TrackDownloadsStorage tracksStorage;
    private final OfflineContentStorage offlineContentStorage;
    private final PlaylistOperations playlistOperations;
    private final CollectionOperations collectionOperations;
    private final FeatureOperations featureOperations;
    private final PolicyOperations policyOperations;
    private final EventBus eventBus;
    private final Scheduler scheduler;

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
                return Observable.just(OfflineState.NOT_OFFLINE);
            }
        }
    };

    private final Func1<List<PlaylistItem>, List<Urn>> TO_URN = new Func1<List<PlaylistItem>, List<Urn>>() {
        @Override
        public List<Urn> call(List<PlaylistItem> playlistItems) {
            return Lists.transform(playlistItems, PlayableItem.TO_URN);
        }
    };

    private final Func1<List<Urn>, Observable<List<PlaylistWithTracks>>> loadPlaylistsTracksIfNecessary = new Func1<List<Urn>, Observable<List<PlaylistWithTracks>>>() {
        @Override
        public Observable<List<PlaylistWithTracks>> call(List<Urn> urns) {
            return playlistOperations.playlists(urns);
        }
    };

    private final Func1<List<Urn>, Observable<?>> addOfflinePlaylists = new Func1<List<Urn>, Observable<?>>() {
        @Override
        public Observable<?> call(List<Urn> playlists) {
            return offlineContentStorage.addOfflinePlaylists(playlists);
        }
    };

    private final Action1<Object> storeOfflineCollectionEnabled = new Action1<Object>() {
        @Override
        public void call(Object ignored) {
            offlineContentStorage.storeOfflineCollectionEnabled();
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
                             OfflineServiceInitiator serviceInitiator,
                             FeatureOperations featureOperations,
                             TrackDownloadsStorage tracksStorage,
                             PlaylistOperations playlistOperations,
                             CollectionOperations collectionOperations,
                             @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.storeDownloadUpdatesCommand = storeDownloadUpdatesCommand;
        this.loadTracksWithStalePolicies = loadTracksWithStalePolicies;
        this.clearTrackDownloadsCommand = clearTrackDownloadsCommand;
        this.eventBus = eventBus;
        this.offlineContentStorage = offlineContentStorage;
        this.policyOperations = policyOperations;
        this.loadExpectedContentCommand = loadExpectedContentCommand;
        this.loadOfflineContentUpdatesCommand = loadOfflineContentUpdatesCommand;
        this.serviceInitiator = serviceInitiator;
        this.featureOperations = featureOperations;
        this.tracksStorage = tracksStorage;
        this.playlistOperations = playlistOperations;
        this.collectionOperations = collectionOperations;
        this.scheduler = scheduler;
    }

    public Observable<Void> enableOfflineCollection() {
        final Observable<Object> storeCollectionsPlaylists = collectionOperations
                .myPlaylists().map(TO_URN)
                .flatMap(addOfflinePlaylists);
        return offlineContentStorage
                .storeLikedTrackCollection()
                .flatMap(continueWith(storeCollectionsPlaylists))
                .doOnNext(storeOfflineCollectionEnabled)
                .map(RxUtils.TO_VOID)
                .doOnNext(serviceInitiator.action1Start())
                .subscribeOn(scheduler);
    }

    public void disableOfflineCollection() {
        offlineContentStorage.storeOfflineCollectionDisabled();
    }

    public boolean isOfflineCollectionEnabled() {
        return offlineContentStorage.isOfflineCollectionEnabled();
    }

    public Observable<Void> disableOfflineLikedTracks() {
        return offlineContentStorage.deleteLikedTrackCollection()
                .doOnNext(eventBus.publishAction1(EventQueue.OFFLINE_CONTENT_CHANGED, OfflineContentChangedEvent.removed(true)))
                .doOnNext(serviceInitiator.action1Start())
                .map(RxUtils.TO_VOID)
                .subscribeOn(scheduler);
    }

    public Observable<Void> enableOfflineLikedTracks() {
        return offlineContentStorage.storeLikedTrackCollection()
                .doOnNext(serviceInitiator.action1Start())
                .map(RxUtils.TO_VOID)
                .subscribeOn(scheduler);
    }

    public Observable<Boolean> isOfflineLikedTracksEnabled() {
        return offlineContentStorage
                .isOfflineLikesEnabled()
                .subscribeOn(scheduler);
    }

    public Observable<Boolean> getOfflineLikedTracksStatusChanges() {
        final Observable<Boolean> changedEventObservable = eventBus
                .queue(EventQueue.OFFLINE_CONTENT_CHANGED)
                .filter(OfflineContentChangedEvent.HAS_LIKED_COLLECTION_CHANGE)
                .map(OfflineContentChangedEvent.TO_LIKED_TRACKS_OFFLINE_STATUS_CHANGE);
        return isOfflineLikedTracksEnabled().concatWith(changedEventObservable);
    }

    public Observable<Boolean> isOfflinePlaylist(Urn playlist) {
        return offlineContentStorage.isOfflinePlaylist(playlist).subscribeOn(scheduler);
    }

    public Observable<ChangeResult> makePlaylistAvailableOffline(final Urn playlistUrn) {
        return offlineContentStorage
                .storeAsOfflinePlaylist(playlistUrn)
                .doOnNext(serviceInitiator.action1Start())
                .subscribeOn(scheduler);
    }

    public Observable<ChangeResult> makePlaylistUnavailableOffline(final Urn playlistUrn) {
        return offlineContentStorage
                .removePlaylistFromOffline(playlistUrn)
                .doOnNext(eventBus.publishAction1(EventQueue.OFFLINE_CONTENT_CHANGED, OfflineContentChangedEvent.removed(playlistUrn)))
                .doOnNext(serviceInitiator.action1Start())
                .subscribeOn(scheduler);
    }

    public Observable<Boolean> getOfflineContentOrOfflineLikesStatusChanges() {
        return featureOperations.offlineContentEnabled().concatWith(getOfflineLikedTracksStatusChanges());
    }

    public Observable<Void> clearOfflineContent() {
        return notifyOfflineContentRemoved()
                .flatMap(continueWith(clearTrackDownloadsCommand.toObservable(null)))
                .doOnNext(serviceInitiator.action1Start())
                .map(RxUtils.TO_VOID)
                .subscribeOn(scheduler);
    }

    private Observable<?> notifyOfflineContentRemoved() {
        return Observable.zip(
                offlineContentStorage.loadOfflinePlaylists(),
                isOfflineLikedTracksEnabled(),
                new Func2<List<Urn>, Boolean, OfflineContentChangedEvent>() {
                    @Override
                    public OfflineContentChangedEvent call(List<Urn> playlists, Boolean isOfflineLikedTracks) {
                        return new OfflineContentChangedEvent(OfflineState.NOT_OFFLINE, playlists, isOfflineLikedTracks);
                    }
                })
                .doOnNext(new Action1<OfflineContentChangedEvent>() {
                    @Override
                    public void call(OfflineContentChangedEvent event) {
                        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, event);
                    }
                });
    }

    Observable<Boolean> getOfflineCollectionStateChanges() {
        return offlineContentStorage.getOfflineCollectionStateChanges();
    }

    Observable<OfflineContentUpdates> loadOfflineContentUpdates() {
        return offlineContentStorage
                .loadOfflinePlaylists()
                .flatMap(loadPlaylistsTracksIfNecessary)
                .flatMap(tryToUpdateAllPolicies())
                .flatMap(loadExpectedContentCommand.toContinuation())
                .flatMap(loadOfflineContentUpdatesCommand.toContinuation())
                .doOnNext(storeDownloadUpdatesCommand.toAction())
                .subscribeOn(scheduler);
    }

    private Func1<List<PlaylistWithTracks>, Observable<List<PlaylistWithTracks>>> tryToUpdateAllPolicies() {
        return new Func1<List<PlaylistWithTracks>, Observable<List<PlaylistWithTracks>>>() {
            @Override
            public Observable<List<PlaylistWithTracks>> call(List<PlaylistWithTracks> playlistWithTracks) {
                return updateOfflineContentStalePolicies()
                        .map(returning(playlistWithTracks))
                        .onErrorResumeNext(Observable.just(playlistWithTracks));
            }
        };
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

    public boolean hasOfflineContent() {
        return offlineContentStorage.hasOfflineContent();
    }

    public void setHasOfflineContent(boolean hasOfflineContent) {
        offlineContentStorage.setHasOfflineContent(hasOfflineContent);
    }
}
