package com.soundcloud.android.offline;

import static com.soundcloud.android.events.PlaylistMarkedForOfflineStateChangedEvent.fromPlaylistsMarkedForDownload;
import static com.soundcloud.android.events.PlaylistMarkedForOfflineStateChangedEvent.fromPlaylistsUnmarkedForDownload;
import static java.util.Collections.singletonList;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.collection.CollectionOperations;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.introductoryoverlay.IntroductoryOverlayKey;
import com.soundcloud.android.introductoryoverlay.IntroductoryOverlayOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.Playlist;
import com.soundcloud.android.policies.PolicyOperations;
import com.soundcloud.android.rx.RxJava;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncInitiatorBridge;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.propeller.TxnResult;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;

import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;
import java.util.List;

public class OfflineContentOperations {

    private final StoreDownloadUpdatesCommand storeDownloadUpdatesCommand;
    private final OfflineStatePublisher publisher;
    private final LoadTracksWithStalePoliciesCommand loadTracksWithStalePolicies;
    private final LoadExpectedContentCommand loadExpectedContentCommand;
    private final LoadOfflineContentUpdatesCommand loadOfflineContentUpdatesCommand;
    private final ClearOfflineContentCommand clearOfflineContentCommand;
    private final LoadOfflinePlaylistsCommand loadOfflinePlaylistsCommand;
    private final ResetOfflineContentCommand resetOfflineContentCommand;
    private final OfflineServiceInitiator serviceInitiator;
    private final OfflineContentScheduler serviceScheduler;
    private final SyncInitiatorBridge syncInitiatorBridge;
    private final SyncInitiator syncInitiator;
    private final TrackDownloadsStorage tracksStorage;
    private final OfflineContentStorage offlineContentStorage;
    private final CollectionOperations collectionOperations;
    private final FeatureOperations featureOperations;
    private final PolicyOperations policyOperations;
    private final EventBus eventBus;
    private final Scheduler scheduler;
    private final IntroductoryOverlayOperations introductoryOverlayOperations;

    private final Func1<Collection<Urn>, Observable<?>> UPDATE_POLICIES =
            new Func1<Collection<Urn>, Observable<?>>() {
                @Override
                public Observable<?> call(Collection<Urn> urns) {
                    if (urns.isEmpty()) {
                        return Observable.just(null);
                    }
                    return policyOperations.updatePolicies(urns);
                }
            };

    @Inject
    OfflineContentOperations(StoreDownloadUpdatesCommand storeDownloadUpdatesCommand,
                             OfflineStatePublisher publisher,
                             LoadTracksWithStalePoliciesCommand loadTracksWithStalePolicies,
                             ClearOfflineContentCommand clearOfflineContentCommand,
                             ResetOfflineContentCommand resetOfflineContentCommand,
                             EventBus eventBus,
                             OfflineContentStorage offlineContentStorage,
                             PolicyOperations policyOperations,
                             LoadExpectedContentCommand loadExpectedContentCommand,
                             LoadOfflineContentUpdatesCommand loadOfflineContentUpdatesCommand,
                             OfflineServiceInitiator serviceInitiator,
                             OfflineContentScheduler serviceScheduler,
                             SyncInitiatorBridge syncInitiatorBridge,
                             SyncInitiator syncInitiator,
                             FeatureOperations featureOperations,
                             TrackDownloadsStorage tracksStorage,
                             CollectionOperations collectionOperations,
                             LoadOfflinePlaylistsCommand loadOfflinePlaylistsCommand,
                             @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler,
                             IntroductoryOverlayOperations introductoryOverlayOperations) {
        this.storeDownloadUpdatesCommand = storeDownloadUpdatesCommand;
        this.publisher = publisher;
        this.loadTracksWithStalePolicies = loadTracksWithStalePolicies;
        this.clearOfflineContentCommand = clearOfflineContentCommand;
        this.resetOfflineContentCommand = resetOfflineContentCommand;
        this.eventBus = eventBus;
        this.offlineContentStorage = offlineContentStorage;
        this.policyOperations = policyOperations;
        this.loadExpectedContentCommand = loadExpectedContentCommand;
        this.loadOfflineContentUpdatesCommand = loadOfflineContentUpdatesCommand;
        this.serviceInitiator = serviceInitiator;
        this.serviceScheduler = serviceScheduler;
        this.syncInitiatorBridge = syncInitiatorBridge;
        this.syncInitiator = syncInitiator;
        this.featureOperations = featureOperations;
        this.tracksStorage = tracksStorage;
        this.collectionOperations = collectionOperations;
        this.loadOfflinePlaylistsCommand = loadOfflinePlaylistsCommand;
        this.scheduler = scheduler;
        this.introductoryOverlayOperations = introductoryOverlayOperations;
    }

