package com.soundcloud.android.offline;

import static com.soundcloud.android.rx.RxUtils.returning;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.CurrentDownloadEvent;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.policies.PolicyOperations;
import com.soundcloud.android.rx.RxUtils;
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

    public void enableOfflineCollection() {
        offlineContentStorage.storeOfflineCollectionEnabled();
    }

    public void disableOfflineCollection() {
        offlineContentStorage.storeOfflineCollectionDisabled();
    }

    public boolean isOfflineCollectionEnabled() {
        return offlineContentStorage.isOfflineCollectionEnabled();
    }

    public Observable<Void> disableOfflineLikedTracks() {
        return offlineContentStorage.storeOfflineLikesDisabled()
                .map(RxUtils.TO_VOID)
                .doOnNext(publishLikesMarkedForOfflineChange(false))
                .subscribeOn(scheduler);
    }

    public Observable<Void> enableOfflineLikedTracks() {
        return offlineContentStorage.storeOfflineLikesEnabled()
                .map(RxUtils.TO_VOID)
                .doOnNext(publishLikesMarkedForOfflineChange(true))
                .subscribeOn(scheduler);
    }

    public Observable<Boolean> isOfflineLikedTracksEnabled() {
        return offlineContentStorage
                .isOfflineLikesEnabled()
                .subscribeOn(scheduler);
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

    public Observable<Urn> makePlaylistAvailableOffline(final Urn playlistUrn) {
        return offlineContentStorage
                .storeAsOfflinePlaylist(playlistUrn)
                .map(returning(playlistUrn))
                .doOnNext(publishMarkedForOfflineChange(playlistUrn, true))
                .subscribeOn(scheduler);
    }

    public Observable<Urn> makePlaylistUnavailableOffline(final Urn playlistUrn) {
        return offlineContentStorage
                .removeFromOfflinePlaylists(playlistUrn)
                .map(returning(playlistUrn))
                .doOnNext(publishMarkedForOfflineChange(playlistUrn, false))
                .subscribeOn(scheduler);
    }

    Observable<Void> setOfflinePlaylists(final List<Urn> playlists) {
        return offlineContentStorage
                .setOfflinePlaylists(playlists)
                .map(RxUtils.TO_VOID)
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

    Observable<Boolean> getOfflineCollectionStateChanges() {
        return offlineContentStorage.getOfflineCollectionStateChanges();
    }

    private Action1<Urn> publishMarkedForOfflineChange(final Urn playlistUrn, final boolean isMarkedOffline) {
        return eventBus.publishAction1(EventQueue.ENTITY_STATE_CHANGED,
                EntityStateChangedEvent.fromMarkedForOffline(playlistUrn, isMarkedOffline));
    }

    private Action1<Void> publishLikesMarkedForOfflineChange(final boolean isMarkedOffline) {
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

    public boolean hasOfflineContent() {
        return offlineContentStorage.hasOfflineContent();
    }

    public void setHasOfflineContent(boolean hasOfflineContent) {
        offlineContentStorage.setHasOfflineContent(hasOfflineContent);
    }
}
