package com.soundcloud.android.playlists;

import static com.soundcloud.java.collections.Iterables.filter;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static com.soundcloud.java.collections.Lists.transform;
import static rx.Observable.concat;
import static rx.Observable.just;
import static rx.Observable.merge;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
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
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.android.transformers.Transformers;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.functions.Func1;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;
import rx.subscriptions.CompositeSubscription;

import android.support.annotation.NonNull;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@AutoFactory(allowSubclasses = true)
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
    private final BehaviorSubject<PlaylistWithExtrasState> data = BehaviorSubject.create();
    private final BehaviorSubject<Urn> latestUrn;
    private final Observable<Object> refresh;
    private CompositeSubscription subscription;

    @Inject
    DataSourceProvider(Urn initialUrn,
                       Observable<Object> refresh,
                       @Provided PlaylistRepository playlistRepository,
                       @Provided TrackRepository trackRepository,
                       @Provided EventBus eventBus,
                       @Provided OtherPlaylistsByUserConfig otherPlaylistsByUserConfig,
                       @Provided AccountOperations accountOperations,
                       @Provided MyPlaylistsOperations myPlaylistsOperations,
                       @Provided ProfileApiMobile profileApiMobile,
                       @Provided SyncInitiator syncInitiator) {
        this.refresh = refresh;
        this.playlistRepository = playlistRepository;
        this.trackRepository = trackRepository;
        this.eventBus = eventBus;
        this.otherPlaylistsByUserConfig = otherPlaylistsByUserConfig;
        this.accountOperations = accountOperations;
        this.myPlaylistsOperations = myPlaylistsOperations;
        this.profileApiMobile = profileApiMobile;

        // we store the latest urn in this subject, as it may change (when a local playlist is pushed)
        latestUrn = BehaviorSubject.create(initialUrn);
        this.syncInitiator = syncInitiator;
    }

    public void connect() {
        // two things that can result in a new paging sequence. When either emits, start over
        Observable<Urn> pageSequenceStarters = Observable.merge(
                refreshIntent(), // the user refreshed
                latestUrn // initial load, or the urn changed
        );

        subscription = new CompositeSubscription();
        subscription.addAll(

                // keep up with URN updates when we push to the server
                urnChanged().subscribe(latestUrn),

                // the actual output
                pageSequenceStarters.switchMap(
                        urn -> Observable.merge(
                                refreshStateSubject, // refresh state
                                playlistWithExtras(urn) // current load
                        ).scan(
                                PlaylistWithExtrasState.initialState(),
                                (oldState, partialState) -> partialState.newState(oldState)
                        )
                ).subscribe(data)
        );
    }

    public void disconnect() {
        subscription.unsubscribe();
    }

    public BehaviorSubject<PlaylistWithExtrasState> data() {
        return data;
    }

    @NonNull
    private Observable<PartialState> playlistWithExtras(Urn urn) {
        return playlistRepository.withUrn(urn)
                                 .flatMap(this::emissions)
                                 .onErrorReturn(LoadingError::new);
    }

    private Observable<Urn> refreshIntent() {
        return latestUrn.compose(Transformers.takeWhen(refresh))
                        .flatMap(urn -> syncInitiator.syncPlaylist(urn)
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
                PartialState.PlaylistWithExtrasLoaded::new
        ).cast(PartialState.class).startWith(new PartialState.PlaylistWithExtrasLoaded(playlist, Optional.absent(), Collections.emptyList()));
    }

    private Observable<List<Playlist>> otherPlaylistsByUser(Playlist playlist) {
        if (!otherPlaylistsByUserConfig.isEnabled()) {
            return Observable.just(Collections.emptyList());
        } else if (accountOperations.isLoggedInUser(playlist.creatorUrn())) {
            return myPlaylistsOperations.myPlaylists(PlaylistsOptions.builder().showLikes(false).showPosts(true).build())
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

    private Func1<List<Playlist>, List<Playlist>> playlistsWithExclusion(Playlist playlist) {
        return playlistItems -> newArrayList(filter(playlistItems,
                                                    input -> !input.urn().equals(playlist.urn())
                                                            && input.isAlbum() == playlist.isAlbum()));
    }

    private Observable<List<Track>> tracks(Urn urn) {
        return merge(just(null), tracklistChanges(urn))
                .switchMap(ignored -> trackRepository.forPlaylist(urn, STALE_TIME_MILLIS));
    }

    @NonNull
    private Observable<PlaylistChangedEvent> tracklistChanges(Urn urn) {
        return eventBus.queue(EventQueue.PLAYLIST_CHANGED)
                       .filter(event -> event.changeMap().containsKey(urn))
                       .filter(event -> event.kind() == PlaylistChangedEvent.Kind.TRACK_ADDED || event.kind() == PlaylistChangedEvent.Kind.TRACK_REMOVED);
    }

    private Observable<Urn> urnChanged() {
        return eventBus.queue(EventQueue.PLAYLIST_CHANGED)
                       .filter(event -> event.kind() == PlaylistChangedEvent.Kind.PLAYLIST_PUSHED_TO_SERVER)
                       .filter(event -> event.changeMap().containsKey(latestUrn.getValue()))
                       .map(event -> ((PlaylistEntityChangedEvent) event).changeMap().get(latestUrn.getValue()).urn());

    }
}