    public Observable<Void> enableOfflineCollection() {
        return offlineContentStorage
                .addLikedTrackCollection()
                .flatMap(o -> setMyPlaylistsAsOfflinePlaylists())
                .doOnNext(ignored -> offlineContentStorage.addOfflineCollection())
                .doOnNext(serviceInitiator.startFromUserAction())
                .doOnNext(ignored -> introductoryOverlayOperations.setOverlayShown(IntroductoryOverlayKey.LISTEN_OFFLINE_LIKES, true))
                .flatMap(ignored -> RxJava.toV1Observable(syncInitiatorBridge.refreshMyPlaylists()))
                .map(RxUtils.TO_VOID)
                .subscribeOn(scheduler);
    }

    private Observable<TxnResult> setMyPlaylistsAsOfflinePlaylists() {
        return RxJava.toV1Observable(collectionOperations.myPlaylists())
                     .map(playlists -> Lists.transform(playlists, Playlist::urn))
                     .flatMap(offlineContentStorage::resetOfflinePlaylists);
    }

    public void disableOfflineCollection() {
        offlineContentStorage.removeOfflineCollection();
    }

    public boolean isOfflineCollectionEnabled() {
        return offlineContentStorage.isOfflineCollectionEnabled();
    }

    public Observable<Void> disableOfflineLikedTracks() {
        return offlineContentStorage.removeLikedTrackCollection()
                                    .doOnNext(eventBus.publishAction1(EventQueue.OFFLINE_CONTENT_CHANGED,
                                                                      OfflineContentChangedEvent.removed(true)))
                                    .doOnNext(serviceInitiator.startFromUserAction())
                                    .doOnNext(serviceScheduler.scheduleCleanupAction())
                                    .map(RxUtils.TO_VOID)
                                    .subscribeOn(scheduler);
    }

    public Observable<Void> enableOfflineLikedTracks() {
        return offlineContentStorage.addLikedTrackCollection()
                                    .doOnNext(serviceInitiator.startFromUserAction())
                                    .doOnNext(ignored -> introductoryOverlayOperations.setOverlayShown(IntroductoryOverlayKey.LISTEN_OFFLINE_LIKES, true))
                                    .map(RxUtils.TO_VOID)
                                    .subscribeOn(scheduler);
    }

    Observable<Boolean> isOfflineLikedTracksEnabled() {
        return RxJava.toV1Observable(offlineContentStorage
                .isOfflineLikesEnabled())
                .subscribeOn(scheduler);
    }

    public Observable<Boolean> getOfflineLikedTracksStatusChanges() {
        final Observable<Boolean> changedEventObservable = eventBus
                .queue(EventQueue.OFFLINE_CONTENT_CHANGED)
                .filter(OfflineContentChangedEvent.HAS_LIKED_COLLECTION_CHANGE)
                .map(OfflineContentChangedEvent.TO_LIKES_COLLECTION_MARKED_OFFLINE);
        return isOfflineLikedTracksEnabled().concatWith(changedEventObservable);
    }

    Observable<Boolean> isOfflinePlaylist(Urn playlist) {
        return offlineContentStorage.isOfflinePlaylist(playlist).subscribeOn(scheduler);
    }

    public Observable<Void> makePlaylistAvailableOffline(final Urn playlist) {
        return makePlaylistAvailableOffline(singletonList(playlist));
    }

    Observable<Void> makePlaylistAvailableOffline(final List<Urn> playlistUrns) {
        return offlineContentStorage
                .storeAsOfflinePlaylists(playlistUrns)
                .doOnNext(eventBus.publishAction1(EventQueue.PLAYLIST_CHANGED, fromPlaylistsMarkedForDownload(playlistUrns)))
                .doOnNext(serviceInitiator.startFromUserAction())
                .doOnNext(ignored -> syncInitiator.syncPlaylistsAndForget(playlistUrns))
                .map(RxUtils.TO_VOID)
                .subscribeOn(scheduler);
    }

    public Observable<Void> makePlaylistUnavailableOffline(final Urn playlistUrn) {
        return makePlaylistUnavailableOffline(singletonList(playlistUrn));
    }

