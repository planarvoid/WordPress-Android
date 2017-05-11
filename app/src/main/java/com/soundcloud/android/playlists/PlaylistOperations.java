package com.soundcloud.android.playlists;

import static com.soundcloud.android.playlists.AddTrackToPlaylistCommand.AddTrackToPlaylistParams;
import static com.soundcloud.android.playlists.RemoveTrackFromPlaylistCommand.RemoveTrackFromPlaylistParams;
import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;
import static rx.Observable.just;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaylistEntityChangedEvent;
import com.soundcloud.android.events.PlaylistTrackCountChangedEvent;
import com.soundcloud.android.events.UrnStateChangedEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.playlists.EditPlaylistCommand.EditPlaylistCommandParams;
import com.soundcloud.android.rx.RxJava;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncInitiatorBridge;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.rx.eventbus.EventBus;
import io.reactivex.Maybe;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import java.util.List;

public class PlaylistOperations {

    private final Action1<Urn> publishPlaylistCreatedEvent = new Action1<Urn>() {
        @Override
        public void call(Urn urn) {
            eventBus.publish(EventQueue.URN_STATE_CHANGED, UrnStateChangedEvent.fromEntityCreated(urn));
        }
    };

    private final Scheduler scheduler;
    private final Provider<LoadPlaylistTrackUrnsCommand> loadPlaylistTrackUrnsProvider;
    private final PlaylistRepository playlistRepository;
    private final PlaylistTracksStorage playlistTracksStorage;
    private final TrackRepository trackRepository;
    private final AddTrackToPlaylistCommand addTrackToPlaylistCommand;
    private final RemoveTrackFromPlaylistCommand removeTrackFromPlaylistCommand;
    private final EditPlaylistCommand editPlaylistCommand;
    private final SyncInitiator syncInitiator;
    private final SyncInitiatorBridge syncInitiatorBridge;
    private final OfflineContentOperations offlineContentOperations;
    private final EventBus eventBus;

    @Inject
    PlaylistOperations(@Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler,
                       SyncInitiator syncInitiator,
                       PlaylistRepository playlistRepository,
                       Provider<LoadPlaylistTrackUrnsCommand> loadPlaylistTrackUrnsProvider,
                       PlaylistTracksStorage playlistTracksStorage,
                       TrackRepository trackRepository,
                       AddTrackToPlaylistCommand addTrackToPlaylistCommand,
                       RemoveTrackFromPlaylistCommand removeTrackFromPlaylistCommand,
                       EditPlaylistCommand editPlaylistCommand,
                       SyncInitiatorBridge syncInitiatorBridge,
                       OfflineContentOperations offlineContentOperations,
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
        this.syncInitiatorBridge = syncInitiatorBridge;
        this.offlineContentOperations = offlineContentOperations;
        this.eventBus = eventBus;
    }

    Observable<List<AddTrackToPlaylistItem>> loadPlaylistForAddingTrack(Urn trackUrn) {
        return playlistTracksStorage
                .loadAddTrackToPlaylistItems(trackUrn)
                .subscribeOn(scheduler);
    }

    Observable<Urn> createNewPlaylist(String title, boolean isPrivate, boolean isOffline, Urn firstTrackUrn) {
        return playlistTracksStorage.createNewPlaylist(title, isPrivate, firstTrackUrn)
                                    .flatMap(urn -> isOffline ?
                                                    offlineContentOperations.makePlaylistAvailableOffline(urn)
                                                                            .map(aVoid -> urn) :
                                                    Observable.just(urn))
                                    .doOnNext(publishPlaylistCreatedEvent)
                                    .subscribeOn(scheduler)
                                    .doOnCompleted(syncInitiator.requestSystemSyncAction());
    }

    Observable<Playlist> editPlaylist(Playlist playlist, List<Urn> tracks) {
        return editPlaylist(playlist.urn(), playlist.title(), playlist.isPrivate(), tracks);
    }

    Observable<Playlist> editPlaylist(Urn playlistUrn, String title, boolean isPrivate, List<Urn> updatedTracklist) {
        return editPlaylistCommand.toObservable(new EditPlaylistCommandParams(playlistUrn,
                                                                              title,
                                                                              isPrivate,
                                                                              updatedTracklist))
                                  .flatMap(o -> RxJava.toV1Observable(playlistRepository.withUrn(playlistUrn)))
                                  .doOnNext(newPlaylistTrackData -> eventBus.publish(EventQueue.PLAYLIST_CHANGED, PlaylistEntityChangedEvent.fromPlaylistEdited(newPlaylistTrackData)))
                                  .doOnNext(playlist -> fireAndForget(syncInitiator.syncPlaylist(playlist.urn())))
                                  .subscribeOn(scheduler);
    }

