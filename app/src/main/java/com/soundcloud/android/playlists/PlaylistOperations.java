package com.soundcloud.android.playlists;

import static com.soundcloud.android.playlists.AddTrackToPlaylistCommand.AddTrackToPlaylistParams;
import static com.soundcloud.android.playlists.RemoveTrackFromPlaylistCommand.RemoveTrackFromPlaylistParams;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaylistEntityChangedEvent;
import com.soundcloud.android.events.PlaylistTrackCountChangedEvent;
import com.soundcloud.android.events.UrnStateChangedEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.playlists.EditPlaylistCommand.EditPlaylistCommandParams;
import com.soundcloud.android.rx.RxJava;
import com.soundcloud.android.rx.observers.DefaultCompletableObserver;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.rx.eventbus.EventBus;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.SingleSource;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import java.util.List;

public class PlaylistOperations {

    private final Scheduler scheduler;
    private final Provider<LoadPlaylistTrackUrnsCommand> loadPlaylistTrackUrnsProvider;
    private final PlaylistRepository playlistRepository;
    private final PlaylistTracksStorage playlistTracksStorage;
    private final TrackRepository trackRepository;
    private final AddTrackToPlaylistCommand addTrackToPlaylistCommand;
    private final RemoveTrackFromPlaylistCommand removeTrackFromPlaylistCommand;
    private final EditPlaylistCommand editPlaylistCommand;
    private final SyncInitiator syncInitiator;
    private final OfflineContentOperations offlineContentOperations;
    private final RemovePlaylistFromDatabaseCommand removePlaylistCommand;
    private final EventBus eventBus;

    @Inject
    PlaylistOperations(@Named(ApplicationModule.RX_HIGH_PRIORITY) Scheduler scheduler,
                       SyncInitiator syncInitiator,
                       PlaylistRepository playlistRepository,
                       Provider<LoadPlaylistTrackUrnsCommand> loadPlaylistTrackUrnsProvider,
                       PlaylistTracksStorage playlistTracksStorage,
                       TrackRepository trackRepository,
                       AddTrackToPlaylistCommand addTrackToPlaylistCommand,
                       RemoveTrackFromPlaylistCommand removeTrackFromPlaylistCommand,
                       EditPlaylistCommand editPlaylistCommand,
                       OfflineContentOperations offlineContentOperations,
                       RemovePlaylistFromDatabaseCommand removePlaylistCommand,
                       EventBus eventBus) {
        this.scheduler = scheduler;
        this.syncInitiator = syncInitiator;
        this.playlistRepository = playlistRepository;
        this.loadPlaylistTrackUrnsProvider = loadPlaylistTrackUrnsProvider;
        this.playlistTracksStorage = playlistTracksStorage;
        this.trackRepository = trackRepository;
        this.addTrackToPlaylistCommand = addTrackToPlaylistCommand;
        this.removeTrackFromPlaylistCommand = removeTrackFromPlaylistCommand;
        this.editPlaylistCommand = editPlaylistCommand;
        this.offlineContentOperations = offlineContentOperations;
        this.removePlaylistCommand = removePlaylistCommand;
        this.eventBus = eventBus;
    }

    Completable removePlaylist(Urn playlist) {
        return removePlaylistCommand.toSingle(playlist)
                                    .flatMapCompletable(result -> offlineContentOperations.removeOfflinePlaylist(playlist))
                                    .subscribeOn(scheduler);
    }

    Single<List<AddTrackToPlaylistItem>> loadPlaylistForAddingTrack(Urn trackUrn) {
        return RxJava.toV2Single(playlistTracksStorage.loadAddTrackToPlaylistItems(trackUrn))
                     .subscribeOn(scheduler);
    }

    Single<Urn> createNewPlaylist(String title, boolean isPrivate, boolean isOffline, Urn firstTrackUrn) {
        return RxJava.toV2Single(playlistTracksStorage.createNewPlaylist(title, isPrivate, firstTrackUrn))
                     .flatMap(urn -> isOffline ? offlineContentOperations.makePlaylistAvailableOffline(urn).toSingle(() -> urn) : Single.just(urn))
                     .doOnSuccess(urn1 -> eventBus.publish(EventQueue.URN_STATE_CHANGED, UrnStateChangedEvent.fromEntityCreated(urn1)))
                     .doOnSuccess(__ -> this.requestSystemSync())
                     .subscribeOn(scheduler);
    }