    Observable<Void> makePlaylistUnavailableOffline(final List<Urn> playlistUrns) {
        return offlineContentStorage
                .removePlaylistsFromOffline(playlistUrns)
                .doOnNext(eventBus.publishAction1(EventQueue.PLAYLIST_CHANGED,
                                                  fromPlaylistsUnmarkedForDownload(playlistUrns)))
                .doOnNext(eventBus.publishAction1(EventQueue.OFFLINE_CONTENT_CHANGED,
                                                  OfflineContentChangedEvent.removed(playlistUrns)))
                .doOnNext(serviceInitiator.startFromUserAction())
                .doOnNext(serviceScheduler.scheduleCleanupAction())
                .map(RxUtils.TO_VOID)
                .subscribeOn(scheduler);
    }

    public Observable<Boolean> getOfflineContentOrOfflineLikesStatusChanges() {
        return RxJava.toV1Observable(featureOperations.offlineContentEnabled()).concatWith(getOfflineLikedTracksStatusChanges());
    }

    public io.reactivex.Observable<Boolean> getOfflineContentOrOfflineLikesStatusChangesV2() {
        return featureOperations.offlineContentEnabled().concatWith(RxJava.toV2Observable(getOfflineLikedTracksStatusChanges()));
    }

    public Observable<Void> disableOfflineFeature() {
        return clearOfflineContent()
                .doOnNext(ignored -> disableOfflineCollection());
    }

    public Observable<Void> clearOfflineContent() {
        return notifyOfflineContentRemoved()
                .flatMap(ignored -> clearOfflineContentCommand.toObservable(null))
                .doOnNext(serviceInitiator.startFromUserAction())
                .map(RxUtils.TO_VOID)
                .subscribeOn(scheduler);
    }

    public Observable<Void> resetOfflineContent(OfflineContentLocation location) {
        return notifyOfflineContentRequested()
                .flatMap(ignored -> resetOfflineContentCommand.toObservable(location))
                .doOnNext(serviceInitiator.startFromUserAction())
                .map(RxUtils.TO_VOID)
                .subscribeOn(scheduler);
    }

    private Observable<?> notifyOfflineContentRemoved() {
        return notifyOfflineContent(OfflineState.NOT_OFFLINE);
    }

    private Observable<?> notifyOfflineContentRequested() {
        return notifyOfflineContent(OfflineState.REQUESTED);
    }

    private Observable<?> notifyOfflineContent(OfflineState state) {
        return Observable.zip(
                loadOfflinePlaylistsCommand.toObservable(null),
                isOfflineLikedTracksEnabled(),
                (playlists, isOfflineLikedTracks) -> new OfflineContentChangedEvent(state,
                                                                                    playlists,
                                                                                    isOfflineLikedTracks))
                         .doOnNext(event -> eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, event));
    }

    Observable<OfflineContentUpdates> loadOfflineContentUpdates() {
        return tryToUpdatePolicies()
                .flatMap(loadExpectedContentCommand.toContinuation())
                .flatMap(loadOfflineContentUpdatesCommand.toContinuation())
                .doOnNext(this::storeAndPublishUpdates)
                .subscribeOn(scheduler);
    }

    private void storeAndPublishUpdates(OfflineContentUpdates offlineContentUpdates) {
        // Store and Publish must be atomic from an RX perspective.
        // i.e. do not store without publishing upon unsubscribe
        storeDownloadUpdatesCommand.toAction1().call(offlineContentUpdates);
        publishUpdates(offlineContentUpdates);
    }

    private void publishUpdates(OfflineContentUpdates updates) {
        publisher.publishEmptyCollections(updates.userExpectedOfflineContent());
        publisher.publishRemoved(updates.tracksToRemove());
        publisher.publishDownloaded(updates.tracksToRestore());
        publisher.publishUnavailable(updates.unavailableTracks());
    }

    private Observable<?> tryToUpdatePolicies() {
        final Observable<?> resumeObservable = Observable.just(null);
        return updateOfflineContentStalePolicies().onErrorResumeNext(resumeObservable);
    }

    Observable<List<Urn>> loadContentToDelete() {
        return tracksStorage.getTracksToRemove().subscribeOn(scheduler);
    }

    @VisibleForTesting
    Observable<Object> updateOfflineContentStalePolicies() {
        return loadTracksWithStalePolicies.toObservable(null)
                                          .flatMap(UPDATE_POLICIES)
                                          .subscribeOn(scheduler);
    }

    public boolean hasOfflineContent() {
        return offlineContentStorage.hasOfflineContent();
    }

    void setHasOfflineContent(boolean hasOfflineContent) {
        offlineContentStorage.setHasOfflineContent(hasOfflineContent);
    }

}
