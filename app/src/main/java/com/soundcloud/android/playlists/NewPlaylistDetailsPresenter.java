package com.soundcloud.android.playlists;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;
import static java.util.Collections.singleton;
import static rx.Observable.combineLatest;
import static com.soundcloud.java.optional.Optional.of;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.events.EntityMetadata;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineInteractionEvent;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.events.PlaylistChangedEvent;
import com.soundcloud.android.events.PlaylistEntityChangedEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.likes.LikedStatuses;
import com.soundcloud.android.likes.LikesStateProvider;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.offline.OfflineProperties;
import com.soundcloud.android.offline.OfflinePropertiesProvider;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.playback.playqueue.PlayQueueHelper;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.android.transformers.Transformers;
import com.soundcloud.android.view.AsyncViewModel;
import com.soundcloud.java.collections.Pair;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.observables.ConnectableObservable;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;
import rx.subscriptions.CompositeSubscription;

import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@AutoFactory
class NewPlaylistDetailsPresenter implements PlaylistDetailsInputs {

    private final PlaylistOperations playlistOperations;
    private final PlaybackInitiator playbackInitiator;
    private final LikesStateProvider likesStateProvider;
    private final PlayQueueHelper playQueueHelper;
    private final OfflinePropertiesProvider offlinePropertiesProvider;
    private final SyncInitiator syncInitiator;
    private final EventBus eventBus;
    private final OfflineContentOperations offlineContentOperations;
    private final EventTracker eventTracker;
    private final LikeOperations likeOperations;
    private final Urn playlistUrn;
    private final SearchQuerySourceInfo searchQuerySourceInfo;
    private final PromotedSourceInfo promotedSourceInfo;
    private final String screen;

    private final PlaylistDetailsViewModelCreator viewModelCreator;

    private Subscription subscription = RxUtils.invalidSubscription();

    // inputs
    private final BehaviorSubject<Boolean> refresh = BehaviorSubject.create(false);
    private final BehaviorSubject<Boolean> editMode = BehaviorSubject.create(false);
    private final PublishSubject<Void> offlineAvailable = PublishSubject.create();
    private final PublishSubject<Void> offlineUnavailable = PublishSubject.create();
    private final PublishSubject<Void> onCreatorClicked = PublishSubject.create();
    private final PublishSubject<Void> headerPlayClicked = PublishSubject.create();
    private final PublishSubject<Void> like = PublishSubject.create();
    private final PublishSubject<Void> unlike = PublishSubject.create();

    // outputs
    private final BehaviorSubject<AsyncViewModel<PlaylistDetailsViewModel>> viewModelSubject = BehaviorSubject.create();
    private final PublishSubject<Urn> gotoCreator = PublishSubject.create();
    private final PublishSubject<PlaybackResult.ErrorReason> playbackError = PublishSubject.create();
    private final ConnectableObservable<PlaylistWithTracks> dataSource;

    NewPlaylistDetailsPresenter(Urn playlistUrn,
                                String screen,
                                @Nullable SearchQuerySourceInfo searchQuerySourceInfo,
                                @Nullable PromotedSourceInfo promotedSourceInfo,
                                @Provided PlaybackInitiator playbackInitiator,
                                @Provided PlaylistOperations playlistOperations,
                                @Provided LikesStateProvider likesStateProvider,
                                @Provided PlayQueueHelper playQueueHelper,
                                @Provided OfflinePropertiesProvider offlinePropertiesProvider,
                                @Provided SyncInitiator syncInitiator,
                                @Provided EventBus eventBus,
                                @Provided OfflineContentOperations offlineContentOperations,
                                @Provided EventTracker eventTracker,
                                @Provided LikeOperations likeOperations,
                                @Provided PlaylistDetailsViewModelCreator viewModelCreator,
                                @Provided NewPlaylistDetailsPresenter_DataSourceProviderFactory dataSourceProviderFactory) {
        this.searchQuerySourceInfo = searchQuerySourceInfo;
        this.promotedSourceInfo = promotedSourceInfo;
        this.screen = screen;
        this.playbackInitiator = playbackInitiator;
        this.playlistOperations = playlistOperations;
        this.likesStateProvider = likesStateProvider;
        this.playQueueHelper = playQueueHelper;
        this.offlinePropertiesProvider = offlinePropertiesProvider;
        this.syncInitiator = syncInitiator;
        this.eventBus = eventBus;
        this.playlistUrn = playlistUrn;
        this.offlineContentOperations = offlineContentOperations;
        this.eventTracker = eventTracker;
        this.likeOperations = likeOperations;
        this.viewModelCreator = viewModelCreator;
        this.dataSource = dataSourceProviderFactory.create(playlistUrn).data().replay(1);
    }