    Maybe<Playlist> editPlaylist(Urn playlistUrn, String title, boolean isPrivate, List<Urn> updatedTracklist) {
        return editPlaylistCommand.toSingle(new EditPlaylistCommandParams(playlistUrn,
                                                                          title,
                                                                          isPrivate,
                                                                          updatedTracklist))
                                  .flatMapMaybe(o -> playlistRepository.withUrn(playlistUrn))
                                  .doOnSuccess(newPlaylistTrackData -> eventBus.publish(EventQueue.PLAYLIST_CHANGED, PlaylistEntityChangedEvent.fromPlaylistEdited(newPlaylistTrackData)))
                                  .doOnSuccess(playlist -> syncInitiator.syncPlaylistAndForget(playlist.urn()))
                                  .subscribeOn(scheduler);
    }

    Single<List<Track>> editPlaylistTracks(Urn playlistUrn, List<Urn> updatedTracklist) {
        return editPlaylistCommand.toSingle(new EditPlaylistCommandParams(playlistUrn, updatedTracklist))
                                  .flatMapMaybe(o -> playlistRepository.withUrn(playlistUrn))
                                  .doOnSuccess(newPlaylistTrackData -> eventBus.publish(EventQueue.PLAYLIST_CHANGED, PlaylistEntityChangedEvent.fromPlaylistEdited(newPlaylistTrackData)))
                                  .doOnSuccess(playlist -> syncInitiator.syncPlaylistAndForget(playlist.urn()))
                                  .flatMapSingle(playlist -> trackRepository.forPlaylist(playlist.urn()))
                                  .subscribeOn(scheduler);
    }

    Single<Integer> addTrackToPlaylist(Urn playlistUrn, Urn trackUrn) {
        final AddTrackToPlaylistParams params = new AddTrackToPlaylistParams(playlistUrn, trackUrn);
        return addTrackToPlaylistCommand.toSingle(params)
                                        .doOnSuccess(trackCount -> eventBus.publish(EventQueue.PLAYLIST_CHANGED, PlaylistTrackCountChangedEvent.fromTrackAddedToPlaylist(playlistUrn, trackCount)))
                                        .doOnSuccess(__ -> this.requestSystemSync())
                                        .subscribeOn(scheduler);
    }

    public Single<Integer> removeTrackFromPlaylist(Urn playlistUrn, Urn trackUrn) {
        final RemoveTrackFromPlaylistParams params = new RemoveTrackFromPlaylistParams(playlistUrn, trackUrn);
        return removeTrackFromPlaylistCommand.toSingle(params)
                                             .doOnSuccess(trackCount -> eventBus.publish(EventQueue.PLAYLIST_CHANGED,
                                                                                         PlaylistTrackCountChangedEvent.fromTrackRemovedFromPlaylist(playlistUrn, trackCount)))
                                             .doOnSuccess(__ -> this.requestSystemSync())
                                             .subscribeOn(scheduler);
    }

    public Single<List<Urn>> trackUrnsForPlayback(final Urn playlistUrn) {
        return loadPlaylistTrackUrnsProvider.get()
                                            .toSingle(playlistUrn)
                                            .subscribeOn(scheduler)
                                            .flatMap(urns -> backfilledTracks(playlistUrn, urns));
    }

    private SingleSource<? extends List<Urn>> backfilledTracks(Urn playlistUrn, List<Urn> urns) {
        if (urns.isEmpty()) {
            return updatedUrnsForPlayback(playlistUrn);
        } else {
            return Single.just(urns);
        }
    }

    private Single<List<Urn>> updatedUrnsForPlayback(final Urn playlistUrn) {
        return syncInitiator.syncPlaylist(playlistUrn)
                            .flatMap(syncJobResult -> loadPlaylistTrackUrnsProvider.get().toSingle(playlistUrn))
                            .subscribeOn(scheduler);
    }

    private void requestSystemSync() {
        syncInitiator.requestSystemSync().subscribe(new DefaultCompletableObserver());
    }

    public static class PlaylistMissingException extends Exception {
    }
}
