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
import com.soundcloud.android.rx.RxSignal;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncInitiatorBridge;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.rx.eventbus.EventBusV2;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;

import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class OfflineContentOperations {

    private final OfflineStatePublisher publisher;
    private final LoadTracksWithStalePoliciesCommand loadTracksWithStalePolicies;
    private final LoadExpectedContentCommand loadExpectedContentCommand;
    private final LoadOfflineContentUpdatesCommand loadOfflineContentUpdatesCommand;
    private final ClearOfflineContentCommand clearOfflineContentCommand;
    private final OfflineServiceInitiator serviceInitiator;
    private final OfflineContentScheduler serviceScheduler;
    private final SyncInitiatorBridge syncInitiatorBridge;
    private final SyncInitiator syncInitiator;
    private final TrackDownloadsStorage tracksStorage;
    private final OfflineContentStorage offlineContentStorage;
    private final CollectionOperations collectionOperations;
    private final FeatureOperations featureOperations;
    private final PolicyOperations policyOperations;
    private final EventBusV2 eventBus;
    private final Scheduler scheduler;
    private final IntroductoryOverlayOperations introductoryOverlayOperations;
    private final OfflineSettingsStorage offlineSettingsStorage;
    private final TrackOfflineStateProvider trackOfflineStateProvider;
    private final SecureFileStorage secureFileStorage;

    @Inject
    OfflineContentOperations(OfflineStatePublisher publisher,
                             LoadTracksWithStalePoliciesCommand loadTracksWithStalePolicies,
                             ClearOfflineContentCommand clearOfflineContentCommand,
                             EventBusV2 eventBus,
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
                             @Named(ApplicationModule.RX_HIGH_PRIORITY) Scheduler scheduler,
                             IntroductoryOverlayOperations introductoryOverlayOperations,
                             OfflineSettingsStorage offlineSettingsStorage,
                             TrackOfflineStateProvider trackOfflineStateProvider,
                             SecureFileStorage secureFileStorage) {
        this.publisher = publisher;
        this.loadTracksWithStalePolicies = loadTracksWithStalePolicies;
        this.clearOfflineContentCommand = clearOfflineContentCommand;
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
        this.scheduler = scheduler;
        this.introductoryOverlayOperations = introductoryOverlayOperations;
        this.offlineSettingsStorage = offlineSettingsStorage;
        this.trackOfflineStateProvider = trackOfflineStateProvider;
        this.secureFileStorage = secureFileStorage;
    }

    public Single<RxSignal> enableOfflineCollection() {
        return offlineContentStorage
                .addLikedTrackCollection()
                .toSingle(() -> RxSignal.SIGNAL)
                .flatMapCompletable(o -> setMyPlaylistsAsOfflinePlaylists())
                .doOnComplete(offlineSettingsStorage::addOfflineCollection)
                .doOnComplete(serviceInitiator::startFromUserConsumer)
                .doOnComplete(() -> introductoryOverlayOperations.setOverlayShown(IntroductoryOverlayKey.LISTEN_OFFLINE_LIKES, true))
                .toSingle(() -> RxSignal.SIGNAL)
                .flatMap(ignored -> syncInitiatorBridge.refreshMyPlaylists())
                .map(RxUtils.TO_SIGNAL)
                .subscribeOn(scheduler);
    }

    public Completable removeOfflinePlaylist(Urn playlist) {
        return offlineContentStorage.removePlaylistsFromOffline(playlist)
                                    .subscribeOn(scheduler);
    }

    private Completable setMyPlaylistsAsOfflinePlaylists() {
        return collectionOperations.myPlaylists()
                                   .map(playlists -> Lists.transform(playlists, Playlist::urn))
                                   .flatMapCompletable(offlineContentStorage::resetOfflinePlaylists);
    }

    public void disableOfflineCollection() {
        offlineSettingsStorage.removeOfflineCollection();
    }

    public boolean isOfflineCollectionEnabled() {
        return offlineSettingsStorage.isOfflineCollectionEnabled();
    }

    public Completable disableOfflineLikedTracks() {
        return offlineContentStorage.removeLikedTrackCollection()
                                    .doOnComplete(eventBus.publishAction0(EventQueue.OFFLINE_CONTENT_CHANGED, OfflineContentChangedEvent.removed(true)))
                                    .doOnComplete(serviceInitiator::startFromUserConsumer)
                                    .doOnComplete(serviceScheduler.actionScheduleCleanupConsumer())
                                    .subscribeOn(scheduler);
    }

    public Completable enableOfflineLikedTracks() {
        return offlineContentStorage.addLikedTrackCollection()
                                    .doOnComplete(serviceInitiator::startFromUserConsumer)
                                    .doOnComplete(() -> introductoryOverlayOperations.setOverlayShown(IntroductoryOverlayKey.LISTEN_OFFLINE_LIKES, true))
                                    .subscribeOn(scheduler);
    }

    Single<Boolean> isOfflineLikedTracksEnabled() {
        return offlineContentStorage.isOfflineLikesEnabled().subscribeOn(scheduler);
    }

    public Observable<Boolean> getOfflineLikedTracksStatusChanges() {
        final Observable<Boolean> changedEventObservable = eventBus
                .queue(EventQueue.OFFLINE_CONTENT_CHANGED)
                .filter(event -> event.isLikedTrackCollection)
                .map(event -> event.state != OfflineState.NOT_OFFLINE);
        return isOfflineLikedTracksEnabled().toObservable().concatWith(changedEventObservable);
    }

    Single<Boolean> isOfflinePlaylist(Urn playlist) {
        return offlineContentStorage.isOfflinePlaylist(playlist).subscribeOn(scheduler);
    }

    public Completable makePlaylistAvailableOffline(final Urn playlist) {
        return makePlaylistAvailableOffline(singletonList(playlist));
    }

    Completable makePlaylistAvailableOffline(final Set<Urn> playlistUrns) {
        return makePlaylistAvailableOffline(Lists.newArrayList(playlistUrns));
    }

    Completable makePlaylistAvailableOffline(final List<Urn> playlistUrns) {
        return offlineContentStorage
                .storeAsOfflinePlaylists(playlistUrns)
                .doOnComplete(eventBus.publishAction0(EventQueue.PLAYLIST_CHANGED, fromPlaylistsMarkedForDownload(playlistUrns)))
                .doOnComplete(serviceInitiator::startFromUserConsumer)
                .doOnComplete(() -> syncInitiator.syncPlaylistsAndForget(playlistUrns))
                .subscribeOn(scheduler);
    }

    public Completable replaceOfflinePlaylist(Urn toReplace, Urn updatedPlaylist) {
        return isOfflinePlaylist(toReplace).filter(aBoolean -> aBoolean)
                                           .flatMapCompletable(__ -> offlineContentStorage.removePlaylistsFromOffline(Collections.singletonList(toReplace))
                                                                                          .andThen(makePlaylistAvailableOffline(singletonList(updatedPlaylist))));
    }

    public Completable makePlaylistUnavailableOffline(final Urn playlistUrn) {
        return makePlaylistUnavailableOffline(singletonList(playlistUrn));
    }

    Completable makePlaylistUnavailableOffline(final Set<Urn> playlistUrns) {
        return makePlaylistUnavailableOffline(Lists.newArrayList(playlistUrns));
    }

    Completable makePlaylistUnavailableOffline(final List<Urn> playlistUrns) {
        return offlineContentStorage
                .removePlaylistsFromOffline(playlistUrns)
                .doOnComplete(eventBus.publishAction0(EventQueue.PLAYLIST_CHANGED,
                                                      fromPlaylistsUnmarkedForDownload(playlistUrns)))
                .doOnComplete(eventBus.publishAction0(EventQueue.OFFLINE_CONTENT_CHANGED,
                                                      OfflineContentChangedEvent.removed(playlistUrns)))
                .doOnComplete(serviceInitiator::startFromUserConsumer)
                .doOnComplete(serviceScheduler.actionScheduleCleanupConsumer())
                .subscribeOn(scheduler);
    }

    public Observable<Boolean> getOfflineContentOrOfflineLikesStatusChanges() {
        return featureOperations.offlineContentEnabled().concatWith(getOfflineLikedTracksStatusChanges());
    }

    public Observable<Boolean> getOfflineContentOrOfflineLikesStatusChangesV2() {
        return featureOperations.offlineContentEnabled().concatWith(getOfflineLikedTracksStatusChanges());
    }

    public Single<RxSignal> disableOfflineFeature() {
        return clearOfflineContent()
                .doOnSuccess(signal -> disableOfflineCollection());
    }

    public Single<RxSignal> clearOfflineContent() {
        return notifyOfflineContentRemoved()
                .flatMap(ignored -> clearOfflineContentCommand.toSingle())
                .doOnSuccess(__ -> serviceInitiator.startFromUserConsumer())
                .map(RxUtils.TO_SIGNAL)
                .subscribeOn(scheduler);
    }

    public Completable resetOfflineContent(OfflineContentLocation location) {
        return notifyOfflineContentRequested()
                .flatMapCompletable(ignored -> tracksStorage.getResetTracksToRequested()
                                                            .andThen(Completable.fromAction(() -> {
                                                                trackOfflineStateProvider.clear();
                                                                secureFileStorage.deleteAllTracks();
                                                                offlineSettingsStorage.setOfflineContentLocation(location);
                                                                secureFileStorage.updateOfflineDir();
                                                            })))
                .doOnComplete(() -> serviceInitiator.startFromUserConsumer())
                .subscribeOn(scheduler);
    }

    private Single<?> notifyOfflineContentRemoved() {
        return notifyOfflineContent(OfflineState.NOT_OFFLINE);
    }

    private Single<?> notifyOfflineContentRequested() {
        return notifyOfflineContent(OfflineState.REQUESTED);
    }

    private Single<?> notifyOfflineContent(OfflineState state) {
        return Single.zip(
                offlineContentStorage.getOfflinePlaylists(),
                isOfflineLikedTracksEnabled(),
                (playlists, isOfflineLikedTracks) -> new OfflineContentChangedEvent(state,
                                                                                    playlists,
                                                                                    isOfflineLikedTracks))
                     .doOnSuccess(event -> eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, event));
    }

    Single<OfflineContentUpdates> loadOfflineContentUpdates() {
        return tryToUpdatePolicies()
                .andThen(loadExpectedContentCommand.toSingle())
                .flatMap(loadOfflineContentUpdatesCommand::toSingle)
                .flatMap(this::storeAndPublishUpdates)
                .subscribeOn(scheduler);
    }

    private Single<OfflineContentUpdates> storeAndPublishUpdates(OfflineContentUpdates offlineContentUpdates) {
        // Store and Publish must be atomic from an RX perspective.
        // i.e. do not store without publishing upon unsubscribe
        return tracksStorage.writeUpdates(offlineContentUpdates)
                            .doOnSuccess(transaction -> publishUpdates(offlineContentUpdates))
                            .map(transaction -> offlineContentUpdates);

    }

    private void publishUpdates(OfflineContentUpdates updates) {
        publisher.publishEmptyCollections(updates.userExpectedOfflineContent());
        publisher.publishRemoved(updates.tracksToRemove());
        publisher.publishDownloaded(updates.tracksToRestore());
        publisher.publishUnavailable(updates.unavailableTracks());
    }

    private Completable tryToUpdatePolicies() {
        return updateOfflineContentStalePolicies().onErrorComplete();
    }

    Single<List<Urn>> loadContentToDelete() {
        return tracksStorage.getTracksToRemove().subscribeOn(scheduler);
    }

    @VisibleForTesting
    Completable updateOfflineContentStalePolicies() {
        return loadTracksWithStalePolicies.toSingle()
                                          .flatMapCompletable(urns -> urns.isEmpty() ? Completable.complete() : RxJava.toV2Completable(policyOperations.updatePolicies(urns)))
                                          .subscribeOn(scheduler);
    }

    public boolean hasOfflineContent() {
        return offlineSettingsStorage.hasOfflineContent();
    }

    void setHasOfflineContent(boolean hasOfflineContent) {
        offlineSettingsStorage.setHasOfflineContent(hasOfflineContent);
    }

}