    public void connect() {
        subscription.unsubscribe();
        subscription = new CompositeSubscription(

                actionGoToCreator(onCreatorClicked),
                actionMakeAvailableOffline(offlineAvailable),
                actionMakeOfflineUnavailableOffline(offlineUnavailable),
                actionLike(like),
                actionUnlike(unlike),
                actionPlayPlaylist(headerPlayClicked),

                emitViewModel(dataSource)
        );

        dataSource.connect();
    }

    private Subscription emitViewModel(Observable<PlaylistWithTracks> playlistWithTracks) {
        return
                combineLatest(
                        refresh,
                        editMode,
                        playlistWithTracks,
                        likesStateProvider.likedStatuses().distinctUntilChanged(likedStatuses -> likedStatuses.isLiked(playlistUrn)),
                        offlinePropertiesProvider.states(),
                        this::combine)
                        .doOnNext(viewModelSubject::onNext)
                        .subscribe(new DefaultSubscriber<>());
    }

    private Subscription actionPlayPlaylist(PublishSubject<Void> trigger) {
        return dataSource.compose(Transformers.takeWhen(trigger))
                         .map(playlistWithTracks -> playlistWithTracks.playlist().urn())
                         .flatMap(playlistOperations::trackUrnsForPlayback)
                         .withLatestFrom(playSessionSource(), Pair::of)
                         .flatMap(pair -> playbackInitiator.playTracks(pair.first(), 0, pair.second()))
                         .subscribe(showPlaybackResult());
    }

    private Subscription actionUnlike(PublishSubject<Void> trigger) {
        return dataSource.compose(Transformers.takeWhen(trigger))
                         .map(PlaylistWithTracks::playlist)
                         .withLatestFrom(playSessionSource(), Pair::of)
                         .subscribe(playlistSourcePair -> doLike(playlistSourcePair, false));
    }

    private Subscription actionLike(PublishSubject<Void> trigger) {
        return dataSource.compose(Transformers.takeWhen(trigger))
                         .map(PlaylistWithTracks::playlist)
                         .withLatestFrom(playSessionSource(), Pair::of)
                         .subscribe(playlistSourcePair -> doLike(playlistSourcePair, true));
    }

    private Subscription actionMakeOfflineUnavailableOffline(PublishSubject<Void> trigger) {
        return dataSource.compose(Transformers.takeWhen(trigger))
                         .map(playlistWithTracks -> playlistWithTracks.playlist().urn())
                         .withLatestFrom(playSessionSource(), Pair::of)
                         .subscribe(makePlaylistUnavailableOffline());
    }

    private Subscription actionMakeAvailableOffline(PublishSubject<Void> trigger) {
        return dataSource.compose(Transformers.takeWhen(trigger))
                         .map(playlistWithTracks -> playlistWithTracks.playlist().urn())
                         .withLatestFrom(playSessionSource(), Pair::of)
                         .subscribe(makePlaylistAvailableOffline());
    }

    private Observable<PlaySessionSource> playSessionSource() {
        return dataSource.map(createPlaySessionSource());
    }

    private Subscription actionGoToCreator(PublishSubject<Void> trigger) {
        return dataSource.compose(Transformers.takeWhen(trigger))
                         .map(playlistWithTracks -> playlistWithTracks.playlist().creatorUrn())
                         .subscribe(gotoCreator);
    }

    private Action1<Pair<Urn, PlaySessionSource>> makePlaylistAvailableOffline() {
        return urnSourcePair -> {
            fireAndForget(offlineContentOperations.makePlaylistAvailableOffline(urnSourcePair.first()));
            eventBus.publish(EventQueue.TRACKING, getOfflinePlaylistTrackingEvent(urnSourcePair.first(), true, urnSourcePair.second()));
        };
    }

    private Action1<Pair<Urn, PlaySessionSource>> makePlaylistUnavailableOffline() {
        return urnSourcePair -> {
            fireAndForget(offlineContentOperations.makePlaylistUnavailableOffline(urnSourcePair.first()));
            eventBus.publish(EventQueue.TRACKING, getOfflinePlaylistTrackingEvent(urnSourcePair.first(), false, urnSourcePair.second()));

        };
    }

