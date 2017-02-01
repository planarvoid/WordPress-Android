package com.soundcloud.android.playlists;

import static com.soundcloud.android.playlists.AddTrackToPlaylistCommand.AddTrackToPlaylistParams;
import static com.soundcloud.android.playlists.RemoveTrackFromPlaylistCommand.RemoveTrackFromPlaylistParams;
import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;
import static com.soundcloud.java.collections.Iterables.filter;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static com.soundcloud.java.collections.Lists.transform;
import static rx.Observable.concat;
import static rx.Observable.error;
import static rx.Observable.just;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.model.ApiPlaylistPost;
import com.soundcloud.android.collection.playlists.MyPlaylistsOperations;
import com.soundcloud.android.collection.playlists.PlaylistsOptions;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaylistEntityChangedEvent;
import com.soundcloud.android.events.PlaylistTrackCountChangedEvent;
import com.soundcloud.android.events.UrnStateChangedEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.playlists.EditPlaylistCommand.EditPlaylistCommandParams;
import com.soundcloud.android.profile.ProfileApiMobile;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncInitiatorBridge;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.collections.Pair;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
    private final ProfileApiMobile profileApiMobile;
    private final MyPlaylistsOperations myPlaylistsOperations;
    private final AccountOperations accountOperations;
    private final FeatureFlags featureFlags;
    private final PlaylistDetailsViewModelCreator viewModelCreator;

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
                       EventBus eventBus,
                       ProfileApiMobile profileApiMobile,
                       MyPlaylistsOperations myPlaylistsOperations,
                       AccountOperations accountOperations,
                       FeatureFlags featureFlags,
                       PlaylistDetailsViewModelCreator viewModelCreator) {
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
        this.profileApiMobile = profileApiMobile;
        this.myPlaylistsOperations = myPlaylistsOperations;
        this.accountOperations = accountOperations;
        this.featureFlags = featureFlags;
        this.viewModelCreator = viewModelCreator;
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
                                  .flatMap(o -> playlistRepository.withUrn(playlistUrn))
                                  .doOnNext(newPlaylistTrackData -> eventBus.publish(EventQueue.PLAYLIST_CHANGED, PlaylistEntityChangedEvent.fromPlaylistEdited(newPlaylistTrackData)))
                                  .doOnCompleted(syncInitiator.requestSystemSyncAction())
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
                                            .flatMap(new Func1<List<Urn>, Observable<List<Urn>>>() {
                                                @Override
                                                public Observable<List<Urn>> call(List<Urn> trackItems) {
                                                    if (trackItems.isEmpty()) {
                                                        return updatedUrnsForPlayback(playlistUrn);
                                                    } else {
                                                        return just(trackItems);
                                                    }
                                                }
                                            });
    }

    public Observable<Playlist> playlist(final Urn playlistUrn) {
        return playlistRepository.withUrn(playlistUrn)
                                 .flatMap(syncIfNecessary(playlistUrn))
                                 .switchIfEmpty(updatedPlaylist(playlistUrn));
    }

    public Observable<Playlist> updatedPlaylist(final Urn playlistUrn) {
        return syncInitiator
                .syncPlaylist(playlistUrn)
                .observeOn(scheduler)
                .flatMap(new Func1<SyncJobResult, Observable<Playlist>>() {
                    @Override
                    public Observable<Playlist> call(SyncJobResult playlistWasUpdated) {
                        return playlistRepository.withUrn(playlistUrn)
                                                 .switchIfEmpty(error(new PlaylistMissingException()));
                    }
                });
    }

    Observable<PlaylistDetailsViewModel> playlistWithTracksAndRecommendations(Urn playlistUrn) {
        return playlistWithTracks(playlistUrn)
                .flatMap(addOtherPlaylists())
                .subscribeOn(scheduler);
    }

    Observable<PlaylistDetailsViewModel> updatedPlaylistWithTracksAndRecommendations(Urn playlistUrn) {
        return syncInitiator.syncPlaylist(playlistUrn)
                            .flatMap(ignored -> playlistWithTracksAndRecommendations(playlistUrn))
                            .subscribeOn(scheduler);
    }

    private Observable<Pair<Playlist, List<Track>>> playlistWithTracks(Urn playlistUrn) {
        return Observable.combineLatest(playlistRepository.withUrn(playlistUrn),
                              trackRepository
                                      .forPlaylist(playlistUrn)
                                      .startWith(Collections.<Track>emptyList())
                                      .debounce(300, TimeUnit.MILLISECONDS)
                                      .onErrorResumeNext(throwable -> just(Collections.emptyList())),
                              Pair::of).switchIfEmpty(error(new PlaylistMissingException()));
    }

    private Func1<Pair<Playlist, List<Track>>, Observable<PlaylistDetailsViewModel>> addOtherPlaylists() {
        return playlistWithTracks -> {
            final Playlist playlist = playlistWithTracks.first();
            final List<Track> tracks = playlistWithTracks.second();

            if (tracks.isEmpty() || featureFlags.isDisabled(Flag.OTHER_PLAYLISTS_BY_CREATOR)) {
                return just(Collections.<Playlist>emptyList())
                        .map(toViewModel(playlist, tracks));

            } else if (accountOperations.isLoggedInUser(playlist.creatorUrn())) {
                return myOtherPlaylists()
                        .map(playlistsWithExclusion(playlist))
                        .map(toViewModel(playlist, tracks));
            } else {

                Observable<PlaylistDetailsViewModel> withoutOtherPlaylists = just(Collections.<Playlist>emptyList())
                        .map(toViewModel(playlist, tracks));
                Observable<PlaylistDetailsViewModel> withOtherPlaylists = playlistsForOtherUser(playlist.creatorUrn())
                        .map(playlistsWithExclusion(playlist))
                        .map(toViewModel(playlist, tracks));

                return concat(withoutOtherPlaylists, withOtherPlaylists);
            }
        };
    }

    private Func1<List<Playlist>, List<Playlist>> playlistsWithExclusion(Playlist playlist) {
        return playlistItems -> newArrayList(filter(playlistItems,
                                                    input -> !input.urn().equals(playlist.urn())));
    }



    private Func1<List<Playlist>, PlaylistDetailsViewModel> toViewModel(Playlist playlist, List<Track> tracks) {
        return playlists -> {
            final List<PlaylistItem> playlistsItems = Lists.transform(playlists, PlaylistItem::from);
            return viewModelCreator.create(playlist,
                                           Lists.transform(tracks, TrackItem::from),
                                           playlist.isLikedByCurrentUser().or(false),
                                           false,
                                           playlist.offlineState().or(OfflineState.NOT_OFFLINE),
                                           playlistsItems.isEmpty() ? Optional.absent() :
                                           Optional.of(new PlaylistDetailOtherPlaylistsItem(playlist.creatorName(), playlistsItems)));
        };
    }

    private Observable<List<Playlist>> playlistsForOtherUser(Urn userUrn) {
        return profileApiMobile.userPlaylists(userUrn)
                               .map(apiPlaylistPosts -> transform(apiPlaylistPosts.getCollection(), ApiPlaylistPost::getApiPlaylist))
                               .map(input -> Lists.transform(input, Playlist::from));
    }

    private Observable<List<Playlist>> myOtherPlaylists() {
        return myPlaylistsOperations.myPlaylists(PlaylistsOptions.builder().showLikes(false).showPosts(true).build());
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
