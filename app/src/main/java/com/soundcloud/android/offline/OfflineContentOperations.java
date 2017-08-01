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
import com.soundcloud.propeller.TxnResult;
import com.soundcloud.rx.eventbus.EventBusV2;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;

import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Set;

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
    private final EventBusV2 eventBus;
    private final Scheduler scheduler;
    private final IntroductoryOverlayOperations introductoryOverlayOperations;

    @Inject
    OfflineContentOperations(StoreDownloadUpdatesCommand storeDownloadUpdatesCommand,
                             OfflineStatePublisher publisher,
                             LoadTracksWithStalePoliciesCommand loadTracksWithStalePolicies,
                             ClearOfflineContentCommand clearOfflineContentCommand,
                             ResetOfflineContentCommand resetOfflineContentCommand,
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
                             LoadOfflinePlaylistsCommand loadOfflinePlaylistsCommand,
                             @Named(ApplicationModule.RX_HIGH_PRIORITY) Scheduler scheduler,
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

    public Observable<RxSignal> enableOfflineCollection() {
        return offlineContentStorage
                .addLikedTrackCollection()
                .flatMap(o -> setMyPlaylistsAsOfflinePlaylists())
                .doOnNext(ignored -> offlineContentStorage.addOfflineCollection())
                .doOnNext(serviceInitiator.startFromUserConsumer())
                .doOnNext(ignored -> introductoryOverlayOperations.setOverlayShown(IntroductoryOverlayKey.LISTEN_OFFLINE_LIKES, true))
                .flatMapSingle(ignored -> syncInitiatorBridge.refreshMyPlaylists())
                .map(RxUtils.TO_SIGNAL)
                .subscribeOn(scheduler);
    }

    private Observable<TxnResult> setMyPlaylistsAsOfflinePlaylists() {
        return collectionOperations.myPlaylists()
                                   .map(playlists -> Lists.transform(playlists, Playlist::urn))
                                   .flatMapObservable(offlineContentStorage::resetOfflinePlaylists);
    }

    public void disableOfflineCollection() {
        offlineContentStorage.removeOfflineCollection();
    }

    public boolean isOfflineCollectionEnabled() {
        return offlineContentStorage.isOfflineCollectionEnabled();
    }

    public Observable<RxSignal> disableOfflineLikedTracks() {
        return offlineContentStorage.removeLikedTrackCollection()
                                    .doOnNext(eventBus.publishAction1(EventQueue.OFFLINE_CONTENT_CHANGED,
                                                                      OfflineContentChangedEvent.removed(true)))
                                    .doOnNext(serviceInitiator.startFromUserConsumer())
                                    .doOnNext(serviceScheduler.scheduleCleanupConsumer())
                                    .map(RxUtils.TO_SIGNAL)
                                    .subscribeOn(scheduler);
    }

    public Observable<RxSignal> enableOfflineLikedTracks() {
        return offlineContentStorage.addLikedTrackCollection()
                                    .doOnNext(serviceInitiator.startFromUserConsumer())
                                    .doOnNext(ignored -> introductoryOverlayOperations.setOverlayShown(IntroductoryOverlayKey.LISTEN_OFFLINE_LIKES, true))
                                    .map(RxUtils.TO_SIGNAL)
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

    public Observable<RxSignal> makePlaylistAvailableOffline(final Urn playlist) {
        return makePlaylistAvailableOffline(singletonList(playlist));
    }

    Observable<RxSignal> makePlaylistAvailableOffline(final Set<Urn> playlistUrns) {
        return makePlaylistAvailableOffline(Lists.newArrayList(playlistUrns));
    }

    Observable<RxSignal> makePlaylistAvailableOffline(final List<Urn> playlistUrns) {
        return offlineContentStorage
                .storeAsOfflinePlaylists(playlistUrns)
                .doOnNext(eventBus.publishAction1(EventQueue.PLAYLIST_CHANGED, fromPlaylistsMarkedForDownload(playlistUrns)))
                .doOnNext(serviceInitiator.startFromUserConsumer())
                .doOnNext(ignored -> syncInitiator.syncPlaylistsAndForget(playlistUrns))
                .map(RxUtils.TO_SIGNAL)
                .subscribeOn(scheduler);
    }

    public Observable<RxSignal> makePlaylistUnavailableOffline(final Urn playlistUrn) {
        return makePlaylistUnavailableOffline(singletonList(playlistUrn));
    }

    Observable<RxSignal> makePlaylistUnavailableOffline(final Set<Urn> playlistUrns) {
        return makePlaylistUnavailableOffline(Lists.newArrayList(playlistUrns));
    }

    Observable<RxSignal> makePlaylistUnavailableOffline(final List<Urn> playlistUrns) {
        return offlineContentStorage
                .removePlaylistsFromOffline(playlistUrns)
                .doOnNext(eventBus.publishAction1(EventQueue.PLAYLIST_CHANGED,
                                                  fromPlaylistsUnmarkedForDownload(playlistUrns)))
                .doOnNext(eventBus.publishAction1(EventQueue.OFFLINE_CONTENT_CHANGED,
                                                  OfflineContentChangedEvent.removed(playlistUrns)))
                .doOnNext(serviceInitiator.startFromUserConsumer())
                .doOnNext(serviceScheduler.scheduleCleanupConsumer())
                .map(RxUtils.TO_SIGNAL)
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
                .doOnSuccess(serviceInitiator.startFromUserConsumer())
                .map(RxUtils.TO_SIGNAL)
                .subscribeOn(scheduler);
    }

    public Single<RxSignal> resetOfflineContent(OfflineContentLocation location) {
        return notifyOfflineContentRequested()
                .flatMap(ignored -> resetOfflineContentCommand.toSingle(location))
                .doOnSuccess(serviceInitiator.startFromUserConsumer())
                .map(RxUtils.TO_SIGNAL)
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
                loadOfflinePlaylistsCommand.toSingle(),
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
                .doOnSuccess(this::storeAndPublishUpdates)
                .subscribeOn(scheduler);
    }

    private void storeAndPublishUpdates(OfflineContentUpdates offlineContentUpdates) {
        // Store and Publish must be atomic from an RX perspective.
        // i.e. do not store without publishing upon unsubscribe
        storeDownloadUpdatesCommand.call(offlineContentUpdates);
        publishUpdates(offlineContentUpdates);
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
        return offlineContentStorage.hasOfflineContent();
    }

    void setHasOfflineContent(boolean hasOfflineContent) {
        offlineContentStorage.setHasOfflineContent(hasOfflineContent);
    }

}
