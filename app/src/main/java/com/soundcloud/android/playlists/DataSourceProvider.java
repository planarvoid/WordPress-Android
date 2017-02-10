package com.soundcloud.android.playlists;

import static com.soundcloud.java.collections.Iterables.filter;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static com.soundcloud.java.collections.Lists.transform;
import static rx.Observable.combineLatest;
import static rx.Observable.concat;
import static rx.Observable.empty;
import static rx.Observable.just;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.model.ApiPlaylistPost;
import com.soundcloud.android.collection.playlists.MyPlaylistsOperations;
import com.soundcloud.android.collection.playlists.PlaylistsOptions;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaylistChangedEvent;
import com.soundcloud.android.events.PlaylistEntityChangedEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.profile.ProfileApiMobile;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;
import rx.subjects.BehaviorSubject;
import rx.subscriptions.CompositeSubscription;

import android.support.annotation.NonNull;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@AutoFactory(allowSubclasses = true)
class DataSourceProvider {

    private PlaylistRepository playlistRepository;
    private TrackRepository trackRepository;
    private EventBus eventBus;

    private FeatureFlags featureFlags;
    private AccountOperations accountOperations;
    private MyPlaylistsOperations myPlaylistsOperations;
    private ProfileApiMobile profileApiMobile;

    private final BehaviorSubject<Urn> playlistUpdates;
    private final BehaviorSubject<Urn> playlistOrTracklistUpdates;
    private final BehaviorSubject<PlaylistWithExtras> data = BehaviorSubject.create();

    private final Scheduler scheduler;
    private CompositeSubscription subscription;

    @Inject
    DataSourceProvider(Urn initialUrn,
                       @Provided @Named(ApplicationModule.LOW_PRIORITY) Scheduler scheduler,
                       @Provided PlaylistRepository playlistRepository,
                       @Provided TrackRepository trackRepository,
                       @Provided EventBus eventBus,
                       @Provided FeatureFlags featureFlags,
                       @Provided AccountOperations accountOperations,
                       @Provided MyPlaylistsOperations myPlaylistsOperations,
                       @Provided ProfileApiMobile profileApiMobile) {
        this.scheduler = scheduler;
        this.playlistRepository = playlistRepository;
        this.trackRepository = trackRepository;
        this.eventBus = eventBus;
        this.featureFlags = featureFlags;
        this.accountOperations = accountOperations;
        this.myPlaylistsOperations = myPlaylistsOperations;
        this.profileApiMobile = profileApiMobile;

        // we cannot store the urn, as it may change. We use these subjects to respond to these changes, also to know when to refresh
        playlistUpdates = BehaviorSubject.create(initialUrn);
        playlistOrTracklistUpdates = BehaviorSubject.create(initialUrn);
    }

    public void connect() {
        subscription = new CompositeSubscription();
        subscription.addAll(
                playlisUpdatesOnly().subscribe(playlistUpdates),
                playlistOrTracklistUpdates().subscribe(playlistOrTracklistUpdates),
                dataSource().subscribe(data)
        );
    }

    public void disconnect() {
        subscription.unsubscribe();
    }

    public BehaviorSubject<PlaylistWithExtras> data() {
        return data;
    }

    Observable<PlaylistWithExtras> dataSource() {
        return Observable.combineLatest(

                playlistOrTracklistUpdates.switchMap(this::playlistWithTracks),

                playlistUpdates.switchMap(this::otherPlaylistsByUser),

                (playlistWithTracks, otherPlaylists) -> {
                    if (!playlistWithTracks.tracksOpt.isPresent() || playlistWithTracks.tracksOpt.get().isEmpty() || otherPlaylists.isEmpty()) {
                        return PlaylistWithExtras.create(playlistWithTracks.playlist, playlistWithTracks.tracksOpt);
                    } else {
                        return PlaylistWithExtras.create(playlistWithTracks.playlist, playlistWithTracks.tracksOpt, otherPlaylists);
                    }
                }
        );
    }