    Observable<List<Track>> editPlaylistTracks(Urn playlistUrn, List<Urn> updatedTracklist) {
        return editPlaylistCommand.toObservable(new EditPlaylistCommandParams(playlistUrn, updatedTracklist))
                                  .flatMap(o -> RxJava.toV1Observable(playlistRepository.withUrn(playlistUrn)))
                                  .doOnNext(newPlaylistTrackData -> eventBus.publish(EventQueue.PLAYLIST_CHANGED, PlaylistEntityChangedEvent.fromPlaylistEdited(newPlaylistTrackData)))
                                  .doOnNext(playlist -> fireAndForget(syncInitiator.syncPlaylist(playlist.urn())))
                                  .flatMap(playlist -> RxJava.toV1Observable(trackRepository.forPlaylist(playlist.urn())))
                                  .subscribeOn(scheduler);
    }

    Observable<Integer> addTrackToPlaylist(Urn playlistUrn, Urn trackUrn) {
        final AddTrackToPlaylistParams params = new AddTrackToPlaylistParams(playlistUrn, trackUrn);
        return addTrackToPlaylistCommand.toObservable(params)
                                        .doOnNext(trackCount -> eventBus.publish(EventQueue.PLAYLIST_CHANGED, PlaylistTrackCountChangedEvent.fromTrackAddedToPlaylist(playlistUrn, trackCount)))
                                        .doOnCompleted(syncInitiator.requestSystemSyncAction())
                                        .subscribeOn(scheduler);
    }

    public Observable<Integer> removeTrackFromPlaylist(Urn playlistUrn, Urn trackUrn) {
        final RemoveTrackFromPlaylistParams params = new RemoveTrackFromPlaylistParams(playlistUrn, trackUrn);
        return removeTrackFromPlaylistCommand.toObservable(params)
                                             .doOnNext(trackCount -> eventBus.publish(EventQueue.PLAYLIST_CHANGED,
                                                                                      PlaylistTrackCountChangedEvent.fromTrackRemovedFromPlaylist(playlistUrn, trackCount)))
                                             .doOnCompleted(syncInitiator.requestSystemSyncAction())
                                             .subscribeOn(scheduler);
    }

    public Observable<List<Urn>> trackUrnsForPlayback(final Urn playlistUrn) {
        return loadPlaylistTrackUrnsProvider.get().with(playlistUrn)
                                            .toObservable()
                                            .subscribeOn(scheduler)
                                            .flatMap(trackItems -> {
                                                if (trackItems.isEmpty()) {
                                                    return updatedUrnsForPlayback(playlistUrn);
                                                } else {
                                                    return just(trackItems);
                                                }
                                            });
    }

    public Observable<Playlist> playlist(final Urn playlistUrn) {
        return RxJava.toV1Observable(playlistRepository.withUrn(playlistUrn))
                     .flatMap(syncIfNecessary(playlistUrn))
                     .switchIfEmpty(updatedPlaylist(playlistUrn));
    }

    private Observable<Playlist> updatedPlaylist(final Urn playlistUrn) {
        return syncInitiator
                .syncPlaylist(playlistUrn)
                .observeOn(scheduler)
                .flatMap(playlistWasUpdated -> RxJava.toV1Observable(playlistRepository.withUrn(playlistUrn)
                                                                                       .switchIfEmpty(Maybe.error(new PlaylistMissingException()))));
    }

    private Observable<List<Urn>> updatedUrnsForPlayback(final Urn playlistUrn) {
        return syncInitiator
                .syncPlaylist(playlistUrn)
                .flatMap(new Func1<SyncJobResult, Observable<List<Urn>>>() {
                    @Override
                    public Observable<List<Urn>> call(SyncJobResult syncJobResult) {
                        return loadPlaylistTrackUrnsProvider.get().with(playlistUrn)
                                                            .toObservable()
                                                            .subscribeOn(scheduler);
                    }
                });
    }

    private Func1<Playlist, Observable<Playlist>> syncIfNecessary(final Urn playlistUrn) {
        return playlist -> {
            final boolean isLocalPlaylist = playlistUrn.getNumericId() < 0;
            if (isLocalPlaylist) {
                fireAndForget(syncInitiatorBridge.refreshMyPlaylists());
                return just(playlist);
            } else {
                return just(playlist);
            }
        };
    }

    public static class PlaylistMissingException extends Exception {

    }
}