    private void doLike(Pair<Playlist, PlaySessionSource> playlistSourcePair, boolean isLike) {
        Playlist playlist = playlistSourcePair.first();
        eventTracker.trackEngagement(UIEvent.fromToggleLike(isLike,
                                                            playlist.urn(),
                                                            getEventContext(),
                                                            playlistSourcePair.second().getPromotedSourceInfo(),
                                                            createEntityMetadata(playlist)));

        fireAndForget(likeOperations.toggleLike(playlist.urn(), isLike));
    }

    private EntityMetadata createEntityMetadata(Playlist playlist) {
        return EntityMetadata.from(playlist.creatorName(), playlist.creatorUrn(),
                                   playlist.title(), playlist.urn());
    }

    private Action1<PlaybackResult> showPlaybackResult() {
        return playbackResult -> {
            if (playbackResult.isSuccess()) {
                eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.expandPlayer());
            } else {
                playbackError.onNext(playbackResult.getErrorReason());
            }
        };
    }

    private Func1<PlaylistWithTracks, PlaySessionSource> createPlaySessionSource() {
        return playlistWithTracks -> {
            final Playlist playlist = playlistWithTracks.playlist();
            PlaySessionSource playSessionSource1 = PlaySessionSource.forPlaylist(screen, playlist.urn(), playlist.creatorUrn(), playlist.trackCount());
            if (promotedSourceInfo != null) {
                playSessionSource1.setPromotedSourceInfo(promotedSourceInfo);
            } else if (searchQuerySourceInfo != null) {
                playSessionSource1.setSearchQuerySourceInfo(searchQuerySourceInfo);
            }
            return playSessionSource1;
        };
    }

    public void refresh() {
        syncInitiator.syncPlaylist(playlistUrn)
                     .subscribe(new DefaultSubscriber<SyncJobResult>() {
                         @Override
                         public void onStart() {
                             refresh.onNext(true);
                         }

                         @Override
                         public void onCompleted() {
                             refresh.onNext(false);
                         }
                     });
    }

    private AsyncViewModel<PlaylistDetailsViewModel> combine(Boolean isRefreshing,
                                                             Boolean isEditMode,
                                                             PlaylistWithTracks playlistWithTracks,
                                                             LikedStatuses likedStatuses,
                                                             OfflineProperties offlineProperties) {
        return AsyncViewModel.create(of(viewModelCreator.create(playlistWithTracks.playlist(),
                                                             updateTracks(playlistWithTracks, offlineProperties),
                                                             likedStatuses.isLiked(playlistWithTracks.playlist().urn()),
                                                             isEditMode,
                                                             offlineProperties.state(playlistWithTracks.playlist().urn()),
                                                             Optional.absent())), isRefreshing, Optional.absent());
    }

    private List<TrackItem> updateTracks(PlaylistWithTracks playlistWithTracks, OfflineProperties offlineProperties) {
        List<Track> tracks = playlistWithTracks.tracks();
        List<TrackItem> trackItems = new ArrayList<>(tracks.size());
        for (Track track : tracks) {
            // todo, use constructor instead of update
            trackItems.add(TrackItem.from(track).updatedWithOfflineState(offlineProperties.state(track.urn())));
        }
        return trackItems;
    }

    void disconnect() {
        subscription.unsubscribe();
    }

    Observable<Urn> goToCreator() {
        return gotoCreator;
    }

    Observable<PlaybackResult.ErrorReason> onPlaybackError() {
        return playbackError;
    }

    public Observable<AsyncViewModel<PlaylistDetailsViewModel>> viewModel() {
        return viewModelSubject;
    }

    @Override
    public void onCreatorClicked() {
        onCreatorClicked.onNext(null);
    }

    @Override
    public void onEnterEditMode() {
        editMode.onNext(true);
    }

    @Override
    public void onExitEditMode() {
        editMode.onNext(false);
    }

    @Override
    public void onMakeOfflineAvailable() {
        offlineAvailable.onNext(null);
    }

    @Override
    public void onMakeOfflineUnavailable() {
        offlineUnavailable.onNext(null);
    }

    @Override
    public void onHeaderPlayButtonClicked() {
        headerPlayClicked.onNext(null);
    }

    @Override
    public void onPlayNext() {
        playQueueHelper.playNext(playlistUrn);
    }

    @Override
    public void onToggleLike(boolean isLiked) {
        if (isLiked) {
            like.onNext(null);
        } else {
            unlike.onNext(null);
        }
    }

    private EventContextMetadata getEventContext() {
        return EventContextMetadata.builder()
                                   .contextScreen(screen)
                                   .pageName(Screen.PLAYLIST_DETAILS.get())
                                   .invokerScreen(Screen.PLAYLIST_DETAILS.get())
                                   .pageUrn(playlistUrn)
                                   .build();
    }

    @Override
    public void onToggleRepost(boolean isReposted) {

    }

    @Override
    public void onShare() {

    }

    @Override
    public void onUpsell() {

    }

    @Override
    public void onOverflowUpsell() {

    }

    @Override
    public void onOverflowUpsellImpression() {

    }

    @Override
    public void onPlayShuffled() {

    }

    @Override
    public void onDeletePlaylist() {

    }

    private OfflineInteractionEvent getOfflinePlaylistTrackingEvent(Urn urn, boolean isMarkedForOffline, PlaySessionSource playSessionSource) {
        return isMarkedForOffline ?
               OfflineInteractionEvent.fromAddOfflinePlaylist(
                       Screen.PLAYLIST_DETAILS.get(),
                       urn,
                       playSessionSource.getPromotedSourceInfo()) :
               OfflineInteractionEvent.fromRemoveOfflinePlaylist(
                       Screen.PLAYLIST_DETAILS.get(),
                       urn,
                       playSessionSource.getPromotedSourceInfo());
    }

    @VisibleForTesting
    @AutoValue
    static abstract class PlaylistWithTracks {

        static PlaylistWithTracks create(Playlist playlist, List<Track> tracks) {
            return new AutoValue_NewPlaylistDetailsPresenter_PlaylistWithTracks(playlist, tracks);
        }

        abstract Playlist playlist();

        abstract List<Track> tracks();
    }

    @AutoFactory(allowSubclasses = true)
    static class DataSourceProvider {

        private PlaylistRepository playlistRepository;
        private TrackRepository trackRepository;
        private EventBus eventBus;
        private Urn playlistUrn;

        private final BehaviorSubject<PlaylistWithTracks> data = BehaviorSubject.create();

        @Inject
        DataSourceProvider(Urn initialUrn,
                           @Provided PlaylistRepository playlistRepository,
                           @Provided TrackRepository trackRepository,
                           @Provided EventBus eventBus) {
            this.playlistRepository = playlistRepository;
            this.trackRepository = trackRepository;
            this.eventBus = eventBus;
            this.playlistUrn = initialUrn;
            dataSource().subscribe(data);
        }

        public Observable<PlaylistWithTracks> data() {
            return data;
        }

        Observable<PlaylistWithTracks> dataSource() {
            return playlistUpdates().startWith(singleton(null))
                    .switchMap(ignored -> combineLatest(playlist(), tracks(), PlaylistWithTracks::create));
        }

        private Observable<List<Track>> tracks() {
            return trackRepository.forPlaylist(playlistUrn)
                                  .startWith(Collections.<Track>emptyList());
        }

        private Observable<Playlist> playlist() {
            return playlistRepository.withUrn(playlistUrn);
        }

        private Observable<PlaylistChangedEvent> playlistUpdates() {
            return eventBus.queue(EventQueue.PLAYLIST_CHANGED)
                           .filter(event -> event.changeMap().containsKey(playlistUrn))
                           .filter(event -> event.isEntityChangeEvent() || event.isTracklistChangeEvent())
                           .doOnNext(this::storeUpdatedUrn);
        }

        private void storeUpdatedUrn(PlaylistChangedEvent event) {
            if (event.kind() == PlaylistChangedEvent.Kind.PLAYLIST_PUSHED_TO_SERVER) {
                playlistUrn = ((PlaylistEntityChangedEvent) event).changeMap().get(playlistUrn).urn();
            }
        }
    }

    /**
     * TODO :
     * - error states
     * - Upsells
     * - Other playlists by user
     * - Local dataSource pushed to server while viewing it
     * - missing playlists
     * - offline logic
     * - show confirmation dialog when exiting offline mode
     */
}
