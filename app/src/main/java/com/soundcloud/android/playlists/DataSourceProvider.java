package com.soundcloud.android.playlists;

import static com.soundcloud.java.collections.Iterables.filter;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static com.soundcloud.java.collections.Lists.transform;
import static rx.Observable.concat;
import static rx.Observable.just;
import static rx.Observable.merge;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.model.ApiPlaylistPost;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.collection.playlists.MyPlaylistsOperations;
import com.soundcloud.android.collection.playlists.PlaylistsOptions;
import com.soundcloud.android.configuration.experiments.OtherPlaylistsByUserConfig;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaylistChangedEvent;
import com.soundcloud.android.events.PlaylistEntityChangedEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistWithExtrasState.PartialState;
import com.soundcloud.android.playlists.PlaylistWithExtrasState.PartialState.LoadingError;
import com.soundcloud.android.profile.ProfileApiMobile;
import com.soundcloud.android.rx.RxJava;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.android.transformers.Transformers;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.functions.Func1;
import rx.subjects.PublishSubject;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

class DataSourceProvider {

    @VisibleForTesting
    static final long STALE_TIME_MILLIS = TimeUnit.DAYS.toMillis(1);

    private final PlaylistRepository playlistRepository;
    private final TrackRepository trackRepository;
    private final EventBus eventBus;
    private final OtherPlaylistsByUserConfig otherPlaylistsByUserConfig;
    private final AccountOperations accountOperations;
    private final MyPlaylistsOperations myPlaylistsOperations;
    private final ProfileApiMobile profileApiMobile;
    private final SyncInitiator syncInitiator;
    private final PublishSubject<PartialState> refreshStateSubject = PublishSubject.create();

    @Inject
    DataSourceProvider(PlaylistRepository playlistRepository,
                       TrackRepository trackRepository,
                       EventBus eventBus,
                       OtherPlaylistsByUserConfig otherPlaylistsByUserConfig,
                       AccountOperations accountOperations,
                       MyPlaylistsOperations myPlaylistsOperations,
                       ProfileApiMobile profileApiMobile,
                       SyncInitiator syncInitiator) {
        this.playlistRepository = playlistRepository;
        this.trackRepository = trackRepository;
        this.eventBus = eventBus;
        this.otherPlaylistsByUserConfig = otherPlaylistsByUserConfig;
        this.accountOperations = accountOperations;
        this.myPlaylistsOperations = myPlaylistsOperations;
        this.profileApiMobile = profileApiMobile;
        this.syncInitiator = syncInitiator;
    }

    Observable<PlaylistWithExtrasState> dataWith(Urn playlistUrn, PublishSubject<Void> refresh) {
        final Observable<Urn> pageSequenceStarters = Observable.merge(
                refreshIntent(playlistUrn(playlistUrn), refresh), // the user refreshed
                playlistUrn(playlistUrn) // initial load, or the urn changed
        );
        return pageSequenceStarters
                .switchMap(
                        urn -> Observable.merge(
                                refreshStateSubject, // refresh state
                                playlistWithExtras(urn) // current load
                        )
                )
                .scan(
                        PlaylistWithExtrasState.initialState(),
                        (oldState, partialState) -> partialState.newState(oldState)
                );
    }

    private Observable<PartialState> playlistWithExtras(Urn urn) {
        return RxJava.toV1Observable(playlistRepository.withUrn(urn))
                     .flatMap(this::emissions)
                     .onErrorReturn(LoadingError::new);
    }

    private Observable<Urn> refreshIntent(Observable<Urn> latestUrn, PublishSubject<Void> refresh) {
        return latestUrn.compose(Transformers.takeWhen(refresh))
                        .flatMap(urn -> RxJava.toV1Observable(syncInitiator.syncPlaylist(urn))
                                              .map(syncJobResult -> urn)
                                              .doOnSubscribe(() -> refreshStateSubject.onNext(new PartialState.RefreshStarted()))
                                              .doOnError(throwable -> refreshStateSubject.onNext(new PartialState.RefreshError(throwable)))
                                              .onErrorResumeNext(Observable.empty()));
    }