    private Observable<PlaylistWithTracks> playlistWithTracks(Urn urn) {
        return combineLatest(playlist(urn), tracks(urn), PlaylistWithTracks::new);
    }

    private Observable<List<Playlist>> otherPlaylistsByUser(Urn urn) {
        if (featureFlags.isDisabled(Flag.OTHER_PLAYLISTS_BY_CREATOR)) {
            return empty();
        } else {
            return playlist(urn).flatMap(playlist -> {
                if (accountOperations.isLoggedInUser(playlist.creatorUrn())) {
                    return myPlaylistsOperations.myPlaylists(PlaylistsOptions.builder().showLikes(false).showPosts(true).build())
                                                .map(playlistsWithExclusion(playlist))
                                                .filter(playlists -> !playlists.isEmpty());
                } else {

                    Observable<List<Playlist>> withoutOtherPlaylists = just(Collections.<Playlist>emptyList());
                    Observable<List<Playlist>> withOtherPlaylists = playlistsForOtherUser(playlist.creatorUrn())
                            .map(playlistsWithExclusion(playlist));

                    return concat(withoutOtherPlaylists, withOtherPlaylists);
                }
            });
        }
    }

    private Observable<List<Playlist>> playlistsForOtherUser(Urn creatorUrn) {
        return profileApiMobile.userPlaylists(creatorUrn)
                               .map(apiPlaylistPosts -> transform(apiPlaylistPosts.getCollection(), ApiPlaylistPost::getApiPlaylist))
                               .map(input -> Lists.transform(input, Playlist::from));
    }

    private Func1<List<Playlist>, List<Playlist>> playlistsWithExclusion(Playlist playlist) {
        return playlistItems -> newArrayList(filter(playlistItems,
                                                    input -> !input.urn().equals(playlist.urn())));
    }

    private Observable<Optional<List<Track>>> tracks(Urn urn) {
        return trackRepository.forPlaylist(urn)
                              .map(Optional::of)
                              .startWith(Optional.absent())
                              .debounce(100, TimeUnit.MILLISECONDS, scheduler);
    }

    private Observable<Playlist> playlist(Urn urn) {
        return playlistRepository.withUrn(urn);
    }

    private Observable<Urn> playlisUpdatesOnly() {
        return eventBus.queue(EventQueue.PLAYLIST_CHANGED)
                       .filter(event -> event.changeMap().containsKey(playlistUpdates.getValue()))
                       .filter(event -> !event.isPlaylistEdited() && event.isEntityChangeEvent())
                       .map(getNextUrn(playlistOrTracklistUpdates.getValue()));
    }

    private Observable<Urn> playlistOrTracklistUpdates() {
        return eventBus.queue(EventQueue.PLAYLIST_CHANGED)
                       .filter(event -> event.changeMap().containsKey(playlistOrTracklistUpdates.getValue()))
                       .filter(event -> !event.isPlaylistEdited() && (event.isEntityChangeEvent() || event.isTracklistChangeEvent()))
                       .map(getNextUrn(playlistOrTracklistUpdates.getValue()));

    }

    @NonNull
    private Func1<PlaylistChangedEvent, Urn> getNextUrn(Urn lastUrn) {
        return event -> {
            if (event.kind() == PlaylistChangedEvent.Kind.PLAYLIST_PUSHED_TO_SERVER) {
                return ((PlaylistEntityChangedEvent) event).changeMap().get(lastUrn).urn();
            } else {
                return lastUrn;
            }
        };
    }

    private static class PlaylistWithTracks {

        private final Playlist playlist;
        private final Optional<List<Track>> tracksOpt;

        private PlaylistWithTracks(Playlist playlist, Optional<List<Track>> tracksOpt) {
            this.playlist = playlist;
            this.tracksOpt = tracksOpt;
        }
    }
}