    private Observable<PartialState> emissions(Playlist playlist) {
        return Observable.combineLatest(
                just(playlist),
                tracks(playlist.urn()).map(Optional::of),
                otherPlaylistsByUser(playlist).onErrorResumeNext(Observable.just(Collections.emptyList())),
                (updatedPlaylist, trackList, otherPlaylistsOpt) -> new PartialState.PlaylistWithExtrasLoaded(updatedPlaylist, trackList, otherPlaylistsOpt, isOwner(updatedPlaylist))
        ).cast(PartialState.class).startWith(new PartialState.PlaylistWithExtrasLoaded(playlist, Optional.absent(), Collections.emptyList(), isOwner(playlist)));
    }

    private boolean isOwner(Playlist playlist) {
        return accountOperations.isLoggedInUser(playlist.creatorUrn());
    }

    private Observable<List<Playlist>> otherPlaylistsByUser(Playlist playlist) {
        if (!otherPlaylistsByUserConfig.isEnabled()) {
            return Observable.just(Collections.emptyList());
        } else if (isOwner(playlist)) {
            return RxJava.toV1Observable(myPlaylistsOperations.myPlaylists(PlaylistsOptions.builder().showLikes(false).showPosts(true).build()))
                         .map(playlistsWithExclusion(playlist));
        } else {
            Observable<List<Playlist>> eagerEmission = just(Collections.<Playlist>emptyList());
            Observable<List<Playlist>> lazyEmission = playlistsForOtherUser(playlist)
                    .map(playlistsWithExclusion(playlist));

            return concat(eagerEmission, lazyEmission);
        }
    }

    private Observable<List<Playlist>> playlistsForOtherUser(Playlist playlist) {
        Urn creatorUrn = playlist.creatorUrn();
        return fromApi(playlist.isAlbum() ? profileApiMobile.userAlbums(creatorUrn) : profileApiMobile.userPlaylists(creatorUrn));
    }

    private Observable<List<Playlist>> fromApi(Observable<ModelCollection<ApiPlaylistPost>> apiPlaylistPosts) {
        return apiPlaylistPosts
                .map(posts -> transform(posts.getCollection(), ApiPlaylistPost::getApiPlaylist))
                .map(input -> transform(input, Playlist::from));
    }

    private static Func1<List<Playlist>, List<Playlist>> playlistsWithExclusion(Playlist playlist) {
        return playlistItems -> newArrayList(filter(playlistItems,
                                                    input -> !input.urn().equals(playlist.urn())
                                                            && input.isAlbum() == playlist.isAlbum()));
    }

    private Observable<List<Track>> tracks(Urn urn) {
        return merge(just(null), tracklistChanges(urn))
                .switchMap(ignored -> RxJava.toV1Observable(trackRepository.forPlaylist(urn, STALE_TIME_MILLIS)));
    }

    private Observable<PlaylistChangedEvent> tracklistChanges(Urn urn) {
        return eventBus.queue(EventQueue.PLAYLIST_CHANGED)
                       .filter(event -> event.changeMap().containsKey(urn))
                       .filter(event -> event.kind() == PlaylistChangedEvent.Kind.TRACK_ADDED || event.kind() == PlaylistChangedEvent.Kind.TRACK_REMOVED);
    }

    private Observable<Urn> playlistUrn(Urn initialUrn) {
        if (initialUrn.isLocal()) {
            return just(initialUrn);
        } else {
            return eventBus.queue(EventQueue.PLAYLIST_CHANGED)
                           .filter(event -> event.kind() == PlaylistChangedEvent.Kind.PLAYLIST_PUSHED_TO_SERVER)
                           .filter(event -> event.changeMap().containsKey(initialUrn))
                           .map(event -> ((PlaylistEntityChangedEvent) event).changeMap().get(initialUrn).urn())
                           .startWith(initialUrn);
        }

    }
}
